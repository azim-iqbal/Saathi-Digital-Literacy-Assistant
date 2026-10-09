"""Fixed-phase HTTPS diagnostics. Never retains hostnames, headers, bodies or exception text."""
import http.client
import socket
import ssl
import time
import urllib.request
from backend.research import public_addresses
from backend.gateway import InvalidRequest

PHASES = frozenset(('dns','connect','tls','write','first_byte','body','json','complete'))

class NetworkTrace:
    def __init__(self):
        self.phase = None
        self.started = time.monotonic()
        self.durations = {}
        self.failure_stage = None
        self.failure_kind = None

    def enter(self, phase):
        if phase not in PHASES: raise ValueError('Unknown transport phase')
        now = time.monotonic()
        if self.phase is not None:
            self.durations[self.phase] = self.durations.get(self.phase,0) + max(0,int((now-self.started)*1000))
        self.phase, self.started = phase, now

    def fail(self, error):
        self.failure_stage = self.phase
        reason = getattr(error,'reason',error)
        self.failure_kind = ('dns' if isinstance(reason,socket.gaierror) else
                             'tls' if isinstance(reason,ssl.SSLError) else
                             'timeout' if isinstance(reason,TimeoutError) else
                             'cancelled' if isinstance(reason,InterruptedError) else
                             'connection' if isinstance(reason,ConnectionError) else
                             'invalid_json' if self.phase == 'json' and isinstance(reason,ValueError) else
                             'invalid_schema' if isinstance(reason,InvalidRequest) else 'transport')

    def snapshot(self):
        durations = dict(self.durations)
        if self.phase is not None:
            durations[self.phase] = durations.get(self.phase,0) + max(0,int((time.monotonic()-self.started)*1000))
        return dict(phase=self.phase, phase_ms=durations, failure_stage=self.failure_stage,
                    failure_kind=self.failure_kind)

class ObservedHTTPSConnection(http.client.HTTPSConnection):
    def __init__(self,*args,deadline,trace,cancelled=None,**kwargs):
        super().__init__(*args,**kwargs)
        self.deadline,self.trace,self.cancelled=deadline,trace,cancelled

    def remaining(self):
        if self.cancelled is not None and self.cancelled.is_set(): raise InterruptedError()
        remaining=self.deadline-time.monotonic()
        if remaining <= 0: raise TimeoutError('Provider deadline')
        return remaining

    def connect(self):
        self.remaining()
        self.trace.enter('dns')
        try: addresses=public_addresses(self.host,deadline=self.deadline)
        except InvalidRequest: raise socket.gaierror('Provider DNS unavailable') from None
        self.trace.enter('connect')
        raw=None
        for address in addresses[:4]:
            try:
                raw=socket.create_connection((address,self.port),self.remaining())
                break
            except OSError:
                if address == addresses[:4][-1]: raise
        try:
            self.trace.enter('tls')
            raw.settimeout(self.remaining())
            self.sock=self._context.wrap_socket(raw,server_hostname=self.host)
        except BaseException:
            if raw is not None: raw.close()
            raise

    def request(self,*args,**kwargs):
        if self.sock is None: self.connect()
        self.trace.enter('write')
        self.sock.settimeout(self.remaining())
        return super().request(*args,**kwargs)

    def getresponse(self):
        self.trace.enter('first_byte')
        self.sock.settimeout(self.remaining())
        response=super().getresponse()
        self.trace.enter('body')
        return response

class ObservedHTTPSHandler(urllib.request.HTTPSHandler):
    def __init__(self,deadline,trace,cancelled=None):
        super().__init__()
        self.deadline,self.trace,self.cancelled=deadline,trace,cancelled

    def https_open(self,request):
        def connection(*args,**kwargs):
            return ObservedHTTPSConnection(*args,deadline=self.deadline,trace=self.trace,cancelled=self.cancelled,**kwargs)
        return self.do_open(connection,request,context=self._context)
