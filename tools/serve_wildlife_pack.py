"""Single-file, loopback-only pack delivery for clients on the server's PC.

Usage: python tools/serve_wildlife_pack.py build/packs/Wildlands-Java-Wildlife-0.10.0-wildlife.3.zip
For remote clients host the pack over HTTPS instead. This helper does not serve directories.
"""
import argparse
import hashlib
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit
import zipfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("pack", type=Path)
    parser.add_argument("--port", type=int, default=8765)
    args = parser.parse_args()
    if not 1024 <= args.port <= 65535:
        parser.error("port must be 1024–65535")
    with zipfile.ZipFile(args.pack) as archive:
        if "pack.mcmeta" not in archive.namelist() or archive.testzip():
            parser.error("pack must be a valid Java resource-pack ZIP")
    data = args.pack.read_bytes()

    class PackHandler(BaseHTTPRequestHandler):
        def respond(self, body):
            if urlsplit(self.path).path != "/wildlands-java.zip":
                self.send_error(404)
                return
            self.send_response(200)
            self.send_header("Content-Type", "application/zip")
            self.send_header("Content-Length", str(len(data)))
            self.send_header("Cache-Control", "no-cache")
            self.end_headers()
            if body:
                try:
                    self.wfile.write(data)
                except (BrokenPipeError, ConnectionResetError):
                    pass

        def do_GET(self):
            self.respond(True)

        def do_HEAD(self):
            self.respond(False)

    server = ThreadingHTTPServer(("127.0.0.1", args.port), PackHandler)
    print(f"java:\n  url: 'http://127.0.0.1:{args.port}/wildlands-java.zip'\n  sha1: '{hashlib.sha1(data).hexdigest()}'", flush=True)
    print("Clients must run on this PC. Keep this terminal open; Ctrl+C stops delivery.", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
