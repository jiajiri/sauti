# Sauti Developer API & Cloudflare R2 Model Access Guide

This guide describes how to share model access with external developers, collaborators, and client applications **without risking account takeover, model tampering, or API quota exhaustion**.

---

## 1. Zero-Trust Model Distribution Architecture

```
                       +----------------------------------------------------+
                       |            Cloudflare Account Admin                |
                       |       (Root Account / Full Admin Rights)           |
                       +-------------------------+--------------------------+
                                                 |
                       Issues Scoped Token ONLY  v
                       +----------------------------------------------------+
                       |     Target R2 Bucket: "sauti-models" (Encrypted)   |
                       +-------------------------+--------------------------+
                                                 |
                       +-------------------------+--------------------------+
                       |                                                    |
                       v (Read-Only Token)                                  v (Cloudflare Edge Cache)
+-----------------------------------------------+   +-----------------------------------------------+
|         External Developer / Collaborator     |   |              Production Android App           |
| - Can download weights (GET)                  |   | - Downloads via https://cdn.sauti.ai          |
| - Cannot overwrite weights (PUT -> 403)       |   | - 0 Egress Cost (Cloudflare Edge Cache)       |
| - Cannot delete models (DELETE -> 403)        |   | - Rate limited & WAF protected                |
| - Cannot view billing or account tokens       |   | - Verifies SHA-256 before decryption          |
+-----------------------------------------------+   +-----------------------------------------------+
```

---

## 2. Preventing Account Exhaustion & Model Tampering

To prevent external parties from modifying weights or running up API bills:

### A. Scoped "Object Read-Only" Token
Never share your global Cloudflare API Key or Root R2 Access Key. Generate a scoped token:
1. In Cloudflare Dashboard, navigate to **R2 > Manage R2 API Tokens > Create API Token**.
2. Select **Object Read-Only** (`com.cloudflare.edge.r2.bucket.object.read`).
3. Under **Specify bucket(s)**, select **Only `sauti-models`**.
4. Set a TTL expiration (e.g., 90 or 180 days).

### B. Proof of Tamper Resistance
Give collaborators the generated `developer_r2_readonly.env` file. You can verify the security boundaries using the AWS CLI:

```bash
# 1. Download model or manifest (SUCCEEDS - 200 OK)
aws s3 cp s3://sauti-models/models/latest/manifest.json ./ --profile sauti-readonly

# 2. Attempt to overwrite or upload model (BLOCKED - 403 Forbidden / AccessDenied)
aws s3 cp ./manifest.json s3://sauti-models/models/latest/manifest.json --profile sauti-readonly
# Output: upload failed: (AccessDenied) when calling the PutObject operation

# 3. Attempt to delete model (BLOCKED - 403 Forbidden / AccessDenied)
aws s3 rm s3://sauti-models/models/latest/manifest.json --profile sauti-readonly
# Output: delete failed: (AccessDenied) when calling the DeleteObject operation
```

### C. Quota Protection via Cloudflare Edge Caching
- All `.enc` model weights are configured with `Cache-Control: public, max-age=31536000, immutable`.
- When developers or devices download models, requests hit the nearest Cloudflare Edge PoP.
- **Egress fees are $0**, and repeat requests do not consume backend R2 Class A/B request limits.

---

## 3. Opening the Documentation & API Portal

To explore the codebase KDoc documentation and test model endpoints locally:

### Start the Local Documentation Server
```bash
python3 scripts/serve_docs.py --port 8080 --open
```

### Available Endpoints
- **Developer Portal**: `http://localhost:8080/`
- **Dokka Swagger-style KDoc**: `http://localhost:8080/dokka/`
- **Model Manifest API**: `http://localhost:8080/api/models`
