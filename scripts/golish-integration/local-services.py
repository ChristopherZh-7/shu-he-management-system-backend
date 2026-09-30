#!/usr/bin/env python3
"""Manage only this isolated local acceptance stack, using its private configuration."""
import argparse
import hashlib
import os
from pathlib import Path
import shlex
import shutil
import signal
import subprocess
import sys
import time

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('action', choices=['start', 'status', 'stop', 'restart'])
parser.add_argument('component', choices=['shuhe-backend', 'golish-api', 'golish-harness', 'fixture-target', 'shuhe-frontend'])
parser.add_argument('--runtime', type=Path, required=True)
parser.add_argument('--golish-repo', type=Path)
parser.add_argument('--java-home', type=Path, default=Path(os.environ.get('JAVA_HOME', '/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home')))
args = parser.parse_args()
runtime = args.runtime.resolve()
backend = Path(__file__).resolve().parents[2]
frontend = backend.parent / 'frontend'
pidfile = runtime / (args.component + '.pid')
pid = int(pidfile.read_text()) if pidfile.exists() else None
command = subprocess.run(['ps', '-p', str(pid or 0), '-o', 'command='], capture_output=True, text=True).stdout.strip()
owned = command and (str(runtime) in command or str(backend) in command or (args.golish_repo and str(args.golish_repo.resolve()) in command))
if args.action == 'status':
    state = 'running' if owned else ('unrecognized process' if command else 'stopped')
    print(args.component, state, pid if command else '')
    sys.exit(1 if command and not owned else 0)
if command and not owned:
    raise SystemExit('PID belongs to another process; refusing to signal or replace it')
if owned and args.action in ['stop', 'restart']:
    os.kill(pid, signal.SIGTERM)
    for _ in range(100):
        check = subprocess.run(['ps', '-p', str(pid), '-o', 'stat='], capture_output=True, text=True).stdout.strip()
        if not check or check.startswith('Z'):
            break
        time.sleep(0.1)
    else:
        raise SystemExit('Process is still stopping; inspect before retrying')
    pidfile.unlink(missing_ok=True)
elif owned:
    print(args.component, 'already running', pid)
    sys.exit(0)
if args.action == 'stop':
    sys.exit(0)
environment = os.environ.copy()
cwd = backend
if args.component.startswith('golish-'):
    if not args.golish_repo:
        parser.error('--golish-repo is required for Golish services')
    for line in (runtime / 'golish/credentials.env').read_text().splitlines():
        parts = shlex.split(line, comments=True)
        if parts and parts[0] == 'export':
            parts = parts[1:]
        if len(parts) == 1 and '=' in parts[0]:
            key, value = parts[0].split('=', 1)
            environment[key] = value
    golish = args.golish_repo.resolve()
    if args.component == 'golish-api':
        argv = [str(golish / 'build/bin/headless/golish'), 'serve', '--headless', '--skip-auto-setup', '--no-hot-reload', '--base-folder', str(runtime / 'golish')]
    else:
        cwd = golish / 'platform/golish-agent-harness'
        argv = ['node', str(cwd / 'scripts/start.mjs')]
elif args.component == 'shuhe-backend':
    artifact = backend / 'shuhe-server/target/shuhe-server.jar'
    release = runtime / 'releases' / ('shuhe-' + hashlib.sha256(artifact.read_bytes()).hexdigest()[:16] + '.jar')
    release.parent.mkdir(parents=True, exist_ok=True)
    if not release.exists():
        shutil.copy2(artifact, release)
    argv = [str(args.java_home / 'bin/java'), '-Xms256m', '-Xmx1536m', '-jar', str(release), '--spring.profiles.active=golish-local', '--spring.config.additional-location=file:' + str(runtime) + '/']
elif args.component == 'fixture-target':
    argv = [sys.executable, str(Path(__file__).with_name('fixture-target.py')), '--repair-flag', str(runtime / 'fixture-repaired')]
else:
    artifact = frontend / 'apps/web-antd/dist'
    release = runtime / 'releases' / ('frontend-' + hashlib.sha256((artifact / 'index.html').read_bytes()).hexdigest()[:16])
    if not release.exists():
        shutil.copytree(artifact, release)
    argv = [sys.executable, str(Path(__file__).with_name('local-web.py')), '--dist', str(release)]
(runtime / 'logs').mkdir(parents=True, exist_ok=True)
with (runtime / 'logs' / (args.component + '.log')).open('ab') as log:
    process = subprocess.Popen(argv, cwd=cwd, env=environment, stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
pidfile.write_text(str(process.pid))
print(args.component, 'started', process.pid)
