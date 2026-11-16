#!/usr/bin/env python3
"""Servidor HTTPS local para demostrar eventos entre Android y la web."""
import json
import ssl
import threading
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parent
lock = threading.Lock()
state = {"theme": "light", "events": []}

class Handler(BaseHTTPRequestHandler):
    def send_data(self, status, data, content_type="application/json; charset=utf-8"):
        payload = json.dumps(data, ensure_ascii=False).encode() if isinstance(data, dict) else data
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(payload)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(payload)

    def do_GET(self):
        path = urlparse(self.path).path
        if path == "/":
            self.send_data(200, (ROOT / "index.html").read_bytes(), "text/html; charset=utf-8")
        elif path == "/api/state":
            with lock:
                self.send_data(200, {"theme": state["theme"], "events": state["events"][-100:]})
        else:
            self.send_data(404, {"error": "Ruta desconocida"})

    def do_POST(self):
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if not 0 < length <= 4096:
                raise ValueError("Tamaño inválido")
            body = json.loads(self.rfile.read(length))
            if not isinstance(body, dict):
                raise ValueError("Se esperaba un objeto JSON")
        except (ValueError, UnicodeError):
            return self.send_data(400, {"error": "JSON inválido"})
        path = urlparse(self.path).path
        with lock:
            if path == "/api/events":
                theme, origin = body.get("theme"), body.get("origin")
                if theme not in ("light", "dark") or origin not in ("android", "web"):
                    return self.send_data(400, {"error": "Tema u origen inválido"})
                event = {"eventId": str(uuid.uuid4()), "requestId": str(uuid.uuid4()),
                         "theme": theme, "origin": origin, "status": "accepted"}
                for previous in state["events"]:
                    if previous["status"] == "accepted":
                        previous["status"] = "superseded"
                state["theme"] = theme
                state["events"].append(event)
                print(f"REQUEST {origin} {theme} requestId={event['requestId']} eventId={event['eventId']} RESPONSE 202", flush=True)
                return self.send_data(202, event.copy())
            if path == "/api/ack":
                event = next ( (e for e in state["events"] if e["eventId"] == body.get("eventId")), None)
                client = body.get("client")
                if event is None:
                    return self.send_data(404, {"error": "Evento desconocido"})
                expected = "web" if event["origin"] == "android" else "android"
                if client != expected:
                    return self.send_data(400, {"error": "Destinatario incorrecto"})
                if event["status"] == "superseded":
                    return self.send_data(409, {"error": "Evento reemplazado por uno más reciente"})
                if event["status"] != "processed":
                    event["status"] = "processed"
                    print(f"ACK {client} requestId={event['requestId']} eventId={event['eventId']} RESPONSE 200", flush=True)
                return self.send_data(200, event.copy())
        self.send_data(404, {"error": "Ruta desconocida"})

if __name__ == "__main__":
    server = ThreadingHTTPServer(("127.0.0.1", 8443), Handler)
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.load_cert_chain(ROOT / "certs/server.pem", ROOT / "certs/server.key")
    server.socket = context.wrap_socket(server.socket, server_side=True)
    print("Servidor de demostración: https://localhost:8443", flush=True)
    server.serve_forever()
