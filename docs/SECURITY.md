# Security Policy

Sauti takes security and user privacy seriously. Because Sauti operates as an offline, sovereign edge-AI platform handling on-device voice processing, maintaining strict local storage isolation and zero data exfiltration is a core design principle.

---

## Supported Versions

Security updates and bug fixes are provided for the following releases:

| Version | Supported |
| :--- | :--- |
| 1.0.x | Yes |
| < 1.0 | No |

---

## Air-Gapped Security Architecture

Sauti adheres to the following privacy and security principles:

- **Zero Network Permissions**: The application intentionally omits `android.permission.INTERNET` from its manifest. The application cannot communicate over the internet or send data to external cloud services.
- **Local Storage Encryption**: User chat history, preferences, and cached state are encrypted at rest using 256-bit AES via SQLCipher.
- **Hardware-Backed Keystore**: Cryptographic keys are anchored to the Android Keystore system (TEE / StrongBox where available on the device).

---

## Reporting a Vulnerability

If you discover a security vulnerability or privacy flaw in Sauti, please do not report it through public GitHub issues.

Instead, please report it via one of the following methods:

1. **Email**: Send details to `laphilemon.tech@gmail.com`.
2. **GitHub Security Advisories**: Use the private vulnerability reporting feature on GitHub under the repository's Security tab.

### What to Include in Your Report
To help us triage and resolve the issue quickly, please provide:
- A description of the vulnerability and its potential impact
- Detailed steps to reproduce the issue or proof-of-concept code
- The Android version, device model, and APK version tested
- Any suggested mitigations or patches

### Response Timeline
- **Initial Response**: We will acknowledge receipt of your report within 48 hours.
- **Status Updates**: We will provide periodic updates as the issue is investigated and patched.
- **Public Disclosure**: We will coordinate with you on public disclosure after a fix has been verified and released.
