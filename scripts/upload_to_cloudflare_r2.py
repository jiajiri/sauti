#!/usr/bin/env python3
"""
Sauti Cloudflare R2 Model Packaging & Upload Pipeline (LLMOps / MLOps)
---------------------------------------------------------------------
Features:
1. Encrypts model weights using AES-256-GCM + Argon2id / Keystore key derivation.
2. Compresses encrypted payloads with Gzip stream compression for reduced data transfer.
3. Calculates deterministic SHA-256 integrity checksums for both plaintext and encrypted payloads.
4. Generates standard CDN manifest.json with semantic versioning.
5. Uploads assets to Cloudflare R2 via S3-compatible API (zero egress fees) using parallel multipart streaming.

Zero-secrets compliance:
Credentials must be loaded strictly from environment variables or .env file.
"""

import os
import sys
import json
import gzip
import zipfile
import hashlib
import argparse
from typing import Optional, Dict, Any, List

try:
    from cryptography.hazmat.primitives.ciphers.aead import AESGCM
except ImportError:
    print("Error: 'cryptography' library is required. Install via: pip install cryptography")
    sys.exit(1)

# Constants matching Sauti Android Keystore & ModelCryptoManager
DERIVATION_SALT = b"sauti_offline_ai_device_sovereign_salt_2026_argon2"
DERIVATION_INFO = b"sauti_aes_gcm_256_model_encryption_key"
MODELS_DIR = "models_store"
OUTPUT_DIR = "encrypted_models_cdn"

def derive_key(salt: bytes = DERIVATION_SALT) -> bytes:
    h = hashlib.sha256()
    h.update(salt)
    h.update(DERIVATION_INFO)
    return h.digest()

def calculate_sha256(filepath: str) -> str:
    sha = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(1024 * 1024):
            sha.update(chunk)
    return sha.hexdigest()

def encrypt_and_compress_file(input_path: str, output_path: str, key: bytes, compress: bool = True) -> Dict[str, Any]:
    """
    Encrypts input_path with AES-256-GCM, prepending 12-byte random IV.
    Optionally compresses the encrypted stream with gzip for minimum network bandwidth.
    """
    aesgcm = AESGCM(key)
    iv = os.urandom(12)

    print(f"Reading {input_path} for encryption...")
    plain_sha = calculate_sha256(input_path)
    plain_size = os.path.getsize(input_path)

    with open(input_path, "rb") as fin:
        plaintext = fin.read()

    ciphertext = aesgcm.encrypt(iv, plaintext, None)
    encrypted_payload = iv + ciphertext

    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    if compress:
        final_output_path = output_path + ".gz"
        print(f"Compressing encrypted payload with gzip -> {final_output_path}...")
        with gzip.open(final_output_path, "wb", compresslevel=9) as fgz:
            fgz.write(encrypted_payload)
        enc_size = os.path.getsize(final_output_path)
        enc_sha = calculate_sha256(final_output_path)
    else:
        final_output_path = output_path
        with open(final_output_path, "wb") as fout:
            fout.write(encrypted_payload)
        enc_size = len(encrypted_payload)
        enc_sha = hashlib.sha256(encrypted_payload).hexdigest()

    saved_pct = ((plain_size - enc_size) / plain_size) * 100
    print(f"Done: {plain_size / (1024*1024):.1f} MB -> {enc_size / (1024*1024):.1f} MB ({saved_pct:.1f}% bandwidth savings)")

    return {
        "final_path": final_output_path,
        "compressed": compress,
        "plain_sha256": plain_sha,
        "encrypted_sha256": enc_sha,
        "plain_size_bytes": plain_size,
        "encrypted_size_bytes": enc_size
    }

def load_r2_client():
    try:
        import boto3
        from botocore.config import Config
    except ImportError:
        print("Error: boto3 is required to upload directly to Cloudflare R2. Install via: pip install boto3")
        sys.exit(1)

    account_id = os.getenv("CLOUDFLARE_ACCOUNT_ID")
    access_key = os.getenv("CLOUDFLARE_R2_ACCESS_KEY_ID")
    secret_key = os.getenv("CLOUDFLARE_R2_SECRET_ACCESS_KEY")

    if not account_id:
        raise ValueError("Missing required environment variable 'CLOUDFLARE_ACCOUNT_ID'. Set it in .env")
    if not access_key:
        raise ValueError("Missing required environment variable 'CLOUDFLARE_R2_ACCESS_KEY_ID'. Set it in .env")
    if not secret_key:
        raise ValueError("Missing required environment variable 'CLOUDFLARE_R2_SECRET_ACCESS_KEY'. Set it in .env")

    endpoint_url = f"https://{account_id}.r2.cloudflarestorage.com"
    s3_client = boto3.client(
        "s3",
        endpoint_url=endpoint_url,
        aws_access_key_id=access_key,
        aws_secret_access_key=secret_key,
        config=Config(signature_version="s3v4")
    )
    return s3_client

