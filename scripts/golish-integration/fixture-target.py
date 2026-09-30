#!/usr/bin/env python3
"""Loopback-only synthetic target for initial scan and repair/retest acceptance.

No production data, real credentials, database or outbound requests. Touch the
--repair-flag file to require authorization on the intentionally exposed page.
"""
import argparse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--port', type=int, default=18446)
parser.add_argument('--repair-flag', type=Path, required=True)
args = parser.parse_args()


class Handler(BaseHTTPRequestHandler):
    def version_string(self):
        return 'LocalFixture' if args.repair_flag.exists() else super().version_string()

    def send_error(self, code, message=None, explain=None):
        if not args.repair_flag.exists():
            return super().send_error(code, message, explain)
        body = b'{"error":"Request could not be processed"}\n'
        self.send_response(code)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        if self.command != 'HEAD':
            self.wfile.write(body)
        self.close_connection = True

    def end_headers(self):
        self.send_header('X-Content-Type-Options', 'nosniff')
        if args.repair_flag.exists():
            self.send_header('Content-Security-Policy', "default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'none'")
            self.send_header('X-Frame-Options', 'DENY')
            self.send_header('Referrer-Policy', 'no-referrer')
            self.send_header('Permissions-Policy', 'camera=(), microphone=(), geolocation=()')
            self.send_header('X-Permitted-Cross-Domain-Policies', 'none')
            self.send_header('X-Download-Options', 'noopen')
        super().end_headers()

    def do_HEAD(self):
        self.do_GET()

    def do_GET(self):
        path = urlsplit(self.path).path
        status, content_type = 200, 'text/html; charset=utf-8'
        if path == '/':
            body = '<!doctype html><html><title>本机安全测试联调站点</title><body><h1>本机联调站点</h1><p>仅用于授权联调，全部内容为合成测试数据。</p><a href="/debug/config">诊断配置</a><a href="/health">健康状态</a></body></html>'
        elif path == '/health':
            content_type, body = 'application/json', '{"status":"ok","fixture":true}'
        elif path.rstrip('/') == '/debug/config':
            content_type = 'text/plain; charset=utf-8'
            if args.repair_flag.exists():
                status, body = 403, 'Forbidden: administrator authorization required\n'
            else:
                body = 'INTERNAL_CONFIGURATION=true\nFIXTURE_ONLY=true\nDATABASE_USER=fixture_admin\nDATABASE_PASSWORD=synthetic-demo-value\nADMIN_SIGNING_SECRET=synthetic-demo-value\n'
        elif path == '/robots.txt':
            content_type, body = 'text/plain', 'User-agent: *\nDisallow: /debug/\n'
        else:
            status, body = 404, 'Not found'
        encoded = body.encode()
        self.send_response(status)
        self.send_header('Content-Type', content_type)
        self.send_header('Content-Length', str(len(encoded)))
        self.end_headers()
        if self.command != 'HEAD':
            self.wfile.write(encoded)


ThreadingHTTPServer(('127.0.0.1', args.port), Handler).serve_forever()
