"""Optional self-hosted SearXNG discovery adapter; search ranking never grants trust.

No account keys are required by this protocol. An operator must choose and secure
an HTTPS instance with JSON output enabled. Public instances are not silently used.
"""
from dataclasses import dataclass
import time
from urllib.parse import urlencode, urlsplit
from backend.gateway import InvalidRequest
from backend.research import canonical_url, public_addresses, PinnedHTTPS


@dataclass(frozen=True)
class SearchHit:
    url: str
    title: str


def post_search(endpoint, form, deadline):
    p = urlsplit(canonical_url(endpoint))
    addresses = public_addresses(p.hostname, deadline=deadline)
    remaining = deadline - time.monotonic()
    if remaining <= 0: raise TimeoutError()
    connection = PinnedHTTPS(p.hostname, addresses[0], min(remaining, 3))
    try:
        connection.request('POST', p.path, body=urlencode(form).encode(), headers={
            'Content-Type':'application/x-www-form-urlencoded', 'Accept':'application/json',
            'Accept-Encoding':'identity', 'User-Agent':'Saathi-Research/1.0'})
        response = connection.getresponse()
        if (response.status != 200 or response.getheader('Content-Type','').split(';')[0] != 'application/json'
                or response.getheader('Content-Encoding','identity') != 'identity'):
            raise InvalidRequest('Search unavailable')
        raw=bytearray()
        while True:
            remaining=deadline-time.monotonic()
            if remaining <= 0: raise TimeoutError()
            if connection.sock: connection.sock.settimeout(min(remaining,3))
            chunk=response.read1(min(4096,131073-len(raw)))
            if not chunk: break
            raw.extend(chunk)
            if len(raw)>131072: raise InvalidRequest('Search too large')
        if time.monotonic() >= deadline: raise TimeoutError()
        from backend.providers import strict_json
        return strict_json(raw.decode('utf-8'))
    finally: connection.close()


class SearxSearch:
    def __init__(self, endpoint, transport=post_search):
        self.endpoint, self.transport = canonical_url(endpoint), transport

    def search(self, query, deadline):
        form={'q':query.goal+' '+query.jurisdiction, 'format':'json', 'pageno':'1',
              'language':'hi-IN' if query.locale == 'hi-IN' else 'en-IN', 'safesearch':'2'}
        if query.claim_type == 'outage': form['time_range']='day'
        data=self.transport(self.endpoint,form,deadline)
        if not isinstance(data,dict) or not isinstance(data.get('results'),list):
            raise InvalidRequest('Search schema invalid')
        hits,seen=[],set()
        for row in data['results'][:20]:
            try:
                if not isinstance(row,dict): continue
                url=canonical_url(row['url'])
                title=row.get('title','')
                if (not isinstance(title,str) or not title.strip() or len(title)>160
                        or any(ord(c)<32 or 0xD800<=ord(c)<=0xDFFF for c in title)): continue
                if url not in seen: hits.append(SearchHit(url,title)); seen.add(url)
            except (KeyError,InvalidRequest,ValueError): continue
            if len(hits)==8: break
        return tuple(hits)
