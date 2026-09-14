# Development accounts

These intentionally public demonstration credentials are created only with `sentinel.seed=true`, `DEMO_SEED=true`, or the `dev` profile. Every password is stored as a BCrypt hash; the raw value is never returned from the account API.

| Role | Email | Development password |
|---|---|---|
| ADMIN | admin@sentinelx.local | SentinelX-Local-2026! |
| OPERATOR | operator@sentinelx.local | SentinelX-Local-2026! |
| DEVELOPER | developer@sentinelx.local | SentinelX-Local-2026! |
| USER | user@sentinelx.local | SentinelX-Local-2026! |

Seeding does not overwrite existing accounts or reset changed passwords. If you change a demo password, use that password thereafter or intentionally reset the local demo database. The load and chaos scripts accept `DEMO_PASSWORD` if you changed the relevant demo account password.

## Password recovery in local development

1. Select Forgot password, enter an active account's email, and submit.
2. The response is the same for known and unknown accounts.
3. Open `backend/data/reset-mailbox/<user-id>.txt` locally. This private development mailbox is deliberately not an HTTP endpoint.
4. Open the reset link in that file. Establish and confirm a password of at least 12 characters and at most 72 UTF-8 bytes.
5. The link expires after 15 minutes and cannot be reused. Resetting revokes every session and invalidates other reset tokens.

The database stores only SHA-256 reset-token hashes. The local file represents email delivery and necessarily contains a raw link; protect the directory, do not publish it, and remove old mailbox files when no longer needed. It is ignored by Git and Docker. Container mailbox location: `/app/data/reset-mailbox`; inspect it with `docker compose exec gateway cat /app/data/reset-mailbox/<user-id>.txt`.

Never enable the demo seed or local mailbox in an Internet-facing deployment. Production reset delivery needs an actual email provider or a controlled account-recovery adapter; that integration is outside this local MVP.
