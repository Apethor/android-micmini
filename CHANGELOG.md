# Changelog

All notable changes to this project are documented here.
Releases are cut automatically from `v*` tags (see `.github/workflows/release.yml`).

## v1.1 — 2026-07-01
- Generic package `com.apethor.micmini` (previously tied to a specific device).
- `MicService`: `volatile` server field; optional `BIND_LOOPBACK` (listen on 127.0.0.1 only);
  clearer AudioRecord init-failure log; documented the fixed sample-rate wire contract and the
  single-client backlog behavior.
- English comments throughout; README rewritten in English (generic `<PHONE_IP>`, quick-start,
  data-flow diagram, config table, troubleshooting, security section).
- `receiver_example.py`: host via CLI argument, optional stdout pipe.
- CI/CD: Android lint in CI, release-on-tag, automated Claude PR review, Dependabot,
  PR template, CODEOWNERS.

## v1.0 — 2026-07-01
- Initial release: Android microphone → raw PCM (s16le, mono, 16 kHz) over TCP port 6000.
- Foreground service + auto-start on boot. No root. minSdk 23 (Android 6.0).
