"""Credential-free, bounded retrieval from operator-reviewed source relationships.

No model or client can grant a host authority. Content, links and publication dates
remain untrusted. The network adapter pins a public IP while verifying TLS for the
original hostname; redirects, cookies, proxies and private addresses are refused.
"""
from dataclasses import dataclass, asdict
from enum import Enum
from datetime import datetime, timezone
from html.parser import HTMLParser
from concurrent.futures import ThreadPoolExecutor, TimeoutError as FutureTimeout
import hashlib
import http.client
import ipaddress
import json
import re
import socket
import ssl
import threading
import time
from urllib.parse import urlsplit, urljoin

from backend.gateway import InvalidRequest
from backend.live import safe_text


class SourceType(str, Enum):
    OFFICIAL = "official"
    PRIMARY = "primary"
    SECONDARY = "secondary"
    COMMUNITY = "community"
    UNVERIFIED = "unverified"


# This is a claim policy, not a single cache TTL. Retrieval alone never proves
# that an undated page describes current rules.
TTL = {"outage": 300, "pricing": 3600, "deadline": 3600, "eligibility": 86400,
       "requirements": 86400, "procedure": 86400, "background": 604800}
AUTHORITY = {kind.value: rank for rank, kind in enumerate(SourceType)}


def canonical_url(value):
    if not isinstance(value, str) or not 1 <= len(value) <= 1024 or not value.isascii():
        raise InvalidRequest("Unsafe destination")
    if any(ord(c) <= 32 for c in value) or any(c in value for c in ('\\', '%', '@')):
        raise InvalidRequest("Unsafe destination")
    p = urlsplit(value)
    try: port = p.port
    except ValueError: raise InvalidRequest("Unsafe destination") from None
    if (p.scheme != "https" or not p.hostname or p.username or p.password or port not in (None, 443)
            or p.query or p.fragment or p.hostname.endswith('.') or 'xn--' in p.hostname
            or not re.fullmatch(r"[a-z0-9]+(?:[.-][a-z0-9]+)*\.[a-z]{2,63}", p.hostname)
            or any(part in ('.', '..') for part in p.path.split('/'))):
        raise InvalidRequest("Unsafe destination")
    return "https://" + p.hostname + (p.path or '/')


@dataclass(frozen=True)
class Source:
    url: str
    title: str
    source_type: str
    jurisdiction: str
    verified_by: str  # Operator review reference, never supplied by search/page/model.

    def __post_init__(self):
        if canonical_url(self.url) != self.url or self.source_type not in AUTHORITY:
            raise ValueError("Invalid reviewed source")
        if not all(isinstance(v, str) and 1 <= len(v) <= 160 for v in (self.title, self.jurisdiction, self.verified_by)):
            raise ValueError("Missing source review")


class SourceRegistry:
    def __init__(self, sources=()):
        self.sources = tuple(sources)
        if len(self.sources) > 32 or len({s.url for s in self.sources}) != len(self.sources):
            raise ValueError("Bounded unique source registry required")

    def source_for(self, url):
        url = canonical_url(url)
        # Authority follows only a reviewed origin AND path scope, not a substring
        # (official.example.evil.test) or arbitrary external link from a good page.
        candidates = [s for s in self.sources if url == s.url or (
            s.url.endswith('/') and url.startswith(s.url))]
        return max(candidates, key=lambda s: len(s.url), default=None)

    def destination(self, url, high_risk=True):
        try: source = self.source_for(url)
        except InvalidRequest: return "blocked"
        if source is None: return "unverified"
        if high_risk and source.source_type not in ('official', 'primary'): return "unverified"
        return "reviewed"


