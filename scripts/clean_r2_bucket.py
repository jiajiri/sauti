#!/usr/bin/env python3
import os
import sys
import json
import boto3
from botocore.config import Config

def main():
    env_file = '.env'
    if os.path.exists(env_file):
        with open(env_file) as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith('#') and '=' in line:
                    k, v = line.split('=', 1)
                    os.environ.setdefault(k.strip(), v.strip())

    account_id = os.getenv('CLOUDFLARE_ACCOUNT_ID')
    access_key = os.getenv('CLOUDFLARE_R2_ACCESS_KEY_ID')
    secret_key = os.getenv('CLOUDFLARE_R2_SECRET_ACCESS_KEY')
    bucket_name = os.getenv('CLOUDFLARE_R2_BUCKET_NAME', 'sauti-models')

    if not account_id or not access_key or not secret_key:
        print("Missing required environment variables for Cloudflare R2.")
        sys.exit(1)

    endpoint_url = f'https://{account_id}.r2.cloudflarestorage.com'
    s3 = boto3.client(
        's3',
        endpoint_url=endpoint_url,
        aws_access_key_id=access_key,
        aws_secret_access_key=secret_key,
        config=Config(signature_version='s3v4')
    )

    print(f"Inspecting Cloudflare R2 bucket: {bucket_name}")
    res = s3.list_objects_v2(Bucket=bucket_name)
    contents = res.get('Contents', [])
    print(f"Current objects count: {len(contents)}")
    for obj in contents:
        print(f" - {obj['Key']} ({obj['Size'] / (1024*1024):.2f} MB)")

    # 1. Remove the redundant 57MB model
    key_to_delete = 'models/v1.0.0/ggml-base.en-q5_1.bin.enc.gz'
    print(f"\nDeleting redundant model: {key_to_delete}...")
    s3.delete_object(Bucket=bucket_name, Key=key_to_delete)
    print("Deleted successfully.")

    # 2. Update manifest to clean structure (only exactly 2 models: voice_lite and core_pro)
    manifest_obj = s3.get_object(Bucket=bucket_name, Key='models/latest/manifest.json')
    manifest_data = json.loads(manifest_obj['Body'].read().decode('utf-8'))

    # Keep only required production models
    clean_models = [m for m in manifest_data.get('models', []) if m.get('id') != 'voice_en']
    manifest_data['models'] = clean_models

    cleaned_manifest_json = json.dumps(manifest_data, indent=2).encode('utf-8')

    print("\nUpdating manifests with clean 2-model structure...")
    s3.put_object(Bucket=bucket_name, Key='models/latest/manifest.json', Body=cleaned_manifest_json, ContentType='application/json')
    s3.put_object(Bucket=bucket_name, Key='models/v1.0.0/manifest.json', Body=cleaned_manifest_json, ContentType='application/json')
    print("Manifests updated successfully.")

    # 3. Final verification of bucket contents
    print("\nVerified clean bucket contents:")
    final_res = s3.list_objects_v2(Bucket=bucket_name)
    for obj in final_res.get('Contents', []):
        print(f" - {obj['Key']} ({obj['Size'] / (1024*1024):.2f} MB)")

if __name__ == '__main__':
    main()
