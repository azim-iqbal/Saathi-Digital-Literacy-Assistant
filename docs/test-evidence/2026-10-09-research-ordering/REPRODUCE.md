# Local reproduction

Use the JDK/build/unit commands in `../2026-10-09-private-handoff/REPRODUCE.md`. Run each instrumentation group separately; changing the reverse mapping during a test can invalidate its result. These commands use deterministic fixtures and do not load `backend/.env`.

1. Start `python3 -m backend.tests.device_server` (loopback 8767). It creates the temporary owner-only `/tmp/saathi-gateway-test-token` if absent. Push it to `/data/local/tmp/saathi-test-token` on the emulator.
2. Start `python3 -m http.server 8766 --bind 127.0.0.1 --directory backend/fixtures` and map `adb reverse tcp:8766 tcp:8766` for the Chrome test.
3. Map `adb reverse tcp:8765 tcp:8767`. Run the classes in `core-private-final.txt` with `am instrument -w -e class <comma-separated fully qualified classes> -e additionalTestOutputDir /data/user/0/com.saathi/files/core-private-final com.saathi.test/androidx.test.runner.AndroidJUnitRunner`. All test classes are under `com.saathi.ui`.
4. `PausedRequestUiTest` and `GatewayTransportIntegrationTest` create their own in-emulator loopback servers. The latter's capacity regression pauses a worker immediately before cleanup and asserts that no completion callback can run before capacity is released. No provider connection is made.
5. `MixedLifecycleEnduranceTest` accepts `-e mixed_rounds 8`; it repeats real service revocation/rebind, incomplete-tree rejection/recovery, disconnected-loopback recovery and endpoint replacement in one instrumentation process. It saves each completed round and failures. The recorded eight-round result predates only the final private-practice precedence fix; it must not be represented as hours-long validation.

For the practice HTTP group, start a separate local server and map 8765 to its port (8768 used here):

```python
from pathlib import Path
from backend.gateway import Gateway
from backend.server import make_server
gateway = Gateway(global_limit=200, provider_limit=100)
server = make_server(Path('/tmp/saathi-gateway-test-token').read_text(), gateway, port=8768)
try:
    server.serve_forever()
finally:
    server.server_close()
    gateway.close()
```

Run `GatewayIntegrationTest` only against this mock-mode server. Its test private values are fictional; the assertions require no requests during private entry and completion only after the synthetic receipt.

For the connection-diagnostics group, use `RestProvider` with the injected `backend.tests.test_diagnostics.reply` function, never the default live transport:

```python
from pathlib import Path
from backend.gateway import Gateway
from backend.providers import RestProvider
from backend.server import make_server
from backend.tests.test_diagnostics import reply
providers = [RestProvider(p, 'synthetic-test-secret', 'fixture-model',
             lambda *args, p=p: reply(p)) for p in ('gemini', 'groq')]
gateway = Gateway(providers, mode='dual_ai', global_limit=200, provider_limit=100)
server = make_server(Path('/tmp/saathi-gateway-test-token').read_text(), gateway, port=8769)
try:
    server.serve_forever()
finally:
    server.server_close()
    gateway.close()
```

Map 8765 to 8769 and run the classes in `reporting-private-final.txt`. The connection screen must explicitly identify its successful responses as fixtures, not live API proof. Consent/worksheet/offline tests never submit a real complaint.

Extract files through `adb exec-out run-as com.saathi tar -C files/<output-name> -cf - .`. Stop these fixture servers, remove only their reverse mappings and remove the ephemeral fixture token from host/device afterward. Leave configured provider credentials untouched.

Do not run `ProviderSmokeTest` or a genuine provider probe with this suite. The separate four-call allowance recorded in `live-four-call/authorized-run.started` is exhausted. Passing local fixtures do not authorize another genuine call.
