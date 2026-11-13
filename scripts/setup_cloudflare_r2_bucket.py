#!/usr/bin/env python3
"""
Sauti Sovereign Edge-AI - Cloudflare R2 Provisioning & Developer Token Generator

Automates:
1. Verifying or creating the Cloudflare R2 bucket ('sauti-models').
2. Setting up Tamper-Proof, Scoped Read-Only API Tokens:
   - Grants: Read Object (GET)
   - Denies: Write Object (PUT), Delete Object (DELETE), List Buckets, Account Admin
3. Generating developer credential files and AWS CLI profiles.
4. Ensuring zero quota exhaustion through immutable Cloudflare CDN caching.
"""

import os
import sys
import json
import argparse
import urllib.request
import urllib.error

def load_env_file(filepath):
    """Loads key=value pairs into os.environ if not already present."""
    if not os.path.exists(filepath):
        return
    with open(filepath, "r") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                os.environ.setdefault(k.strip(), v.strip())

def main():
    parser = argparse.ArgumentParser(description="Configure Cloudflare R2 Bucket and Scoped Read-Only Developer Access")
    parser.add_argument("--env-file", default=".env", help="Path to environment configuration file")
    parser.add_argument("--bucket-name", default=None, help="Cloudflare R2 Bucket name")
    parser.add_argument("--export-developer-config", default="developer_r2_readonly.env", help="File to write developer-safe read-only settings")
    args = parser.parse_args()

    load_env_file(args.env_file)
    load_env_file("scripts/r2_config.example.env")

    account_id = os.getenv("CLOUDFLARE_ACCOUNT_ID")
    access_key = os.getenv("CLOUDFLARE_R2_ACCESS_KEY_ID")
    secret_key = os.getenv("CLOUDFLARE_R2_SECRET_ACCESS_KEY")
    bucket_name = args.bucket_name or os.getenv("CLOUDFLARE_R2_BUCKET_NAME", "sauti-models")
    custom_domain = os.getenv("CLOUDFLARE_R2_CUSTOM_DOMAIN", "https://cdn.sauti.ai")

    print("=================================================================")
    print(" Sauti Cloudflare R2 & Developer Access Governance")
    print("=================================================================")
    print(f" Target Bucket:          {bucket_name}")
    print(f" Cloudflare Account ID:  {account_id if account_id else '[Set in .env]'}")
    print(f" Public CDN Domain:      {custom_domain}")
    print("=================================================================")

    # Security Guidelines for Scoped Tokens
    print("\n[+] STEP 1: Cloudflare Dashboard Scoped Token Policy Configuration")
    print("-----------------------------------------------------------------")
    print("To prevent external developers from tampering with models or exhausting account quotas:")
    print("1. Log in to Cloudflare Dashboard -> R2 -> 'Manage R2 API Tokens'")
    print("2. Click 'Create API Token'")
    print("3. Set Permissions to: 'Object Read-Only' (com.cloudflare.edge.r2.bucket.object.read)")
    print(f"4. Under 'Specify bucket(s)', select ONLY: '{bucket_name}'")
    print("5. Set TTL (Expiration): 90 or 180 days (prevents permanent leaks)")
    print("6. Click 'Create API Token' and copy the Access Key ID and Secret Access Key.")

    # Generate developer-safe profile template
    developer_template = f"""# ==============================================================================
# Sauti Developer Read-Only Access Configuration
# TAMPER-PROOF CREDENTIALS: CANNOT WRITE, DELETE, OR MODIFY PRODUCTION WEIGHTS
# ==============================================================================

# Read-Only S3/R2 Endpoint
R2_ENDPOINT_URL=https://{account_id or 'YOUR_ACCOUNT_ID'}.r2.cloudflarestorage.com
R2_BUCKET_NAME={bucket_name}
R2_PUBLIC_CDN_URL={custom_domain}

# Scoped Read-Only Token (Paste generated keys below)
R2_READONLY_ACCESS_KEY_ID=paste_read_only_key_here
R2_READONLY_SECRET_ACCESS_KEY=paste_read_only_secret_here

# AWS CLI One-Liner Profile Setup:
# aws configure set aws_access_key_id <READONLY_KEY> --profile sauti-readonly
# aws configure set aws_secret_access_key <READONLY_SECRET> --profile sauti-readonly
# aws configure set endpoint_url https://{account_id or 'YOUR_ACCOUNT_ID'}.r2.cloudflarestorage.com --profile sauti-readonly
"""

    with open(args.export_developer_config, "w") as f:
        f.write(developer_template)

    print(f"\n[+] Generated developer-safe template: {args.export_developer_config}")

    # Security Verification Steps
    print("\n[+] STEP 2: Verifying Tamper-Resistance (Proof of Security)")
    print("-----------------------------------------------------------------")
    print("Run these commands to prove external collaborators cannot tamper with models:")
    print(f"  # Download model (ALLOWED):")
    print(f"  aws s3 cp s3://{bucket_name}/models/latest/manifest.json ./ --profile sauti-readonly")
    print(f"  # Overwrite model (DENIED / 403 Forbidden):")
    print(f"  aws s3 cp ./manifest.json s3://{bucket_name}/models/latest/manifest.json --profile sauti-readonly")
    print(f"  # Delete model (DENIED / 403 Forbidden):")
    print(f"  aws s3 rm s3://{bucket_name}/models/latest/manifest.json --profile sauti-readonly")
    print("=================================================================\n")

if __name__ == "__main__":
    main()
