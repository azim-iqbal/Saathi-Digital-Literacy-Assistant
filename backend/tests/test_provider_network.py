"""No external traffic: phase diagnostics and original-deadline socket behavior."""
import socket
import ssl
import threading
import unittest
from unittest.mock import Mock, patch
from backend.provider_network import NetworkTrace, ObservedHTTPSConnection

class ProviderNetworkTests(unittest.TestCase):
    def test_trace_stores_only_fixed_phase_durations(self):
        now=[10.0]
        with patch('time.monotonic',side_effect=lambda:now[0]):
            trace=NetworkTrace()
            trace.enter('dns');now[0]+=.1
            trace.enter('connect');now[0]+=.2
            trace.fail(TimeoutError('secret prompt and private hostname'))
        result=trace.snapshot()
        self.assertEqual(result['failure_stage'],'connect')
        self.assertEqual(result['failure_kind'],'timeout')
        self.assertGreaterEqual(result['phase_ms']['dns'],99)
        self.assertNotIn('secret',str(result))
        with self.assertRaises(ValueError):trace.enter('private arbitrary text')

    def test_dns_tls_write_and_header_failures_are_distinguished_without_messages(self):
        for phase,error,kind in [('dns',socket.gaierror('private'),'dns'),('tls',ssl.SSLError('private'),'tls'),
                                 ('write',BrokenPipeError('private'),'connection'),('first_byte',TimeoutError('private'),'timeout')]:
            trace=NetworkTrace();trace.enter(phase);trace.fail(error)
            self.assertEqual(trace.snapshot()['failure_stage'],phase)
            self.assertEqual(trace.snapshot()['failure_kind'],kind)
            self.assertNotIn('private',str(trace.snapshot()))

    def test_connection_pins_public_dns_and_resets_budget_before_tls(self):
        now=[100.0];raw=Mock();secure=Mock();context=Mock()
        def connect(*args,**kwargs):now[0]+=2;return raw
        context.wrap_socket.return_value=secure
        trace=NetworkTrace()
        with patch('time.monotonic',side_effect=lambda:now[0]),patch('backend.provider_network.public_addresses',return_value=['8.8.8.8']),patch('socket.create_connection',side_effect=connect):
            connection=ObservedHTTPSConnection('example.test',timeout=4,context=context,deadline=104,trace=trace)
            connection.connect()
        raw.settimeout.assert_called_with(2)
        context.wrap_socket.assert_called_once_with(raw,server_hostname='example.test')
        self.assertIs(connection.sock,secure)
        self.assertIn('dns',trace.snapshot()['phase_ms'])
        connection.close()

    def test_expired_or_cancelled_transport_cannot_connect(self):
        for cancelled in (False,True):
            stop=threading.Event()
            if cancelled:stop.set()
            connection=ObservedHTTPSConnection('example.test',deadline=0 if not cancelled else 1e20,trace=NetworkTrace(),cancelled=stop)
            with patch('backend.provider_network.public_addresses') as resolve:
                with self.assertRaises(InterruptedError if cancelled else TimeoutError):connection.connect()
                resolve.assert_not_called()
