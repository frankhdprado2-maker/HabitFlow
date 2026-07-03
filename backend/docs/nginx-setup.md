# Nginx + HTTPS Setup Guide

Deploy the API at `https://platform-api.kankunapaq.com/` using Nginx as a reverse proxy in front of uvicorn running on port `9000`.

---

## Architecture

```
Internet
   │
   ▼
Nginx (ports 80 / 443)          ← handles SSL, domain routing
   │
   ▼
uvicorn on 127.0.0.1:9000       ← runs the FastAPI app
```

Nginx terminates SSL and proxies all requests to the local uvicorn process. The app never exposes port 9000 to the internet directly.

---

## Prerequisites

- Ubuntu VPS with a public IP
- Domain `platform-api.kankunapaq.com` DNS A record pointing to the server IP
- The app running as a systemd service (see [deployment.md](deployment.md))

---

## Step 1 — Verify DNS

Before setting up SSL, confirm the domain resolves to your server:

```bash
dig +short platform-api.kankunapaq.com
# Should return your server's public IP
```

Or from your local machine:

```bash
nslookup platform-api.kankunapaq.com
```

> DNS changes can take up to 24 hours to propagate. Do not proceed to SSL until the domain resolves correctly.

---

## Step 2 — Install Nginx

```bash
sudo apt update
sudo apt install nginx -y
```

Verify it is running:

```bash
sudo systemctl status nginx
```

---

## Step 3 — Configure the systemd service

The app must listen only on `127.0.0.1:9000` (not `0.0.0.0`) so it is not exposed directly to the internet.

Create the service file:

```bash
sudo nano /etc/systemd/system/platform-api.service
```

Paste the following (adjust `User` and paths if needed):

```ini
[Unit]
Description=Platform API
After=network.target

[Service]
User=ubuntu
WorkingDirectory=/home/ubuntu/open_projects/platform-api
ExecStart=/home/ubuntu/.local/bin/uv run uvicorn app.main:app --host 127.0.0.1 --port 9000
Restart=always
RestartSec=5
Environment=PYTHONUNBUFFERED=1

[Install]
WantedBy=multi-user.target
```

Enable and start the service:

```bash
sudo systemctl daemon-reload
sudo systemctl enable platform-api
sudo systemctl start platform-api
```

Verify the app is listening on port 9000:

```bash
sudo ss -tlnp | grep 9000
# Should show: 127.0.0.1:9000
```

---

## Step 4 — Configure Nginx

Create a new site config:

```bash
sudo nano /etc/nginx/sites-available/platform-api
```

Paste:

```nginx
server {
    listen 80;
    server_name platform-api.kankunapaq.com;

    location / {
        proxy_pass         http://127.0.0.1:9000;
        proxy_http_version 1.1;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_set_header   Upgrade           $http_upgrade;
        proxy_set_header   Connection        "upgrade";
    }
}
```

Enable the site and test the config:

```bash
sudo ln -s /etc/nginx/sites-available/platform-api /etc/nginx/sites-enabled/
sudo nginx -t
```

Expected output:
```
nginx: the configuration file /etc/nginx/nginx.conf syntax is ok
nginx: configuration file /etc/nginx/nginx.conf test is successful
```

Reload Nginx:

```bash
sudo systemctl reload nginx
```

Test HTTP before adding SSL:

```bash
curl http://platform-api.kankunapaq.com/
# Should return the landing page HTML
```

---

## Step 5 — Install SSL with Certbot

```bash
sudo apt install certbot python3-certbot-nginx -y
```

Request a certificate for the domain:

```bash
sudo certbot --nginx -d platform-api.kankunapaq.com
```

Certbot will:
1. Verify domain ownership via HTTP challenge
2. Issue a Let's Encrypt certificate
3. Automatically update the Nginx config to add HTTPS

When prompted, enter your email and agree to the terms. Choose option **2** (redirect HTTP to HTTPS) when asked.

Verify the certificate was issued:

```bash
sudo certbot certificates
```

---

## Step 6 — Verify the final Nginx config

After Certbot runs, the config at `/etc/nginx/sites-available/platform-api` should look like this:

```nginx
server {
    listen 80;
    server_name platform-api.kankunapaq.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name platform-api.kankunapaq.com;

    ssl_certificate     /etc/letsencrypt/live/platform-api.kankunapaq.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/platform-api.kankunapaq.com/privkey.pem;
    include             /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam         /etc/letsencrypt/ssl-dhparams.pem;

    location / {
        proxy_pass         http://127.0.0.1:9000;
        proxy_http_version 1.1;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_set_header   Upgrade           $http_upgrade;
        proxy_set_header   Connection        "upgrade";
    }
}
```

If Certbot did not add the redirect automatically, edit the file manually to match the above.

Reload Nginx after any manual changes:

```bash
sudo systemctl reload nginx
```

---

## Step 7 — Final verification

```bash
# HTTPS works
curl https://platform-api.kankunapaq.com/

# HTTP redirects to HTTPS
curl -I http://platform-api.kankunapaq.com/
# Should return: HTTP/1.1 301 Moved Permanently

# App health check
curl https://platform-api.kankunapaq.com/layout_example/health
# Should return: {"project": "layout_example", "status": "ok"}
```

---

## Certificate renewal

Let's Encrypt certificates expire every 90 days. Certbot installs a cron job that renews automatically. Verify it works with a dry run:

```bash
sudo certbot renew --dry-run
```

---

## Firewall (UFW)

If UFW is enabled, allow HTTP and HTTPS traffic:

```bash
sudo ufw allow 'Nginx Full'
sudo ufw status
```

> Do **not** expose port 9000 through the firewall. It should only be accessible from localhost via Nginx.

---

## Useful commands

| Command | Description |
|---------|-------------|
| `sudo systemctl status platform-api` | Check if the app is running |
| `sudo systemctl restart platform-api` | Restart the app |
| `sudo journalctl -u platform-api -f` | Tail app logs |
| `sudo systemctl status nginx` | Check Nginx status |
| `sudo nginx -t` | Validate Nginx config syntax |
| `sudo systemctl reload nginx` | Apply Nginx config changes without downtime |
| `sudo certbot certificates` | List installed SSL certificates |
