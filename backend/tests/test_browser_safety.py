import time
import unittest
from backend.gateway import InvalidRequest
from backend.live import LiveSnapshot

class BrowserSafetyTests(unittest.TestCase):
    def test_warning_controls_do_not_reach_providers(self):
        for label in ('Your connection is not private', 'NET::ERR_CERT_DATE_INVALID', 'Deceptive site ahead',
                      'आपका कनेक्शन निजी नहीं है', 'Connection surakshit nahin hai'):
            request=dict(request_id='warning',session_id='test',screen_revision=1,observed_at_ms=int(time.time()*1000),
                         package_name='com.android.chrome',window_id=1,locale='en-IN',goal='Continue',
                         controls=[dict(id='n1',label=label)],previous_steps=[])
            with self.subTest(label=label), self.assertRaises(InvalidRequest):
                LiveSnapshot.parse(request)
