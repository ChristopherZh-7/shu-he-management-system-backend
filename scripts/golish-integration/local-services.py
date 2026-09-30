#!/usr/bin/env python3
"""Start or stop the configured local Shuhe + Golish stack with one command."""
import argparse
import fcntl
import hashlib
import json
import os
from pathlib import Path
import shlex
import shutil
import signal
import socket
import subprocess
import sys
import time
import urllib.request

# Dependency order; the synthetic target is opt-in, never part of normal startup.
PORTS = {'mysql': 13316, 'redis': 16389, 'fixture-target': 18446,
         'golish-api': 8116, 'golish-harness': 3086,
         'shuhe-backend': 48086, 'shuhe-frontend': 5668}
BACKEND = Path(__file__).resolve().parents[2]
SCRIPTS = Path(__file__).resolve().parent


class ServiceError(RuntimeError):
    pass


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, response, code, message, headers, new_url):
        return None  # Health checks must never forward a private bearer token.


def service_names(component, with_fixture=False):
    return [component] if component != 'all' else [
        name for name in PORTS if with_fixture or name != 'fixture-target']


def start_services(manager, names):
    # Discover missing builds/configuration before starting any dependency.
    for name in names:
        manager.preflight(name)
    started = []
    try:
        for name in names:
            if manager.start(name):
                started.append(name)
            manager.wait_ready(name)
    except (Exception, KeyboardInterrupt):
        for name in reversed(started):
            try:
                manager.stop(name)
            except Exception as error:
                print(f'Rollback stopped at {name}: {error}; dependencies kept running', file=sys.stderr)
                break
        raise


def stop_services(manager, names):
    for name in reversed(names):
        # Stop at the first failure so its dependencies remain available.
        manager.stop(name)


