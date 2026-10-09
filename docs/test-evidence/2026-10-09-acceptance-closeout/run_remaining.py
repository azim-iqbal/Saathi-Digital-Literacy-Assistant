import subprocess,sys,time,os
from pathlib import Path
import argparse
parser=argparse.ArgumentParser()
parser.add_argument('--adb',required=True);parser.add_argument('--serial',required=True);parser.add_argument('--output',required=True)
args=parser.parse_args()
assert args.serial.startswith('emulator-'),'Synthetic emulator only; no physical device side effects'
root=Path(__file__).resolve().parents[3];os.chdir(root)
out=Path(args.output).resolve();out.mkdir(parents=True,exist_ok=True)
with (out/'emulator-run.started').open('x') as marker:marker.write('Offline fixtures only; ProviderSmokeTest excluded.\n')
adb=[args.adb,'-s',args.serial]
def command(args,**kw):return subprocess.run(args,check=True,**kw)
def group(name,classes,module=None,port=None,token=None):
    name += "-final"
    device_port = "8766" if name.startswith("browser") else "8765"
    server=None;log=None
    try:
        if module:
            log=(out/(name+'-server.txt')).open('w')
            server=subprocess.Popen([sys.executable,'-m',module],stdout=log,stderr=log)
            time.sleep(1)
            assert server.poll() is None,'Fixture failed to start'
        if port:command(adb+['reverse','tcp:'+device_port,'tcp:'+str(port)],stdout=subprocess.DEVNULL)
        if token:command(adb+['push',token,'/data/local/tmp/saathi-test-token'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
        with (out/(name+'.txt')).open('w') as logtest:
            command(adb+['shell','am','instrument','-w','-e','class',','.join('com.saathi.ui.'+c for c in classes),'-e','additionalTestOutputDir','/data/user/0/com.saathi/files/mandate-'+name,'com.saathi.test/androidx.test.runner.AndroidJUnitRunner'],stdout=logtest,stderr=subprocess.STDOUT,timeout=900)
        result=(out/(name+'.txt')).read_text()
        assert 'OK (' in result and 'FAILURES!!!' not in result,name+' failed: see saved evidence'
        print(name+' PASS',flush=True)
    finally:
        if server:
            server.terminate()
            try:server.wait(timeout=10)
            except subprocess.TimeoutExpired:server.kill();server.wait()
        if log:log.close()
        if port:subprocess.run(adb+['reverse','--remove','tcp:'+device_port],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
        if token:subprocess.run(adb+['shell','rm','-f','/data/local/tmp/saathi-test-token'],stdout=subprocess.DEVNULL)
group('practice',['GatewayIntegrationTest','PracticeRecoveryTest'],'backend.tests.mock_device_server',8766,'/tmp/saathi-release-test-token')
group('research',['LiveAiIntegrationTest','ResearchUiTest','IncidentAssessmentUiTest','CyberReportUiTest','CyberLinkOverlayTest','AssistantUiTest','VoiceSetupUiTest','GatewaySetupUiTest'],'backend.tests.device_server',8767,'/tmp/saathi-gateway-test-token')
group('connection',['ConnectionCheckTest','ConnectionScreenUiTest'],'backend.tests.connection_device_server',8767,'/tmp/saathi-gateway-test-token')
group('visual',['SaathiUiTest','LaunchUiTest'])
with (out/'browser-static-server.txt').open('w') as log:
    static=subprocess.Popen([sys.executable,'-m','http.server','8766','--bind','127.0.0.1','--directory','backend/fixtures'],stdout=log,stderr=log)
    try:
        time.sleep(.5)
        group('browser',['ChromeGuidanceIntegrationTest','PublicHttpsReadinessTest'],port=8766)
    finally:static.terminate();static.wait(timeout=10)