class Page(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.parts, self.links, self.skip = [], [], 0
        self.published = None
        self.title_parts = []
        self.in_title = False

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == 'title': self.in_title = True
        if tag in ('script', 'style', 'noscript', 'template'): self.skip += 1
        if self.skip: return
        if tag == 'a' and len(self.links) < 64 and 'href' in attrs: self.links.append(attrs['href'])
        if tag in ('p', 'div', 'li', 'h1', 'h2', 'h3', 'br'): self.parts.append('\n')
        if tag == 'meta' and attrs.get('property') in ('article:modified_time', 'article:published_time'):
            candidate = attrs.get('content', '')
            if re.fullmatch(r'\d{4}-\d{2}-\d{2}(?:T[0-9:+Z.-]+)?', candidate): self.published = candidate

    def handle_endtag(self, tag):
        if tag == 'title': self.in_title = False
        if tag in ('script', 'style', 'noscript', 'template') and self.skip: self.skip -= 1

    def handle_data(self, data):
        if not self.skip:
            if self.in_title: self.title_parts.append(data)
            else: self.parts.append(data)

    @property
    def title(self):
        return re.sub(r'\s+', ' ', ''.join(self.title_parts)).strip()[:160]

    @property
    def text(self):
        return re.sub(r'\s+', ' ', ' '.join(self.parts)).strip()[:12000]


class PinnedHTTPS(http.client.HTTPSConnection):
    def __init__(self, host, address, timeout):
        super().__init__(host, timeout=timeout, context=ssl.create_default_context())
        self.address = address

    def connect(self):
        raw = socket.create_connection((self.address, 443), self.timeout)
        try: self.sock = self._context.wrap_socket(raw, server_hostname=self.host)
        except BaseException:
            raw.close()
            raise


_dns_pool = ThreadPoolExecutor(max_workers=2, thread_name_prefix='saathi-dns')
_dns_slots = threading.BoundedSemaphore(2)


def public_addresses(host, resolver=socket.getaddrinfo, deadline=None):
    deadline = min(deadline or (time.monotonic()+3), time.monotonic()+3)
    if not _dns_slots.acquire(blocking=False): raise InvalidRequest('DNS busy')
    try:
        future = _dns_pool.submit(resolver, host, 443, type=socket.SOCK_STREAM)
    except Exception:
        _dns_slots.release()
        raise
    future.add_done_callback(lambda _: _dns_slots.release())
    try: answers = future.result(timeout=max(0, deadline-time.monotonic()))
    except FutureTimeout: raise TimeoutError('DNS deadline') from None
    addresses = sorted({answer[4][0] for answer in answers})
    parsed = [ipaddress.ip_address(a) for a in addresses]
    if not addresses or any(not a.is_global or a.is_multicast or a.is_reserved or a.is_unspecified for a in parsed):
        raise InvalidRequest("Non-public destination")
    return addresses


def fetch_html(url, deadline):
    p = urlsplit(canonical_url(url))
    # DNS results are checked once, then the connection is pinned (no second lookup).
    addresses = public_addresses(p.hostname, deadline=deadline)
    remaining = deadline - time.monotonic()
    if remaining <= 0: raise TimeoutError()
    connection = PinnedHTTPS(p.hostname, addresses[0], min(remaining, 3))
    try:
        connection.request('GET', p.path or '/', headers={'User-Agent': 'Saathi-Research/1.0',
            'Accept': 'text/html,text/plain', 'Accept-Encoding': 'identity'})
        response = connection.getresponse()
        if response.status != 200 or response.getheader('Content-Encoding', 'identity') != 'identity':
            raise InvalidRequest("Unsupported source response")
        if response.getheader('Content-Type', '').split(';')[0] not in ('text/html', 'text/plain', 'application/json'):
            raise InvalidRequest("Unsupported source document")
        raw = bytearray()
        while True:
            remaining = deadline - time.monotonic()
            if remaining <= 0: raise TimeoutError()
            if connection.sock: connection.sock.settimeout(min(remaining, 3))
            chunk = response.read1(min(4096, 131073 - len(raw)))
            if not chunk: break
            raw.extend(chunk)
            if len(raw) > 131072: raise InvalidRequest("Source too large")
        if time.monotonic() >= deadline: raise TimeoutError()
        return raw.decode('utf-8', errors='strict')
    finally: connection.close()


@dataclass(frozen=True)
class ResearchQuery:
    request_id: str
    goal: str
    locale: str
    jurisdiction: str
    claim_type: str
    consent: bool

    @classmethod
    def parse(cls, data):
        if not isinstance(data, dict) or set(data) != set(cls.__dataclass_fields__):
            raise InvalidRequest("Invalid research request")
        if (not isinstance(data['request_id'], str) or not re.fullmatch(r'[A-Za-z0-9_-]{1,64}', data['request_id'])
                or data['consent'] is not True or data['locale'] not in ('en-IN', 'hi-IN', 'hinglish')
                or not isinstance(data['claim_type'], str) or data['claim_type'] not in TTL or not safe_text(data['goal'], 160)
                or not safe_text(data['jurisdiction'], 80)):
            raise InvalidRequest("Unsafe research request")
        return cls(**data)


@dataclass(frozen=True)
class ResearchEvidence:
    evidence_id: str
    source_url: str
    source_title: str
    source_type: str
    authority_basis: str
    retrieved_at_ms: int
    published_or_updated: str | None
    jurisdiction: str
    snippet: str
    content_hash: str
    claim_type: str
    # No invented numeric confidence. A dated page is still only a publisher claim.
    confidence: str = 'retrieved_not_independently_verified'

    def freshness(self, now_ms):
        age = now_ms - self.retrieved_at_ms
        if not 0 <= age <= TTL[self.claim_type] * 1000: return 'expired'
        if self.published_or_updated is not None:
            try:
                published = datetime.fromisoformat(self.published_or_updated.replace('Z', '+00:00'))
                if published.tzinfo is None: published = published.replace(tzinfo=timezone.utc)
                publication_age = now_ms - int(published.timestamp()*1000)
                if publication_age < -300000: return 'publisher_date_invalid'
                if self.source_type == 'community' and self.claim_type == 'outage' and publication_age > 86400000:
                    return 'historical_report'
            except (ValueError, OverflowError): return 'publisher_date_invalid'
        return 'recent_fetch_date_unknown' if self.published_or_updated is None else 'recent_fetch_publisher_date_unverified'


class RegistryRetriever:
    """Replaceable search/retrieval adapter. Seed discovery is operator-reviewed.

    A bounded same-scope crawl expands seeds, without trusting search ranking or
    transferring authority across hosts. General web-wide search is not implied.
    """
    def __init__(self, registry, fetch=fetch_html, search=None):
        self.registry, self.fetch, self.search = registry, fetch, search

    def retrieve(self, query, deadline, reserve, cancelled=None):
        words = set(re.findall(r'\w+', query.goal.casefold()))
        seeds = sorted(self.registry.sources, key=lambda s: (
            s.jurisdiction not in (query.jurisdiction, 'global'),
            AUTHORITY[s.source_type], -len(words & set(re.findall(r'\w+', s.title.casefold())))))
        discovered, discovery_errors = {}, []
        if self.search is not None and not (cancelled and cancelled.is_set()):
            if reserve():
                try:
                    for hit in self.search.search(query, deadline):
                        # Search titles/snippets/types are never an authority attestation.
                        discovered[hit.url] = self.registry.source_for(hit.url) or Source(hit.url, hit.title,
                            'unverified', 'unknown', 'search_discovery_not_authority')
                except Exception: discovery_errors.append('search_unavailable')
            else: discovery_errors.append('retrieval_budget_exhausted')
        # Rank seeds and discovered hits together. Otherwise a search response full
        # of unverified hits could exhaust the page cap before an official seed.
        queue = sorted(dict.fromkeys([s.url for s in seeds] + list(discovered)),
            key=lambda u: (AUTHORITY[(self.registry.source_for(u) or discovered[u]).source_type],
                           (self.registry.source_for(u) or discovered[u]).jurisdiction not in (query.jurisdiction, 'global')))
        seen, evidence, errors = set(), [], discovery_errors
        while queue and len(seen) < 4 and time.monotonic() < deadline and not (cancelled and cancelled.is_set()):
            url = queue.pop(0)
            if url in seen: continue
            source = self.registry.source_for(url) or discovered.get(url)
            if source is None: continue
            seen.add(url)
            if not reserve():
                errors.append('retrieval_budget_exhausted'); break
            try:
                page = Page(); page.feed(self.fetch(url, deadline))
                if time.monotonic() >= deadline: raise TimeoutError()
                if cancelled and cancelled.is_set(): break
                text = page.text
                # Never treat page instructions as control flow; keep excerpts inert.
                if text:
                    digest = hashlib.sha256(text.encode()).hexdigest()
                    eid = hashlib.sha256((url + digest).encode()).hexdigest()[:24]
                    evidence.append(ResearchEvidence(eid, url, page.title or source.title, source.source_type,
                        source.verified_by, int(time.time()*1000), page.published, source.jurisdiction,
                        text[:2000], digest, query.claim_type))
                links = []
                for link in page.links:
                    try:
                        target = canonical_url(urljoin(url, link))
                        if self.registry.source_for(target) and target not in seen: links.append(target)
                    except (InvalidRequest, ValueError): pass
                queue = links[:4] + queue
            except Exception:
                errors.append('source_unavailable')  # No URL/error body leaks into diagnostics.
        return tuple(evidence), tuple(errors)


class ResearchService:
    def __init__(self, retriever, reserve=None):
        self.retriever = retriever
        self.reserve = reserve
        self.lock = threading.Lock()
        self.control_lock = threading.Lock()
        self.active = {}
        self.cancelled = {}
        self.calls = 0
        self.last_started = float('-inf')
        self.records = {}  # Per-principal, memory-only, at most 8 short-lived bundles.

    def cancel(self, request_id):
        with self.control_lock:
            now = time.monotonic()
            self.cancelled = {k: t for k, t in self.cancelled.items() if t > now}
            if len(self.cancelled) >= 64: del self.cancelled[next(iter(self.cancelled))]
            self.cancelled[request_id] = now + 60
            event = self.active.get(request_id)
            if event: event.set()
            return event is not None

    def _reserve(self):
        if self.calls >= 32: return False
        if self.reserve is not None and not self.reserve(['research']): return False
        self.calls += 1
        return True

    def run(self, data):
        query = ResearchQuery.parse(data)
        if not self.lock.acquire(blocking=False): return {'status': 'rejected', 'reason': 'busy'}
        try:
            now = time.monotonic()
            self.records = {k: v for k, v in self.records.items() if now - v[0] < 300}
            if query.request_id in self.records: return {'status': 'rejected', 'reason': 'duplicate_request'}
            if now - self.last_started < 10: return {'status': 'rejected', 'reason': 'rate_limited'}
            event = threading.Event()
            with self.control_lock:
                if self.cancelled.get(query.request_id, 0) > now:
                    return {'status': 'rejected', 'reason': 'cancelled'}
                self.active[query.request_id] = event
            self.last_started = now
            evidence, errors = self.retriever.retrieve(query, now + 6, self._reserve, event)
            if event.is_set(): return {'status': 'rejected', 'reason': 'cancelled'}
            if len(self.records) >= 8: del self.records[next(iter(self.records))]
            self.records[query.request_id] = (now, query, evidence)
            result = {'status': 'researched', 'request_id': query.request_id,
                    'evidence': [dict(asdict(e), freshness=e.freshness(int(time.time()*1000))) for e in evidence],
                    'limitations': list(errors) + ['bounded_web_discovery' if self.retriever.search else 'bounded_reviewed_sources', 'content_is_untrusted', 'coverage_not_established']}
            if query.claim_type == 'outage':
                from backend.planning import incident_research
                result['incident'] = incident_research(evidence, int(time.time()*1000))
            return result
        finally:
            with self.control_lock: self.active.pop(query.request_id, None)
            self.lock.release()

    def bundle(self, request_id):
        with self.lock:
            record = self.records.get(request_id)
            if record is None or time.monotonic() - record[0] >= 300 or self.cancelled.get(request_id, 0) > time.monotonic(): raise InvalidRequest('Research expired')
            return record[1], record[2]


def configured_research(reserve=None):
    import os
    path = os.environ.get('SAATHI_RESEARCH_SOURCES')
    search_endpoint = os.environ.get('SAATHI_SEARCH_ENDPOINT')
    if not path and not search_endpoint: return None
    search = None
    if search_endpoint:
        from backend.search import SearxSearch
        search = SearxSearch(search_endpoint)
    if not path: return ResearchService(RegistryRetriever(SourceRegistry(), search=search), reserve)
    # A server-owned config file, never a client-supplied URL/path.
    from pathlib import Path
    if Path(path).stat().st_size > 32768: raise ValueError('Source registry too large')
    raw = Path(path).read_bytes()
    if len(raw) > 32768: raise ValueError('Source registry too large')
    from backend.providers import strict_json
    rows = strict_json(raw.decode())
    if not isinstance(rows, list): raise ValueError('Invalid source registry')
    return ResearchService(RegistryRetriever(SourceRegistry(Source(**row) for row in rows), search=search), reserve)