class LocalServices:
    def __init__(self, args):
        self.args = args
        self.runtime = args.runtime.resolve()
        self.golish = args.golish_repo.resolve() if args.golish_repo else None
        self.children = {}
        self.environment = None
        self.http = urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect())

    def pidfile(self, name):
        return self.runtime / f'{name}.pid'

    def logpath(self, name):
        return self.runtime / 'logs' / f'{name}.log'

    @staticmethod
    def process_command(pid):
        result = subprocess.run(['ps', '-p', str(pid), '-o', 'stat=', '-o', 'command='],
                                capture_output=True, text=True, timeout=5).stdout.strip()
        fields = result.split(None, 1)
        return fields[1] if len(fields) == 2 and not fields[0].startswith('Z') else ''

    def redis(self, *command):
        result = subprocess.run([str(self.args.redis_cli), '-h', '127.0.0.1', '-p',
                                 str(PORTS['redis']), '--raw', *command],
                                capture_output=True, text=True, timeout=5)
        if result.returncode:
            raise ServiceError('Redis command failed; inspect the private Redis log')
        return result.stdout.strip()

    def owns(self, name, pid, command):
        # A child handle remains authoritative while Redis rewrites its process
        # title during startup, before its identity endpoint becomes available.
        child = self.children.get(name)
        if child and child.pid == pid and child.poll() is None:
            return True
        if name == 'redis':
            try:
                info = dict(line.split(':', 1) for line in self.redis('INFO', 'server').splitlines()
                            if ':' in line and not line.startswith('#'))
                directory = self.redis('CONFIG', 'GET', 'dir').splitlines()
                return (info.get('process_id') == str(pid) and len(directory) == 2
                        and Path(directory[1]).resolve() == self.runtime / 'redis')
            except (OSError, ServiceError, subprocess.TimeoutExpired):
                return False
        argv = shlex.split(command)
        if name == 'mysql':
            return (argv and Path(argv[0]).resolve() == self.args.mysql_bin.resolve()
                    and f'--datadir={self.runtime / "mysql"}' in argv
                    and f'--port={PORTS[name]}' in argv)
        if name == 'golish-harness':
            return bool(self.golish and str(self.golish / 'platform/golish-agent-harness/scripts/start.mjs') in argv)
        if name == 'golish-api':
            return ('--headless' in argv and '--base-folder' in argv
                    and argv[argv.index('--base-folder') + 1:][:1] == [str(self.runtime / 'golish')])
        if name == 'shuhe-backend':
            return ('-jar' in argv and '--spring.profiles.active=golish-local' in argv
                    and f'--spring.config.additional-location=file:{self.runtime}/' in argv)
        script = SCRIPTS / ('fixture-target.py' if name == 'fixture-target' else 'local-web.py')
        return (str(script) in argv
                and any(value.startswith(str(self.runtime) + '/') for value in argv))

    def state(self, name):
        path = self.pidfile(name)
        if not path.exists():
            return 'stopped', None
        try:
            pid = int(path.read_text().strip())
            if pid <= 1:
                raise ValueError()
        except ValueError:
            raise ServiceError(f'{name}: invalid PID file {path}') from None
        command = self.process_command(pid)
        if not command:
            return 'stopped', pid
        return ('running' if self.owns(name, pid, command) else 'foreign PID'), pid

    @staticmethod
    def port_available(name):
        with socket.socket() as sock:
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            try:
                sock.bind(('127.0.0.1', PORTS[name]))
                return True
            except OSError:
                return False

    def golish_environment(self):
        if self.environment is not None:
            return self.environment
        environment = os.environ.copy()
        path = self.runtime / 'golish/credentials.env'
        values = {}
        for line in path.read_text().splitlines():
            parts = shlex.split(line, comments=True)
            if parts and parts[0] == 'export':
                parts = parts[1:]
            if len(parts) == 1 and '=' in parts[0]:
                key, value = parts[0].split('=', 1)
                values[key] = value
        expected = {'GOLISH_DSH_PORT': str(PORTS['golish-harness']),
                    'GOLISH_API_URL': f'http://127.0.0.1:{PORTS["golish-api"]}',
                    'DSH_HOME': str(self.runtime / 'golish/dsh-home'),
                    'GOLISH_HEADLESS_BASE': str(self.runtime / 'golish')}
        for key, value in expected.items():
            if values.get(key) != value:
                raise ServiceError(f'{path}: {key} must match this isolated runtime')
        for key, minimum in (('GOLISH_HARNESS_INTERNAL_TOKEN', 24), ('GOLISH_INTEGRATION_API_KEY', 32)):
            if len(values.get(key, '')) < minimum:
                raise ServiceError(f'{path}: configure {key} (at least {minimum} characters)')
        environment.update(values)
        # The native launcher gives these optional variables precedence over
        # DSH_HOME/DSH_PORT; never inherit another desktop runtime's overrides.
        environment['DSH_GOLISH_HOME'] = expected['DSH_HOME']
        environment['GOLISH_RUNTIME_API_PORT'] = expected['GOLISH_DSH_PORT']
        environment['GOLISH_DSH_WORKSPACE'] = str(self.runtime / 'golish/dsh-home/runtime-workspace')
        self.environment = environment
        return environment

    def preflight(self, name):
        state, _ = self.state(name)
        if state == 'foreign PID':
            raise ServiceError(f'{name}: PID belongs to another process; refusing to signal or replace it')
        if name.startswith('golish-'):
            if not self.golish:
                raise ServiceError('--golish-repo is required for Golish services')
            self.golish_environment()
        if state == 'running':
            return
        if not self.port_available(name):
            raise ServiceError(f'{name}: port {PORTS[name]} is occupied without an owned PID')
        files = {
            'mysql': [self.args.mysql_bin, self.runtime / 'mysql/auto.cnf'],
            'redis': [self.args.redis_bin, self.args.redis_cli, self.runtime / 'redis'],
            'shuhe-backend': [self.args.java_home / 'bin/java',
                              BACKEND / 'shuhe-server/target/shuhe-server.jar',
                              self.runtime / 'application-golish-local.yaml'],
            'shuhe-frontend': [BACKEND.parent / 'frontend/apps/web-antd/dist/index.html'],
            'fixture-target': [SCRIPTS / 'fixture-target.py'],
        }
        if self.golish:
            files['golish-api'] = [self.golish / 'build/bin/headless/golish']
            files['golish-harness'] = [self.golish / 'platform/golish-agent-harness' / path for path in (
                'scripts/start.mjs', 'node_modules/@deepseek-ai/dsh/package.json',
                'node_modules/@golish/dsh-golish-platform-plugin/package.json')]
        for path in files[name]:
            if not path.exists():
                raise ServiceError(f'{name}: missing prerequisite {path}; configure/build before starting')
        if name == 'golish-harness' and not shutil.which('node'):
            raise ServiceError('golish-harness: Node.js is missing from PATH')

    def launch_command(self, name):
        cwd, environment = BACKEND, os.environ.copy()
        if name == 'mysql':
            argv = [str(self.args.mysql_bin), '--no-defaults', f'--datadir={self.runtime / "mysql"}',
                    f'--port={PORTS[name]}', '--bind-address=127.0.0.1',
                    f'--socket={self.runtime / "mysql.sock"}', f'--pid-file={self.pidfile(name)}',
                    '--mysqlx=OFF', f'--log-error={self.logpath(name)}']
        elif name == 'redis':
            argv = [str(self.args.redis_bin), '--bind', '127.0.0.1', '--port', str(PORTS[name]),
                    '--protected-mode', 'yes', '--dir', str(self.runtime / 'redis'),
                    '--dbfilename', 'dump.rdb', '--daemonize', 'no', '--pidfile', str(self.pidfile(name))]
        elif name.startswith('golish-'):
            environment = self.golish_environment()
            if name == 'golish-api':
                argv = [str(self.golish / 'build/bin/headless/golish'), 'serve', '--headless',
                        '--skip-auto-setup', '--no-hot-reload', '--base-folder', str(self.runtime / 'golish')]
            else:
                cwd = self.golish / 'platform/golish-agent-harness'
                argv = ['node', str(cwd / 'scripts/start.mjs')]
        elif name == 'shuhe-backend':
            artifact = BACKEND / 'shuhe-server/target/shuhe-server.jar'
            release = self.runtime / 'releases' / ('shuhe-' + hashlib.sha256(artifact.read_bytes()).hexdigest()[:16] + '.jar')
            release.parent.mkdir(parents=True, exist_ok=True)
            if not release.exists():
                shutil.copy2(artifact, release)
            argv = [str(self.args.java_home / 'bin/java'), '-Xms256m', '-Xmx1536m', '-jar', str(release),
                    '--spring.profiles.active=golish-local', f'--spring.config.additional-location=file:{self.runtime}/']
        elif name == 'fixture-target':
            argv = [sys.executable, str(SCRIPTS / 'fixture-target.py'), '--port', str(PORTS[name]),
                    '--repair-flag', str(self.runtime / 'fixture-repaired')]
        else:
            artifact = BACKEND.parent / 'frontend/apps/web-antd/dist'
            release = self.runtime / 'releases' / ('frontend-' + hashlib.sha256((artifact / 'index.html').read_bytes()).hexdigest()[:16])
            if not release.exists():
                shutil.copytree(artifact, release)
            argv = [sys.executable, str(SCRIPTS / 'local-web.py'), '--dist', str(release),
                    '--port', str(PORTS[name]), '--api-port', str(PORTS['shuhe-backend'])]
        return argv, cwd, environment

    def start(self, name):
        self.preflight(name)
        if self.state(name)[0] == 'running':
            print(name, 'already running', flush=True)
            return False
        argv, cwd, environment = self.launch_command(name)
        self.logpath(name).parent.mkdir(parents=True, exist_ok=True)
        with self.logpath(name).open('ab') as log:
            process = subprocess.Popen(argv, cwd=cwd, env=environment, stdout=log,
                                       stderr=subprocess.STDOUT, start_new_session=True)
        self.children[name] = process
        try:
            self.pidfile(name).write_text(str(process.pid))
        except OSError:
            process.terminate()
            process.wait(timeout=30)
            raise
        print(name, 'started', process.pid, flush=True)
        return True

    def ready(self, name):
        if name == 'mysql':
            with socket.create_connection(('127.0.0.1', PORTS[name]), timeout=2):
                return True  # Backend health later verifies its database connection.
        if name == 'redis':
            return self.redis('PING') == 'PONG'
        path, token = '/', None
        if name == 'golish-harness':
            path = '/golish/platform/v1/health'
            token = self.golish_environment()['GOLISH_HARNESS_INTERNAL_TOKEN']
        elif name == 'golish-api':
            path = '/golish/integration/v1/capabilities'
            token = self.golish_environment()['GOLISH_INTEGRATION_API_KEY']
        elif name == 'shuhe-backend':
            path = '/actuator/health'
        request = urllib.request.Request(f'http://127.0.0.1:{PORTS[name]}{path}')
        if token:
            request.add_header('Authorization', 'Bearer ' + token)
        with self.http.open(request, timeout=3) as response:
            if response.status != 200:
                return False
            if name in ('shuhe-frontend', 'fixture-target'):
                return True
            payload = json.loads(response.read(1024 * 1024))
        if not isinstance(payload, dict):
            return False
        if name == 'golish-harness':
            return payload.get('ready') is True
        if name == 'golish-api':
            return payload.get('schema_version') == 1
        return payload.get('status') == 'UP'

    def wait_ready(self, name):
        deadline, next_message = time.monotonic() + self.args.ready_timeout, 0
        while time.monotonic() < deadline:
            if self.state(name)[0] != 'running':
                raise ServiceError(f'{name}: process exited or identity changed; see {self.logpath(name)}')
            try:
                if self.ready(name):
                    print(name, 'ready', flush=True)
                    return
            except (OSError, ValueError, ServiceError, subprocess.TimeoutExpired):
                pass
            if time.monotonic() >= next_message:
                print(f'{name}: waiting for readiness; log: {self.logpath(name)}', flush=True)
                next_message = time.monotonic() + 15
            time.sleep(0.5)
        raise ServiceError(f'{name}: readiness timed out; see {self.logpath(name)}')

    def stop(self, name):
        state, pid = self.state(name)
        if state == 'foreign PID':
            raise ServiceError(f'{name}: PID belongs to another process; refusing to signal it')
        if state == 'stopped' and not self.port_available(name):
            raise ServiceError(f'{name}: port {PORTS[name]} is occupied without an owned PID; dependencies kept running')
        if state == 'running':
            if name == 'redis':
                # A newly started Redis may not be listening yet. SIGTERM also
                # performs graceful shutdown; a ready server explicitly saves.
                try:
                    ready = self.ready(name)
                except (OSError, ServiceError, subprocess.TimeoutExpired):
                    ready = False
                if ready:
                    self.redis('SHUTDOWN', 'SAVE')
                else:
                    os.kill(pid, signal.SIGTERM)
            else:
                os.kill(pid, signal.SIGTERM)
            deadline = time.monotonic() + 30
            while self.process_command(pid):
                if time.monotonic() >= deadline:
                    raise ServiceError(f'{name}: still stopping; dependencies kept running; inspect {self.logpath(name)}')
                time.sleep(0.1)
        child = self.children.pop(name, None)
        if child:
            child.wait(timeout=1)
        self.pidfile(name).unlink(missing_ok=True)
        print(name, 'stopped', flush=True)


