"""Lifecycle regressions; real processes use temporary data and ephemeral ports."""
from contextlib import redirect_stdout
import importlib.util
import io
import json
import os
from pathlib import Path
import socket
import tempfile
import threading
import unittest
import urllib.error
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from unittest.mock import Mock, patch

spec = importlib.util.spec_from_file_location('local_services', Path(__file__).with_name('local-services.py'))
services = importlib.util.module_from_spec(spec)
spec.loader.exec_module(services)


class OrchestrationTests(unittest.TestCase):
    def test_api_is_ready_before_harness_and_platform_and_fixture_is_opt_in(self):
        names = services.service_names('all')
        self.assertEqual(names, ['mysql', 'redis', 'golish-api', 'golish-harness',
                                 'shuhe-backend', 'shuhe-frontend'])
        manager = Mock()
        manager.start.return_value = True
        services.start_services(manager, names)
        calls = [(call[0], call[1][0]) for call in manager.mock_calls]
        self.assertEqual(calls, [('preflight', name) for name in names] + [
            (action, name) for name in names for action in ('start', 'wait_ready')])
        self.assertIn('fixture-target', services.service_names('all', True))

    def test_preflight_failure_never_starts_a_partial_stack(self):
        manager = Mock()
        manager.preflight.side_effect = [None, services.ServiceError('missing build')]
        with self.assertRaisesRegex(services.ServiceError, 'missing build'):
            services.start_services(manager, ['mysql', 'shuhe-frontend'])
        manager.start.assert_not_called()

    def test_failed_readiness_rolls_back_only_new_processes_in_reverse_order(self):
        names = services.service_names('all')
        manager = Mock()
        manager.start.side_effect = [False, False, True, True]
        manager.wait_ready.side_effect = [None, None, None, services.ServiceError('not ready')]
        with self.assertRaisesRegex(services.ServiceError, 'not ready'):
            services.start_services(manager, names)
        self.assertEqual([call.args[0] for call in manager.stop.call_args_list],
                         ['golish-harness', 'golish-api'])
        self.assertEqual(manager.start.call_count, 4)

    def test_interruption_also_reclaims_new_services(self):
        manager = Mock()
        manager.start.return_value = True
        manager.wait_ready.side_effect = KeyboardInterrupt()
        with self.assertRaises(KeyboardInterrupt):
            services.start_services(manager, ['mysql'])
        manager.stop.assert_called_once_with('mysql')

    def test_failed_stop_keeps_dependencies_running(self):
        manager = Mock()
        manager.stop.side_effect = [None, services.ServiceError('still running')]
        with self.assertRaisesRegex(services.ServiceError, 'still running'):
            services.stop_services(manager, services.service_names('all', True))
        self.assertEqual([call.args[0] for call in manager.stop.call_args_list],
                         ['shuhe-frontend', 'shuhe-backend'])


class RuntimeTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix='golish-services-')
        self.addCleanup(self.temporary.cleanup)
        self.runtime = Path(self.temporary.name).resolve()
        self.args = services.parser().parse_args([
            'status', 'all', '--runtime', str(self.runtime), '--ready-timeout', '5',
            '--golish-repo', str(self.runtime / 'repo')])
        self.manager = services.LocalServices(self.args)
        self.output = io.StringIO()
        self.capture = redirect_stdout(self.output)
        self.capture.__enter__()
        self.addCleanup(self.capture.__exit__, None, None, None)

    def write_credentials(self, **overrides):
        values = {
            'GOLISH_DSH_PORT': str(services.PORTS['golish-harness']),
            'GOLISH_API_URL': f'http://127.0.0.1:{services.PORTS["golish-api"]}',
            'GOLISH_HEADLESS_BASE': str(self.runtime / 'golish'),
            'DSH_HOME': str(self.runtime / 'golish/dsh-home'),
            'GOLISH_HARNESS_INTERNAL_TOKEN': 'h' * 32,
            'GOLISH_INTEGRATION_API_KEY': 'i' * 32,
        }
        values.update(overrides)
        path = self.runtime / 'golish/credentials.env'
        path.parent.mkdir(exist_ok=True)
        path.write_text('\n'.join(f'export {key}={value}' for key, value in values.items()))

    def test_cross_runtime_config_is_rejected_without_echoing_credentials(self):
        self.write_credentials(DSH_HOME='/another/runtime')
        with self.assertRaisesRegex(services.ServiceError, 'DSH_HOME') as error:
            self.manager.golish_environment()
        self.assertNotIn('h' * 32, str(error.exception))

    def test_parent_desktop_overrides_cannot_redirect_the_harness_runtime(self):
        self.write_credentials()
        with patch.dict(os.environ, {'DSH_GOLISH_HOME': '/desktop', 'GOLISH_RUNTIME_API_PORT': '3080'}):
            environment = self.manager.golish_environment()
        self.assertEqual(environment['DSH_GOLISH_HOME'], str(self.runtime / 'golish/dsh-home'))
        self.assertEqual(environment['GOLISH_RUNTIME_API_PORT'], '3086')

    def test_foreign_pid_cannot_be_signalled_or_replaced(self):
        self.manager.pidfile('shuhe-backend').write_text(str(os.getpid()))
        with patch.object(services.os, 'kill') as kill:
            with self.assertRaisesRegex(services.ServiceError, 'another process'):
                self.manager.stop('shuhe-backend')
            with self.assertRaisesRegex(services.ServiceError, 'another process'):
                self.manager.start('shuhe-backend')
            kill.assert_not_called()

    def test_unmanaged_listener_is_preserved(self):
        with socket.socket() as listener:
            listener.bind(('127.0.0.1', 0))
            listener.listen()
            with patch.dict(services.PORTS, {'fixture-target': listener.getsockname()[1]}):
                for operation in (self.manager.start, self.manager.stop):
                    with self.assertRaisesRegex(services.ServiceError, 'without an owned PID'):
                        operation('fixture-target')

    def test_redis_requires_matching_pid_and_data_directory(self):
        with patch.object(self.manager, 'redis', side_effect=[
                'process_id:123', f'dir\n{self.runtime / "redis"}']):
            self.assertTrue(self.manager.owns('redis', 123, 'redis-server'))
        with patch.object(self.manager, 'redis', side_effect=['process_id:123', 'dir\n/another/runtime']):
            self.assertFalse(self.manager.owns('redis', 123, 'redis-server'))

    def test_new_redis_identity_does_not_depend_on_its_starting_endpoint(self):
        child = Mock(pid=123)
        child.poll.return_value = None
        self.manager.children['redis'] = child
        with patch.object(self.manager, 'redis') as redis:
            self.assertTrue(self.manager.owns('redis', 123, 'redis-server'))
            redis.assert_not_called()

    def test_golish_probes_use_authenticated_api_and_harness_readiness(self):
        self.write_credentials()
        response = Mock(status=200)
        context = Mock()
        context.__enter__ = Mock(return_value=response)
        context.__exit__ = Mock(return_value=False)
        with patch.object(self.manager.http, 'open', return_value=context) as request:
            response.read.return_value = json.dumps({'ready': False}).encode()
            self.assertFalse(self.manager.ready('golish-harness'))
            self.assertTrue(request.call_args.args[0].full_url.endswith('/golish/platform/v1/health'))
            self.assertEqual(request.call_args.args[0].get_header('Authorization'), 'Bearer ' + 'h' * 32)
            response.read.return_value = json.dumps({'schema_version': 1}).encode()
            self.assertTrue(self.manager.ready('golish-api'))
            self.assertTrue(request.call_args.args[0].full_url.endswith('/golish/integration/v1/capabilities'))
            self.assertEqual(request.call_args.args[0].get_header('Authorization'), 'Bearer ' + 'i' * 32)

    def test_concurrent_lifecycle_command_is_rejected(self):
        with (self.runtime / '.services.lock').open('a') as lock:
            services.fcntl.flock(lock, services.fcntl.LOCK_EX | services.fcntl.LOCK_NB)
            with self.assertRaisesRegex(services.ServiceError, 'Another lifecycle command'):
                services.main(['check', 'fixture-target', '--runtime', str(self.runtime)])

    def test_authenticated_health_redirect_is_rejected(self):
        requests = []

        class Redirect(BaseHTTPRequestHandler):
            def do_GET(self):
                requests.append(self.path)
                self.send_response(302)
                self.send_header('Location', '/unexpected-destination')
                self.end_headers()

            def log_message(self, *_):
                pass

        server = ThreadingHTTPServer(('127.0.0.1', 0), Redirect)
        thread = threading.Thread(target=server.serve_forever)
        thread.start()
        try:
            with patch.dict(services.PORTS, {'golish-harness': server.server_port}):
                self.write_credentials()
                with self.assertRaises(urllib.error.HTTPError):
                    self.manager.ready('golish-harness')
                self.assertEqual(requests, ['/golish/platform/v1/health'])
        finally:
            server.shutdown()
            server.server_close()
            thread.join()

    def test_real_ephemeral_process_start_reuse_stop_and_data_preservation(self):
        with socket.socket() as reservation:
            reservation.bind(('127.0.0.1', 0))
            port = reservation.getsockname()[1]
        marker = self.runtime / 'fixture-repaired'
        marker.write_text('preserve this state')
        with patch.dict(services.PORTS, {'fixture-target': port}):
            try:
                self.assertTrue(self.manager.start('fixture-target'))
                self.manager.wait_ready('fixture-target')
                pid = self.manager.state('fixture-target')[1]
                # A new invocation must recognize the process without Popen handles.
                second = services.LocalServices(self.args)
                self.assertFalse(second.start('fixture-target'))
                self.assertEqual(second.state('fixture-target'), ('running', pid))
                second.stop('fixture-target')
                self.assertEqual(self.manager.children['fixture-target'].wait(timeout=5), -15)
                self.assertFalse(self.manager.pidfile('fixture-target').exists())
                self.assertEqual(marker.read_text(), 'preserve this state')
            finally:
                child = self.manager.children.get('fixture-target')
                if child and child.poll() is None:
                    child.terminate()
                    child.wait(timeout=5)

    def test_real_process_is_reclaimed_after_readiness_timeout(self):
        with socket.socket() as reservation:
            reservation.bind(('127.0.0.1', 0))
            port = reservation.getsockname()[1]
        self.args.ready_timeout = 0.1
        with patch.dict(services.PORTS, {'fixture-target': port}), patch.object(self.manager, 'ready', return_value=False):
            with self.assertRaisesRegex(services.ServiceError, 'readiness timed out'):
                services.start_services(self.manager, ['fixture-target'])
        self.assertFalse(self.manager.pidfile('fixture-target').exists())
        self.assertFalse(self.manager.children)


if __name__ == '__main__':
    unittest.main()
