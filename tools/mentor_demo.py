"""Open an explicitly synthetic mentor demo on an already running emulator.

No backend, tokens, model calls, input automation or permission changes.
The presenter starts/stops guidance through Saathi's existing UI.
"""
import argparse
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', default=shutil.which('adb') or str(Path.home() / 'Library/Android/sdk/platform-tools/adb'))
    parser.add_argument('--serial', help='An emulator serial, e.g. emulator-5554')
    parser.add_argument('--install', action='store_true', help='Install existing debug app and separate synthetic fixture APK (preserves app data)')
    parser.add_argument('--screen', choices=['home', 'native-form', 'web-form', 'navigation', 'portal', 'commerce'], default='home')
    args = parser.parse_args()
    if not Path(args.adb).is_file():
        parser.error('Android platform tools not found. Supply --adb /path/to/adb.')
    rows = subprocess.check_output([args.adb, 'devices'], text=True).splitlines()[1:]
    ready = [row.split()[0] for row in rows if len(row.split()) >= 2 and row.split()[1] == 'device' and row.startswith('emulator-')]
    serial = args.serial or (ready[0] if len(ready) == 1 else None)
    if serial not in ready:
        parser.error('Start a dedicated Android emulator; use --serial if several are running. Physical devices are intentionally excluded.')
    adb = [args.adb, '-s', serial]

    def run(*command):
        return subprocess.run([*adb, *command], check=True, text=True, capture_output=True).stdout

    if args.install:
        apks = ['app/build/outputs/apk/debug/app-debug.apk', 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk']
        if not all((ROOT / apk).is_file() for apk in apks):
            parser.error('Build assembleDebug and assembleDebugAndroidTest first.')
        for apk in apks:
            run('install', '-r', str(ROOT / apk))
        print('Debug app and synthetic fixture installed; existing app data retained.')
    package = 'com.saathi' if args.screen == 'home' else 'com.saathi.test'
    if 'package:' not in run('shell', 'pm', 'path', package):
        parser.error('Required APK missing. Build first, then use --install.')
    if args.screen == 'home':
        run('shell', 'am', 'start', '-n', 'com.saathi/.LaunchActivity')
    else:
        extras = {
            'native-form': ['--ez', 'reactive_form', 'true'],
            'web-form': ['--ez', 'reactive_form', 'true', '--ez', 'web_form', 'true'],
            'navigation': [],
            'portal': ['--ez', 'portal_fixture', 'true'],
            'commerce': ['--ez', 'commerce_fixture', 'true'],
        }[args.screen]
        run('shell', 'am', 'start', '-n', 'com.saathi.test/com.saathi.ui.ExternalSurfaceActivity', '-f', '0x10008000', *extras)
    print('Opened ' + args.screen + '. Follow docs/DEMO_SCRIPT.md. No AI request or permission change was made by this launcher.')


if __name__ == '__main__':
    try:
        main()
    except subprocess.CalledProcessError:
        raise SystemExit('Android command failed. Check emulator readiness and installed APKs; no credentials are needed.') from None
