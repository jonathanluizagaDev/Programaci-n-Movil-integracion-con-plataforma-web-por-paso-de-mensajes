import json
import ssl
import threading
import unittest
import urllib.error
import urllib.request
from http.server import ThreadingHTTPServer
from pathlib import Path

import server

ROOT = Path(__file__).resolve().parent

class ApiTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.httpd = ThreadingHTTPServer(('127.0.0.1', 0), server.Handler)
        tls = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        tls.load_cert_chain(ROOT / 'certs/server.pem', ROOT / 'certs/server.key')
        cls.httpd.socket = tls.wrap_socket(cls.httpd.socket, server_side=True)
        cls.thread = threading.Thread(target=cls.httpd.serve_forever, daemon=True)
        cls.thread.start()
        cls.base = f'https://localhost:{cls.httpd.server_port}'
        cls.context = ssl.create_default_context(cafile=str(ROOT / 'certs/server.pem'))

    @classmethod
    def tearDownClass(cls):
        cls.httpd.shutdown()
        cls.httpd.server_close()
        cls.thread.join()

    def setUp(self):
        with server.lock:
            server.state.update(theme='light', events=[])

    def call(self, path, body=None):
        request = urllib.request.Request(self.base + path, data=None if body is None else json.dumps(body).encode(), headers={'Content-Type': 'application/json'})
        try:
            with urllib.request.urlopen(request, context=self.context) as response:
                return response.status, json.load(response)
        except urllib.error.HTTPError as error:
            return error.code, json.load(error)

    def test_round_trip_and_idempotent_ack(self):
        code, event = self.call('/api/events', {'origin': 'android', 'theme': 'dark'})
        self.assertEqual(202, code)
        self.assertEqual('dark', self.call('/api/state')[1]['theme'])
        self.assertEqual(400, self.call('/api/ack', {'eventId': event['eventId'], 'client': 'android'})[0])
        for _ in range(2):
            code, result = self.call('/api/ack', {'eventId': event['eventId'], 'client': 'web'})
            self.assertEqual((200, 'processed'), (code, result['status']))
            self.assertEqual(event['requestId'], result['requestId'])

    def test_superseded_and_invalid_events(self):
        self.assertEqual(400, self.call('/api/events', {'origin': 'web', 'theme': 'unknown'})[0])
        _, first = self.call('/api/events', {'origin': 'web', 'theme': 'dark'})
        self.call('/api/events', {'origin': 'android', 'theme': 'light'})
        self.assertEqual(409, self.call('/api/ack', {'eventId': first['eventId'], 'client': 'android'})[0])
        self.assertEqual('light', self.call('/api/state')[1]['theme'])

if __name__ == '__main__':
    unittest.main()
