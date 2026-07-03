# Deployment Guide

## Overview

The file `.github/workflows/deploy.yml` is a **GitHub Actions** workflow that automatically deploys the application to a VPS server every time code is pushed to the `main` branch.

### What it does — step by step

| Step | Action |
|------|--------|
| 1. Pull latest code | SSHs into the server and runs `git reset --hard origin/main` to sync the latest code |
| 2. Install dependencies | Runs `uv sync` on the server to install/update Python packages |
| 3. Run migrations | Runs `alembic upgrade head` to apply any pending database migrations |
| 4. Restart service | Runs `sudo systemctl restart platform-api` to reload the app with the new code |
| 5. Health check | Waits 3 seconds, then verifies the systemd service is active — if not, prints the last 50 log lines and fails the pipeline |

---

## Required GitHub Secrets

The workflow uses three secrets. You must configure all three or the deployment will fail.

| Secret name | Description |
|-------------|-------------|
| `SERVER_IP` | Public IP address of your VPS |
| `SERVER_USER` | Linux username used to SSH into the server (e.g. `ubuntu`) |
| `SSH_PRIVATE_KEY` | Private SSH key that has access to the server |

### Where to configure them

1. Go to your GitHub repository
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add each secret with the exact name shown in the table above

---

## How to get each value

### `SERVER_IP`

The public IP of your VPS. Find it in your cloud provider dashboard:

- **AWS EC2:** EC2 console → your instance → *Public IPv4 address*
- **DigitalOcean:** Droplets → your droplet → displayed at the top
- **Hetzner:** Cloud console → your server → *IP address*
- **Any provider:** SSH into the server and run `curl ifconfig.me`

---

### `SERVER_USER`

The username you use to SSH into the server. Common defaults:

| Provider | Default user |
|----------|-------------|
| AWS EC2 (Ubuntu AMI) | `ubuntu` |
| DigitalOcean | `root` (or the user you created) |
| Hetzner | `root` (or custom user) |

To check your current user on the server:
```bash
whoami
```

---

### `SSH_PRIVATE_KEY`

Generate a dedicated key pair on your local machine specifically for this project:

**Step 1 — Generate the key pair (run on your local machine)**

```bash
ssh-keygen -t ed25519 -f ~/.ssh/platform_api_deploy -C "platform-api-deploy"
```

This creates two files:

| File | Purpose |
|------|---------|
| `~/.ssh/platform_api_deploy` | **Private key** → paste into GitHub Secret |
| `~/.ssh/platform_api_deploy.pub` | **Public key** → add to the server |

**Step 2 — Add the public key to the server**

The public key must be appended to `~/.ssh/authorized_keys` on the server. Run from your local machine:

```bash
ssh-copy-id -i ~/.ssh/platform_api_deploy.pub ubuntu@<SERVER_IP>
```

Or manually if `ssh-copy-id` is not available:

```bash
cat ~/.ssh/platform_api_deploy.pub | ssh ubuntu@<SERVER_IP> "cat >> ~/.ssh/authorized_keys"
```

To verify it was added correctly, SSH into the server and run:

```bash
cat ~/.ssh/authorized_keys
# You should see a line ending with: platform-api-deploy
```

**Step 3 — Add the private key to GitHub**

```bash
cat ~/.ssh/platform_api_deploy
```

Copy the entire output (including `-----BEGIN OPENSSH PRIVATE KEY-----` and `-----END OPENSSH PRIVATE KEY-----`) and paste it as the `SSH_PRIVATE_KEY` secret value in GitHub.

---

## Prerequisites on the server

For the workflow to succeed, the following must be set up on the VPS before the first deploy:

1. **Repository cloned** at `~/open_projects/platform-api`
   ```bash
   mkdir -p ~/open_projects
   cd ~/open_projects
   git clone <your-repo-url> platform-api
   ```

2. **`uv` installed** at `~/.local/bin/uv`
   ```bash
   curl -LsSf https://astral.sh/uv/install.sh | sh
   ```

3. **Environment file** present at `credentials/layout_example.env` (not tracked by git)

4. **systemd service** configured as `platform-api`
   - The workflow restarts this service via `sudo systemctl restart platform-api`
   - The server user must have passwordless sudo for this command, or be in the sudoers file

5. **PostgreSQL migrations** — the workflow runs `alembic upgrade head` automatically on every deploy

---

## Trigger

The workflow runs automatically on every push to `main`. To deploy, simply merge or push to `main`:

```bash
git push origin main
```

Monitor the result under **Actions** tab in your GitHub repository.
