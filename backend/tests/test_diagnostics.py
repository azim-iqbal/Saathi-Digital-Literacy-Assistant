import json
import threading
import time
import unittest
from urllib.error import HTTPError
from backend.gateway import Gateway, InvalidRequest
from backend.configuration import UnconfiguredProvider
from backend.diagnostics import check, status
from backend.providers import RestProvider
from backend.tests.test_providers import decision


def reply(provider, total=19):
    text = decision('com.saathi:id/recharge_bills')
    if provider == 'gemini':
        return dict(candidates=[dict(finishReason='STOP', content=dict(parts=[dict(text=text)]))], usageMetadata=dict(promptTokenCount=10, candidatesTokenCount=9, totalTokenCount=total))
    return dict(choices=[dict(finish_reason='stop', message=dict(content=text))], usage=dict(prompt_tokens=10, completion_tokens=9, total_tokens=total))


class DiagnosticsTests(unittest.TestCase):
    def gateway(self, transport):
        gateway = Gateway([RestProvider(p, 'synthetic-test-secret', 'fixture-model', lambda *args, p=p: transport(p)) for p in ('gemini', 'groq')], mode='dual_ai')
        self.addCleanup(gateway.close)
        return gateway

    def test_status_and_mock_check_never_call_models(self):
        gateway = self.gateway(lambda p: self.fail('Status must be offline'))
        result = status(gateway)
        self.assertTrue(all(p['last'] is None and p['attempts'] == 0 for p in result['providers']))
        mock = Gateway(); self.addCleanup(mock.close)
        self.assertEqual(check(mock, dict(request_id='mock', consent=True))['reason'], 'mock_mode')
        self.assertEqual(sum(mock.calls.values()), 0)

    def test_partial_failure_retains_independent_success_and_token_evidence(self):
        def transport(p):
            if p == 'gemini': raise HTTPError('private-url', 401, 'secret-body', {}, None)
            return reply(p)
        gateway = self.gateway(transport)
        result = check(gateway, dict(request_id='probe', consent=True))
        a, b = [p['last'] for p in result['providers']]
        self.assertEqual(result['reason'], 'provider_auth')
        self.assertEqual(a['outcome'], 'provider_auth'); self.assertTrue(a['http_received'])
        self.assertEqual(b['outcome'], 'succeeded'); self.assertEqual(b['total_tokens'], 19)
        self.assertFalse(b['real_api'], 'Injected transport is never evidence of live connectivity')
        for secret in ('synthetic-test-secret', 'secret-body', 'private-url', 'Choose Help'):
            self.assertNotIn(secret, json.dumps(result))

    def test_consent_cancellation_and_call_caps_apply_to_probe(self):
        gateway = self.gateway(reply)
        for data in ({}, dict(request_id='x', consent=False), dict(request_id='x', consent=True, summary='private')):
            with self.assertRaises(InvalidRequest): check(gateway, data)
        self.assertEqual(sum(gateway.calls.values()), 0)
        gateway.cancel('cancel')
        self.assertEqual(check(gateway, dict(request_id='cancel', consent=True))['reason'], 'cancelled')
        gateway.global_limit = 2
        self.assertEqual(check(gateway, dict(request_id='one', consent=True))['reason'], 'accepted')
        blocked = check(gateway, dict(request_id='two', consent=True))
        self.assertEqual(blocked['reason'], 'quota_exhausted')
        self.assertTrue(all(p['last'] is None for p in blocked['providers']))

    def test_missing_key_blocks_both_calls_and_remains_diagnosable(self):
        gateway = Gateway([UnconfiguredProvider('gemini'), UnconfiguredProvider('groq')], mode='dual_ai')
        self.addCleanup(gateway.close)
        result = check(gateway, dict(request_id='missing', consent=True))
        self.assertEqual(result['reason'], 'not_configured')
        self.assertTrue(all(not p['configured'] and p['attempts'] == 0 for p in result['providers']))

    def test_missing_usage_is_null_and_bad_response_still_records_receipt(self):
        def transport(p):
            return dict(choices=[], usage=dict(total_tokens=True)) if p == 'groq' else dict(candidates=[])
        gateway = self.gateway(transport)
        result = check(gateway, dict(request_id='bad', consent=True))
        for provider in result['providers']:
            self.assertEqual(provider['last']['outcome'], 'invalid_response')
            self.assertIsNone(provider['last']['total_tokens'])
            self.assertTrue(provider['last']['http_received'])

    def test_timeout_does_not_hide_other_success_or_start_retry(self):
        release = threading.Event()
        def transport(p):
            if p == 'gemini': release.wait(.2)
            return reply(p)
        gateway = self.gateway(transport); gateway.timeout = .03
        try:
            result = check(gateway, dict(request_id='slow', consent=True))
            self.assertEqual(result['reason'], 'timeout')
            self.assertEqual(result['providers'][1]['last']['outcome'], 'succeeded')
            self.assertEqual(result['providers'][0]['last']['outcome'], 'in_progress')
            self.assertEqual(sum(gateway.calls.values()), 2)
        finally: release.set()

    def test_http_routes_require_auth_and_explicit_consent(self):
        import http.client
        from backend.server import make_server
        gateway = self.gateway(reply)
        secret = "test-token-" + "x"*40
        server = make_server(secret, gateway, port=0)
        thread = threading.Thread(target=server.serve_forever, daemon=True); thread.start()
        def post(path, data, credential=secret):
            connection = http.client.HTTPConnection("127.0.0.1", server.server_port, timeout=2)
            try:
                connection.request("POST", path, json.dumps(data), {"Authorization": "Bearer " + credential, "Content-Type": "application/json"})
                response = connection.getresponse(); return response.status, json.loads(response.read())
            finally: connection.close()
        try:
            self.assertEqual(post("/v1/connection-status", {}, "wrong")[0], 401)
            self.assertEqual(post("/v1/connection-status", {"summary": "never accepted"})[0], 400)
            self.assertEqual(post("/v1/connection-status", {})[1]["status"], "connection")
            self.assertEqual(post("/v1/provider-check", {"request_id": "http-probe", "consent": False})[0], 400)
            self.assertEqual(sum(gateway.calls.values()), 0)
            response = post("/v1/provider-check", {"request_id": "http-probe", "consent": True})
            self.assertEqual(response[1]["reason"], "accepted")
            self.assertEqual(sum(gateway.calls.values()), 2)
        finally: server.shutdown(); server.server_close(); thread.join()

    def test_both_failed_providers_count_toward_circuit_health(self):
        def fail(p): raise HTTPError("private", 404, "private body", {}, None)
        gateway = self.gateway(fail)
        result = check(gateway, dict(request_id="both-fail", consent=True))
        self.assertEqual(result["reason"], "provider_model")
        self.assertEqual(gateway.failures, {"gemini": 1, "groq": 1})
        self.assertTrue(all(p["last"]["outcome"] == "provider_model" for p in result["providers"]))