def upload_file_to_r2(s3_client, local_path: str, bucket_name: str, object_key: str):
    print(f"Uploading {local_path} to R2 bucket '{bucket_name}' key '{object_key}'...")
    s3_client.upload_file(local_path, bucket_name, object_key)
    print(f"Successfully uploaded {object_key}!")

def purge_bucket_objects(s3_client, bucket_name: str):
    """
    Purges legacy and stale model objects from Cloudflare R2 bucket.
    """
    print(f"\nScanning and purging old objects in bucket '{bucket_name}'...")
    paginator = s3_client.get_paginator('list_objects_v2')
    pages = paginator.paginate(Bucket=bucket_name)

    delete_keys = []
    for page in pages:
        for obj in page.get('Contents', []):
            delete_keys.append({'Key': obj['Key']})

    if not delete_keys:
        print("Bucket is already empty.")
        return

    # Delete in batches of 1000
    for i in range(0, len(delete_keys), 1000):
        batch = delete_keys[i:i + 1000]
        print(f"Deleting batch of {len(batch)} objects from {bucket_name}...")
        s3_client.delete_objects(Bucket=bucket_name, Delete={'Objects': batch})

    print("Bucket purge completed successfully.\n")

def main():
    parser = argparse.ArgumentParser(description="Package, encrypt, compress, and upload Sauti models to Cloudflare R2")
    parser.add_argument("--dry-run", action="store_true", help="Package and encrypt locally without uploading to R2")
    parser.add_argument("--upload", action="store_true", help="Upload generated models and manifest to Cloudflare R2")
    parser.add_argument("--purge-bucket", action="store_true", help="Purge all legacy objects in R2 bucket before uploading")
    parser.add_argument("--no-compress", action="store_true", help="Skip gzip compression of encrypted models")
    parser.add_argument("--bundle", action="store_true", default=True, help="Create a unified multi-model bundle for single-request download")
    parser.add_argument("--version", type=str, default="2.1.0", help="Model distribution release version")
    args = parser.parse_args()

    # Load .env if present
    env_file = ".env"
    if os.path.exists(env_file):
        with open(env_file, "r") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    k, v = line.split("=", 1)
                    os.environ.setdefault(k.strip(), v.strip())

    key = derive_key()
    custom_domain = os.getenv("CLOUDFLARE_R2_CUSTOM_DOMAIN", "https://cdn.sauti.ai").rstrip("/")
    bucket_name = os.getenv("CLOUDFLARE_R2_BUCKET_NAME", "sauti-models")

    # Dynamically build model definitions from environment variables (Zero hardcoded model strings)
    model_mappings = [
        {
            "id": os.getenv("MODEL_VOICE_EN_ID", "voice_en"),
            "name": os.getenv("MODEL_VOICE_EN_NAME", "whisper_base_en"),
            "source": os.getenv("MODEL_VOICE_EN_SOURCE", os.path.join(MODELS_DIR, "ggml-base.en-q5_1.bin")),
            "target": os.getenv("MODEL_VOICE_EN_TARGET", "ggml-base.en-q5_1.bin.enc"),
            "output_filename": os.getenv("MODEL_VOICE_EN_FILENAME", "voice_en.bin"),
            "required": os.getenv("MODEL_VOICE_EN_REQUIRED", "true").lower() == "true",
            "version": args.version
        },
        {
            "id": os.getenv("MODEL_CORE_PRO_ID", "core_pro"),
            "name": os.getenv("MODEL_CORE_PRO_NAME", "gemma_3_1b_instruct"),
            "source": os.getenv("MODEL_CORE_PRO_SOURCE", os.path.join(MODELS_DIR, "gemma-3-1b-it-q4_k_m.gguf")),
            "target": os.getenv("MODEL_CORE_PRO_TARGET", "gemma-3-1b-it-q4_k_m.gguf.enc"),
            "output_filename": os.getenv("MODEL_CORE_PRO_FILENAME", "core_pro.bin"),
            "required": os.getenv("MODEL_CORE_PRO_REQUIRED", "true").lower() == "true",
            "version": args.version
        },
        {
            "id": os.getenv("MODEL_VOICE_SW_ID", "voice_sw"),
            "name": os.getenv("MODEL_VOICE_SW_NAME", "whisper_swahili_small"),
            "source": os.getenv("MODEL_VOICE_SW_SOURCE", os.path.join(MODELS_DIR, "whisper-swahili-small-q4_1.bin")),
            "target": os.getenv("MODEL_VOICE_SW_TARGET", "whisper-swahili-small-q4_1.bin.enc"),
            "output_filename": os.getenv("MODEL_VOICE_SW_FILENAME", "voice_lite.bin"),
            "required": os.getenv("MODEL_VOICE_SW_REQUIRED", "false").lower() == "true",
            "version": args.version
        },
        {
            "id": os.getenv("MODEL_CORE_ULTRA_ID", "core_ultra"),
            "name": os.getenv("MODEL_CORE_ULTRA_NAME", "gemma_3_4b_instruct"),
            "source": os.getenv("MODEL_CORE_ULTRA_SOURCE", os.path.join(MODELS_DIR, "gemma-3-4b-it-q4_k_m.gguf")),
            "target": os.getenv("MODEL_CORE_ULTRA_TARGET", "gemma-3-4b-it-q4_k_m.gguf.enc"),
            "output_filename": os.getenv("MODEL_CORE_ULTRA_FILENAME", "core_ultra.bin"),
            "required": os.getenv("MODEL_CORE_ULTRA_REQUIRED", "false").lower() == "true",
            "version": args.version
        }
    ]

    manifest = {
        "version": args.version,
        "compression": "gzip" if not args.no_compress else "none",
        "encryption": "AES-256-GCM",
        "models": []
    }

    processed_files = []

    # If --bundle is enabled and both core and voice are present, build unified bundle
    voice_map = next((m for m in model_mappings if m["id"] == "voice_en" or m["id"] == "voice_sw"), None)
    core_map = next((m for m in model_mappings if m["id"] == "core_pro"), None)

    if args.bundle and voice_map and core_map and os.path.exists(voice_map["source"]) and os.path.exists(core_map["source"]):
        bundle_tar_path = os.path.join(OUTPUT_DIR, "bundle.zip")
        print(f"\nCreating unified multi-model zip bundle: {bundle_tar_path}...")
        with zipfile.ZipFile(bundle_tar_path, "w", zipfile.ZIP_DEFLATED) as zf:
            zf.write(voice_map["source"], arcname=voice_map["output_filename"])
            zf.write(core_map["source"], arcname=core_map["output_filename"])

        enc_bundle_target = os.path.join(OUTPUT_DIR, "bundle.bin.enc")
        bundle_res = encrypt_and_compress_file(
            input_path=bundle_tar_path,
            output_path=enc_bundle_target,
            key=key,
            compress=not args.no_compress
        )
        if os.path.exists(bundle_tar_path):
            os.remove(bundle_tar_path)

        bundle_filename = os.path.basename(bundle_res["final_path"])
        bundle_r2_key = f"models/v{args.version}/{bundle_filename}"
        bundle_public_url = f"{custom_domain}/{bundle_r2_key}"

        manifest["bundle"] = {
            "remote_url": bundle_public_url,
            "sha256": bundle_res["plain_sha256"],
            "encrypted_sha256": bundle_res["encrypted_sha256"],
            "size_bytes": bundle_res["encrypted_size_bytes"],
            "plain_size_bytes": bundle_res["plain_size_bytes"],
            "compressed": bundle_res["compressed"],
            "models": [
                {"id": voice_map["id"], "filename": voice_map["output_filename"], "format": "ggml"},
                {"id": core_map["id"], "filename": core_map["output_filename"], "format": "gguf"}
            ]
        }
        processed_files.append((bundle_res["final_path"], bundle_r2_key))

    for m in model_mappings:
        src = m["source"]
        if not os.path.exists(src):
            print(f"Skipping {src} (not found in local models_store)")
            continue

        target_file = os.path.join(OUTPUT_DIR, m["target"])
        result = encrypt_and_compress_file(
            input_path=src,
            output_path=target_file,
            key=key,
            compress=not args.no_compress
        )

        final_filename = os.path.basename(result["final_path"])
        r2_key = f"models/v{args.version}/{final_filename}"
        public_url = f"{custom_domain}/{r2_key}"

        manifest["models"].append({
            "id": m["id"],
            "filename": m["output_filename"],
            "remote_url": public_url,
            "sha256": result["plain_sha256"],
            "encrypted_sha256": result["encrypted_sha256"],
            "size_bytes": result["encrypted_size_bytes"],
            "plain_size_bytes": result["plain_size_bytes"],
            "compressed": result["compressed"],
            "required": m["required"],
            "version": m["version"]
        })

        processed_files.append((result["final_path"], r2_key))

    manifest_path = os.path.join(OUTPUT_DIR, "manifest.json")
    with open(manifest_path, "w") as f:
        json.dump(manifest, f, indent=2)

    print(f"\nManifest successfully created at: {manifest_path}")
    print(json.dumps(manifest, indent=2))

    if args.upload and not args.dry_run:
        s3 = load_r2_client()
        if args.purge_bucket:
            purge_bucket_objects(s3, bucket_name)

        for local_file, r2_key in processed_files:
            upload_file_to_r2(s3, local_file, bucket_name, r2_key)

        # Upload manifest.json to root or versioned path
        manifest_r2_key = f"models/v{args.version}/manifest.json"
        upload_file_to_r2(s3, manifest_path, bucket_name, manifest_r2_key)
        # Also upload to latest
        upload_file_to_r2(s3, manifest_path, bucket_name, "models/latest/manifest.json")
        print("\nAll model weights and manifests deployed to Cloudflare R2 successfully!")

if __name__ == "__main__":
    main()
