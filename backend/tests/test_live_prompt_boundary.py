import json
import threading
import unittest
from backend.providers import RestProvider
from backend.live import validate_live
from backend.tests.test_providers import live

class LivePromptBoundaryTests(unittest.TestCase):
    def test_live_prompt_separates_guidance_from_practice_completion_for_both_adapters(self):
        for name in ('gemini','groq'):
            prompts=[]
            def transport(url,headers,payload,timeout):
                prompt=payload['systemInstruction']['parts'][0]['text'] if name=='gemini' else payload['messages'][0]['content']
                prompts.append(prompt)
                value=json.dumps(dict(action='HIGHLIGHT',target_id='n0',explanation='Open Help.',expected_outcome='Help is visible.',uncertainty=[],completion_evidence=['n0']))
                return {'candidates':[{'finishReason':'STOP','content':{'parts':[{'text':value}]}}]} if name=='gemini' else {'choices':[{'finish_reason':'stop','message':{'content':value}}]}
            provider=RestProvider(name,'synthetic-key','fixture',transport)
            response=provider.propose(live(),threading.Event())
            self.assertIn('completion_evidence must always be []',prompts[0])
            self.assertNotIn('success_title',prompts[0])
            self.assertIsNotNone(validate_live(live(),response),'Unsupported evidence must remain rejected')
