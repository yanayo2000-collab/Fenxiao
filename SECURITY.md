# Security Policy

## Reporting

Do not open a public issue containing credentials, personal data, payment identity, production logs with tokens, or exploit details. Report security concerns directly to the repository owner through a private channel.

## Secrets

- Never commit production `.env` files, passwords, session cookies, OTPs, API tokens, HMAC secrets, database credentials, SSH private keys, or unmasked user/payment data.
- Use environment injection or an approved secret manager.
- If a secret reaches Git history, treat it as compromised: revoke or rotate it before attempting repository cleanup.

## Production changes

- Start from the current frozen production source and apply only the reviewed task diff.
- Preserve immutable Flyway migrations already applied in production; add a new migration instead.
- Create a release identifier, backup, manifest, hashes, receipt, independent read-back, and rollback source.
- Keep `REWARD_ENGINE_ENABLED=false` and `LIFECYCLE_SHADOW_ONLY=true` until the real-data acceptance gates are signed off.

## Access

- Use individual GitHub, SSH, and BANDEIRA admin identities.
- Apply least privilege, revoke inactive sessions, and remove access promptly when responsibilities change.