def parser():
    result = argparse.ArgumentParser(description=__doc__)
    result.add_argument('action', choices=['start', 'check', 'status', 'stop', 'restart'])
    result.add_argument('component', choices=['all', *PORTS])
    result.add_argument('--runtime', type=Path, required=True)
    result.add_argument('--golish-repo', type=Path)
    result.add_argument('--with-fixture', action='store_true', help='Include the synthetic target in start/restart all')
    result.add_argument('--ready-timeout', type=float, default=240, help='Readiness timeout per service, in seconds')
    result.add_argument('--java-home', type=Path, default=Path(os.environ.get('JAVA_HOME', '/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home')))
    result.add_argument('--mysql-bin', type=Path, default=Path('/opt/homebrew/opt/mysql@8.4/bin/mysqld'))
    result.add_argument('--redis-bin', type=Path, default=Path('/opt/homebrew/opt/redis/bin/redis-server'))
    result.add_argument('--redis-cli', type=Path, default=Path('/opt/homebrew/opt/redis/bin/redis-cli'))
    return result


def main(argv=None):
    args = parser().parse_args(argv)
    if args.ready_timeout <= 0:
        raise ServiceError('--ready-timeout must be positive')
    if not args.runtime.is_dir():
        raise ServiceError('--runtime must be an existing configured directory')
    manager = LocalServices(args)
    names = service_names(args.component, args.with_fixture)
    all_names = service_names(args.component, True)
    if args.action == 'status':
        failed = False
        for name in all_names:
            state, pid = manager.state(name)
            if state == 'stopped' and not manager.port_available(name):
                state = 'unmanaged listener'
            print(name, state, pid if state == 'running' else '')
            failed |= state not in ('running', 'stopped')
        return int(failed)
    # Concurrent lifecycle commands must not race over shared PID files.
    with (manager.runtime / '.services.lock').open('a') as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            raise ServiceError('Another lifecycle command is running for this runtime') from None
        if args.action == 'check':
            for name in names:
                manager.preflight(name)
                print(name, 'prerequisites OK')
            return 0
        if args.action in ('stop', 'restart'):
            stop_services(manager, all_names)
        if args.action in ('start', 'restart'):
            start_services(manager, names)
            if args.component == 'all':
                print(f'Management platform ready: http://127.0.0.1:{PORTS["shuhe-frontend"]}/')
    return 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (ServiceError, OSError, subprocess.TimeoutExpired) as error:
        print(f'Error: {error}', file=sys.stderr)
        sys.exit(1)
    except KeyboardInterrupt:
        print('Interrupted', file=sys.stderr)
        sys.exit(130)
