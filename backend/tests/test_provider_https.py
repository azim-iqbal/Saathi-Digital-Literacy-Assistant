"""Real loopback TLS exercises urllib, without credentials or provider traffic."""
import http.server
import json
import ssl
import subprocess
import tempfile
import threading
import time
import unittest
from pathlib import Path
from unittest.mock import patch
from backend.providers import post_json
from backend.provider_network import NetworkTrace
from backend.gateway import InvalidRequest

class ProviderHttpsTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.folder=tempfile.TemporaryDirectory()
        base=Path(cls.folder.name);cert=base/'cert.pem';key=base/'key.pem'
        subprocess.run(['openssl','req','-x509','-newkey','rsa:2048','-nodes','-days','1',
                        '-subj','/CN=localhost','-addext','subjectAltName=DNS:localhost',
                        '-keyout',str(key),'-out',str(cert)],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
        class Handler(http.server.BaseHTTPRequestHandler):
            def log_message(self,*args):pass
            def do_POST(self):
                self.rfile.read(int(self.headers['Content-Length']))
                if self.path == '/drip-headers':
                    try:
                        self.wfile.write(b'HTTP/1.1 200 OK\r\n')
                        for _ in range(30):
                            self.wfile.write(b'X-Fixture: waiting\r\n')
                            self.wfile.flush()
                            time.sleep(.025)
                        self.wfile.write(b'Content-Length: 2\r\n\r\n{}')
                    except (OSError, ssl.SSLError): pass
                    return
                if self.path=='/headers':time.sleep(.2)
                body=b'{not JSON' if self.path=='/invalid' else json.dumps({'ok':True}).encode()
                try:
                    self.send_response(200);self.send_header('Content-Length',str(len(body)));self.end_headers()
                    if self.path=='/body':time.sleep(.2)
                    self.wfile.write(body)
                except (OSError,ssl.SSLError):pass
        cls.server=http.server.ThreadingHTTPServer(('127.0.0.1',0),Handler)
        server_context=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER);server_context.load_cert_chain(cert,key)
        cls.server.socket=server_context.wrap_socket(cls.server.socket,server_side=True)
        cls.worker=threading.Thread(target=cls.server.serve_forever,daemon=True);cls.worker.start()
        cls.context=ssl.create_default_context(cafile=str(cert))
        cls.url='https://localhost:'+str(cls.server.server_port)
    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown();cls.server.server_close();cls.worker.join();cls.folder.cleanup()
    def request(self,path,trace,timeout=1,context=None,cancelled=None):
        with patch('backend.provider_network.public_addresses',return_value=['127.0.0.1']),patch('ssl._create_default_https_context',return_value=context or self.context):
            return post_json(self.url+path,{},dict(fictional=True),timeout,trace=trace,cancelled=cancelled)
    def test_trickling_headers_cannot_extend_total_deadline(self):
        trace=NetworkTrace();start=time.monotonic()
        with self.assertRaises(TimeoutError): self.request('/drip-headers',trace,timeout=.12)
        self.assertLess(time.monotonic()-start,.3)
        self.assertEqual(trace.phase,'first_byte')
    def test_cancellation_interrupts_inflight_headers_and_body(self):
        for path in ('/headers','/body'):
            cancelled=threading.Event()
            timer=threading.Timer(.05,cancelled.set);timer.start()
            start=time.monotonic()
            try:
                with self.assertRaises(InterruptedError): self.request(path,NetworkTrace(),timeout=2,cancelled=cancelled)
                self.assertLess(time.monotonic()-start,.18)
            finally: timer.cancel();timer.join()
    def test_verified_tls_full_request_and_fixed_phase_diagnostics(self):
        trace=NetworkTrace()
        self.assertEqual(self.request('/',trace),{'ok':True})
        self.assertEqual(set(trace.snapshot()['phase_ms']),{'dns','connect','tls','write','first_byte','body','json','complete'})
        self.assertNotIn('localhost',str(trace.snapshot()))
    def test_watchers_are_released_after_success_timeout_and_cancellation(self):
        for _ in range(8):
            self.request('/',NetworkTrace())
            with self.assertRaises(TimeoutError): self.request('/headers',NetworkTrace(),timeout=.04)
            cancelled=threading.Event();cancelled.set()
            with self.assertRaises(InterruptedError): self.request('/',NetworkTrace(),cancelled=cancelled)
            self.assertFalse(any(t.name=='saathi-provider-deadline' for t in threading.enumerate()))
    def test_header_and_body_delays_use_one_deadline(self):
        for path,phase in [('/headers','first_byte'),('/body','body')]:
            trace=NetworkTrace();start=time.monotonic()
            with self.assertRaises(TimeoutError):self.request(path,trace,timeout=.08)
            self.assertLess(time.monotonic()-start,.18)
            self.assertEqual(trace.phase,phase)
    def test_untrusted_certificate_is_rejected(self):
        trace=NetworkTrace()
        with self.assertRaises(Exception) as error:self.request('/',trace,context=ssl.create_default_context())
        trace.fail(error.exception)
        self.assertEqual(trace.snapshot()['failure_stage'],'tls')
        self.assertEqual(trace.snapshot()['failure_kind'],'tls')
    def test_malformed_json_has_a_distinct_private_safe_failure(self):
        trace=NetworkTrace()
        with self.assertRaises(InvalidRequest) as error:self.request('/invalid',trace)
        trace.fail(error.exception)
        self.assertEqual(trace.snapshot()['failure_kind'],'invalid_json')
