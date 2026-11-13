#!/usr/bin/env python3
"""
Sauti Sovereign Edge-AI - Local Documentation & API Portal Server

Serves:
1. Interactive Developer API Portal & Swagger-like UI at http://localhost:8080/
2. Compiled Dokka KDoc Codebase Documentation at http://localhost:8080/dokka/
3. Dynamic Model Manifest Inspector at http://localhost:8080/api/models

Zero external dependencies - runs on standard Python 3 http.server.
"""

import os
import sys
import json
import argparse
import webbrowser
from http.server import HTTPServer, SimpleHTTPRequestHandler
from pathlib import Path

# Paths
REPO_ROOT = Path(__file__).resolve().parent.parent
DOKKA_DIR = REPO_ROOT / "android" / "app" / "build" / "dokka" / "html"
MANIFEST_FILE = REPO_ROOT / "encrypted_models_cdn" / "models" / "latest" / "manifest.json"

PORTAL_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Sauti AI - Developer Documentation & API Portal</title>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
  <style>
    :root {
      --bg: #0d1117;
      --card-bg: #161b22;
      --border: #30363d;
      --text: #c9d1d9;
      --text-bright: #ffffff;
      --accent: #58a6ff;
      --accent-green: #2ea043;
      --accent-warn: #d29922;
      --code-bg: #090d13;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif; }
    body { background-color: var(--bg); color: var(--text); line-height: 1.6; padding: 30px 20px; }
    .container { max-width: 1100px; margin: 0 auto; }
    header { border-bottom: 1px solid var(--border); padding-bottom: 20px; margin-bottom: 30px; display: flex; justify-content: space-between; align-items: center; }
    h1 { color: var(--text-bright); font-size: 26px; display: flex; align-items: center; gap: 10px; }
    .badge { background: #1f6feb22; color: var(--accent); border: 1px solid #1f6feb; font-size: 12px; padding: 3px 8px; border-radius: 12px; }
    .nav-buttons { display: flex; gap: 12px; }
    .btn { background: var(--card-bg); border: 1px solid var(--border); color: var(--text-bright); padding: 8px 16px; border-radius: 6px; text-decoration: none; font-size: 14px; font-weight: 500; display: inline-flex; align-items: center; gap: 8px; transition: all 0.2s; }
    .btn:hover { background: #21262d; border-color: #8b949e; }
    .btn-primary { background: #238636; border-color: #2ea043; }
    .btn-primary:hover { background: #2ea043; }
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 20px; margin-bottom: 30px; }
    .card { background: var(--card-bg); border: 1px solid var(--border); border-radius: 8px; padding: 20px; }
    .card h2 { color: var(--text-bright); font-size: 18px; margin-bottom: 12px; display: flex; align-items: center; gap: 8px; }
    .card p { font-size: 14px; color: #8b949e; margin-bottom: 15px; }
    pre { background: var(--code-bg); border: 1px solid var(--border); border-radius: 6px; padding: 12px; font-family: ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, monospace; font-size: 13px; color: #79c0ff; overflow-x: auto; margin-bottom: 15px; }
    .tag { display: inline-block; font-size: 11px; padding: 2px 6px; border-radius: 4px; margin-right: 5px; font-weight: 600; }
    .tag-get { background: #1f6feb22; color: #58a6ff; border: 1px solid #1f6feb; }
    .tag-auth { background: #d2992222; color: #f0883e; border: 1px solid #d29922; }
    .tag-safe { background: #23863622; color: #3fb950; border: 1px solid #238636; }
    .security-notice { background: #161b22; border-left: 4px solid var(--accent-warn); padding: 15px; border-radius: 0 8px 8px 0; margin-bottom: 25px; }
    .security-notice h3 { color: var(--text-bright); font-size: 15px; margin-bottom: 5px; }
    .security-notice p { font-size: 13px; color: #8b949e; }
    footer { text-align: center; font-size: 13px; color: #8b949e; border-top: 1px solid var(--border); padding-top: 20px; margin-top: 40px; }
  </style>
</head>
<body>
  <div class="container">
    <header>
      <div>
        <h1><i class="fa-solid fa-microphone-lines" style="color: #58a6ff;"></i> Sauti Sovereign Edge-AI <span class="badge">API & Architecture Docs</span></h1>
        <p style="color: #8b949e; font-size: 14px; margin-top: 4px;">Developer Documentation Portal, Cloudflare R2 Distribution & Dokka KDoc Explorer</p>
      </div>
      <div class="nav-buttons">
        <a href="/dokka/" class="btn btn-primary" target="_blank"><i class="fa-solid fa-book"></i> Open Dokka KDoc</a>
        <a href="/api/models" class="btn" target="_blank"><i class="fa-solid fa-code"></i> Model Manifest JSON</a>
      </div>
    </header>

    <div class="security-notice">
      <h3><i class="fa-solid fa-shield-halved" style="color: #d29922;"></i> Tamper-Proof Zero-Trust Model Distribution</h3>
      <p>All model weights hosted on Cloudflare R2 are distributed with <strong>Read-Only API Tokens</strong> and immutable edge caching. Collaborators and client devices can download and verify SHA-256 signatures, but cannot delete, overwrite, tamper with weights, or view root Cloudflare account credentials.</p>
    </div>

    <div class="grid">
      <!-- Card 1: Dokka Codebase API -->
      <div class="card">
        <h2><i class="fa-solid fa-code-branch" style="color: #58a6ff;"></i> Dokka KDoc Engine</h2>
        <p>Interactive Swagger-style documentation for Kotlin classes, JNI bridges, DSA language classifiers, and cryptography managers.</p>
        <span class="tag tag-safe">Offline Ready</span>
        <span class="tag tag-get">KDoc v1.9.20</span>
        <div style="margin-top: 15px;">
          <a href="/dokka/" class="btn" style="width: 100%; justify-content: center;"><i class="fa-solid fa-arrow-up-right-from-square"></i> Browse Code Architecture</a>
        </div>
      </div>

      <!-- Card 2: Model Distribution API -->
      <div class="card">
        <h2><i class="fa-solid fa-cloud-arrow-down" style="color: #3fb950;"></i> Model Manifest Endpoint</h2>
        <p>Cryptographically signed delivery manifest specifying model chunk sizes, hash verification, and target hardware tiers.</p>
        <pre><span class="tag tag-get">GET</span> /models/latest/manifest.json</pre>
        <a href="/api/models" class="btn" style="width: 100%; justify-content: center;"><i class="fa-solid fa-eye"></i> View Live Manifest</a>
      </div>

      <!-- Card 3: Showkase UI Explorer -->
      <div class="card">
        <h2><i class="fa-solid fa-mobile-screen" style="color: #f0883e;"></i> Showkase UI Catalog</h2>
        <p>Interactive in-app visual component browser generated by Airbnb Showkase. Run via ADB on your connected device/emulator:</p>
        <pre>adb shell am start -n \\
  com.sauti.app/\\
  com.airbnb.android.showkase.ui.ShowkaseBrowserActivity</pre>
      </div>
    </div>

    <div class="card" style="margin-bottom: 25px;">
      <h2><i class="fa-solid fa-terminal" style="color: #58a6ff;"></i> Developer Read-Only Model Download Guide</h2>
      <p>Collaborators can download model weights via AWS CLI or S3-compatible SDKs without access to write or delete APIs:</p>
      <pre># 1. Configure scoped read-only credentials in ~/.aws/credentials
[sauti-readonly]
aws_access_key_id = &lt;SCOPED_READONLY_KEY_ID&gt;
aws_secret_access_key = &lt;SCOPED_READONLY_SECRET&gt;

# 2. Download encrypted models safely
aws s3 cp s3://sauti-models/models/latest/whisper-swahili-small-q4_1.bin.enc ./ \\
  --endpoint-url https://&lt;ACCOUNT_ID&gt;.r2.cloudflarestorage.com \\
  --profile sauti-readonly

# 3. Tamper verification test: Attempts to delete or upload are blocked with AccessDenied
aws s3 rm s3://sauti-models/models/latest/manifest.json --profile sauti-readonly
# Output: (AccessDenied) when calling the DeleteObject operation (TAMPER-PROOF!)</pre>
    </div>

    <footer>
      <p>Sauti Sovereign Edge-AI Platform &copy; 2026. Licensed under Apache 2.0. Zero Internet Permission Model.</p>
    </footer>
  </div>
</body>
</html>
"""


class SautiDocsHandler(SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path in ("/", "/docs", "/index.html"):
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            self.wfile.write(PORTAL_HTML.encode("utf-8"))
            return

        if self.path.startswith("/api/models"):
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.end_headers()
            if MANIFEST_FILE.exists():
                with open(MANIFEST_FILE, "r") as f:
                    self.wfile.write(f.read().encode("utf-8"))
            else:
                sample_manifest = {
                    "manifest_version": "1.0.0",
                    "status": "ready",
                    "models": {
                        "voice_sw": {
                            "name": "whisper-swahili-small-q4_1",
                            "file": "whisper-swahili-small-q4_1.bin.enc",
                            "size_bytes": 160756736,
                            "sha256": "806ec54737213454b52994f7d49811802d20364d99042b4759080c98f921ea6e"
                        },
                        "core_pro": {
                            "name": "swahili-gemma-1b-q4_k_m",
                            "file": "swahili-gemma-1b-q4_k_m.gguf.enc",
                            "size_bytes": 786432000,
                            "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
                        }
                    }
                }
                self.wfile.write(json.dumps(sample_manifest, indent=2).encode("utf-8"))
            return

        if self.path.startswith("/dokka"):
            subpath = self.path[len("/dokka"):]
            if not subpath or subpath == "/" or subpath == "":
                subpath = "/index.html"
            target_file = DOKKA_DIR / subpath.lstrip("/")
            if target_file.is_dir():
                target_file = target_file / "index.html"
            
            if target_file.exists() and target_file.is_file():
                content_type = "text/html"
                if target_file.suffix == ".css":
                    content_type = "text/css"
                elif target_file.suffix == ".js":
                    content_type = "application/javascript"
                elif target_file.suffix in (".png", ".jpg", ".jpeg", ".svg"):
                    content_type = f"image/{target_file.suffix.lstrip('.')}"

                self.send_response(200)
                self.send_header("Content-Type", f"{content_type}; charset=utf-8")
                self.end_headers()
                with open(target_file, "rb") as f:
                    self.wfile.write(f.read())
                return
            else:
                self.send_response(404)
                self.send_header("Content-Type", "text/html; charset=utf-8")
                self.end_headers()
                self.wfile.write(b"<h1>404 - Dokka File Not Found</h1>")
                return

        return super().do_GET()


def main():
    parser = argparse.ArgumentParser(description="Serve Sauti Developer API and Dokka documentation portal")
    parser.add_argument("--port", type=int, default=8080, help="Port to listen on (default: 8080)")
    parser.add_argument("--host", type=str, default="127.0.0.1", help="Host interface to bind (default: 127.0.0.1)")
    parser.add_argument("--open", action="store_true", help="Automatically open browser tab on launch")
    args = parser.parse_args()

    os.chdir(REPO_ROOT)
    server_address = (args.host, args.port)
    httpd = HTTPServer(server_address, SautiDocsHandler)

    print(f"=================================================================")
    print(f" Sauti Sovereign Edge-AI Documentation & API Server")
    print(f"=================================================================")
    print(f" Portal URL:    http://{args.host}:{args.port}/")
    print(f" Dokka KDoc:    http://{args.host}:{args.port}/dokka/")
    print(f" Models API:    http://{args.host}:{args.port}/api/models")
    print(f"=================================================================")
    print(f" Press Ctrl+C to stop the server.")

    if args.open:
        webbrowser.open(f"http://{args.host}:{args.port}/")

    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping documentation server.")
        httpd.server_close()


if __name__ == "__main__":
    main()
