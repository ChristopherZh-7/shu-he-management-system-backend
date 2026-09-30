#!/usr/bin/env python3
"""Serve a built frontend and proxy its API on loopback for local acceptance."""
import argparse
import http.client
import shutil
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--dist', type=Path, required=True)
parser.add_argument('--port', type=int, default=5668)
parser.add_argument('--api-port', type=int, default=48086)
args = parser.parse_args()
if not (args.dist / 'index.html').is_file():
    parser.error('Build the frontend before serving it')


class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *values, **kwargs):
        super().__init__(*values, directory=str(args.dist.resolve()), **kwargs)

    def do_GET(self):
        if self.path.startswith('/admin-api/'):
            return self.proxy()
        return super().do_GET()

    def proxy(self):
        if not self.path.startswith('/admin-api/'):
            return self.send_error(404)
        size = int(self.headers.get('Content-Length', '0'))
        if size > 16 * 1024 * 1024:
            return self.send_error(413)
        body = self.rfile.read(size) if size else None
        headers = {key: value for key, value in self.headers.items()
                   if key.lower() not in ('host', 'connection', 'transfer-encoding')}
        connection = http.client.HTTPConnection('127.0.0.1', args.api_port, timeout=60)
        try:
            connection.request(self.command, self.path, body=body, headers=headers)
            response = connection.getresponse()
            self.send_response(response.status)
            for key, value in response.getheaders():
                if key.lower() not in ('connection', 'transfer-encoding'):
                    self.send_header(key, value)
            self.end_headers()
            shutil.copyfileobj(response, self.wfile)
        except (OSError, http.client.HTTPException):
            self.send_error(502)
        finally:
            connection.close()

    do_POST = do_PUT = do_DELETE = do_PATCH = proxy


class LocalServer(ThreadingHTTPServer):
    request_queue_size = 128


LocalServer(('127.0.0.1', args.port), Handler).serve_forever()
