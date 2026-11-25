# Security policy

## Threat model and privacy posture

Tithimala runs offline by default. It collects zero user analytics, telemetry, crash reports, or device identifiers.

All reminders, preferences, and cached calendar data stay on the device in local Room and DataStore storage. The app requires no user account and performs no cloud sync.

The sole optional network request is a weekly background fetch of a static `calendar-data.json` file for updated festival, holiday, and tithi data. Before the app applies a downloaded file, it validates the JSON schema, checks the data version, and verifies the SHA-256 checksum. Users can disable automatic data updates at any time in Settings.

## Supported versions

Security fixes apply to the latest release of Tithimala.

| Version | Supported |
| --- | --- |
| Latest release | Yes |
| Older releases | No |

## Reporting a vulnerability

Do not open a public GitHub issue for security vulnerabilities.

Report vulnerabilities privately through [GitHub Security Advisories](https://github.com/springmusk026/tithimala/security/advisories/new) on `springmusk026/tithimala`.

Include as much of the following detail as possible:

- Affected Tithimala version or commit hash.
- Component or module involved (for example, data update verification, local storage, or notification handling).
- Steps to reproduce or a proof of concept.
- Potential impact and any suggested mitigation.

## Response timeline

- Initial acknowledgment within 3 business days.
- Triage and severity assessment within 7 business days.
- Coordinated disclosure after a fix is ready and released, or after 90 days if unresolved.
