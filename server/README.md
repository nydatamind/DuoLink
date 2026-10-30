# DuoLink Signaling Server & Network Architecture

This directory contains the self-hosted, lightweight WebSocket signaling service for DuoLink.

## Architecture Overview

```
[Android Phone A]                             [Android Phone B]
       |                                             |
       +--------- Secure WebSocket (WSS) ------------+
                          |
             [DuoLink Signaling Server]
             (Ephemeral Session Router)
                          |
             [STUN / COTURN TURN Relay]
              (For NAT traversal fallback)
                          |
[Android Phone A] <==== WebRTC Direct P2P ====> [Android Phone B]
                  (DTLS-SRTP Audio/Video & 
                   AES-256-GCM DataChannel)
```

## Security & Privacy Guarantees
1. **Zero Storage**: Messages, files, and voice recordings are NEVER stored or queued on the server.
2. **End-to-End Encryption**: All message envelopes are encrypted on the sending device using AES-256-GCM with keys negotiated via ECDH P-256. The signaling server sees only opaque ciphertext.
3. **No Third-Party Cloud**: You own and run the server on your personal VPS, Raspberry Pi, home server, or local Wi-Fi router.

---

## Quick Start (Local or VPS)

### Option 1: Run with Node.js
Requirements: Node.js 18+

```bash
# 1. Install dependencies
npm install

# 2. Start server
npm start
# Output:
# DuoLink Signaling Server is listening on port 8080
# WebSocket URL: ws://0.0.0.0:8080/ws
# Health check:  http://0.0.0.0:8080/health
```

### Option 2: Run with Docker Compose
```bash
docker compose up -d
```
This launches both the DuoLink signaling container on port 8080 and an integrated coturn TURN server on port 3478.

---

## Production Deployment with Nginx & Let's Encrypt (WSS)

For production deployment across mobile data and disparate Wi-Fi networks:

1. Point your domain (e.g. `signal.yourdomain.com`) to your VPS IP.
2. Install Nginx and Certbot:
```bash
sudo apt update && sudo apt install -y nginx certbot python3-certbot-nginx
sudo certbot --nginx -d signal.yourdomain.com
```

3. Configure `/etc/nginx/sites-available/duolink`:
```nginx
server {
    server_name signal.yourdomain.com;

    location /ws {
        proxy_pass http://127.0.0.1:8080/ws;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "Upgrade";
        proxy_set_header Host $host;
        proxy_read_timeout 86400s;
        proxy_send_timeout 86400s;
    }

    location /health {
        proxy_pass http://127.0.0.1:8080/health;
    }

    listen 443 ssl;
    # Managed by Certbot...
}
```
4. Reload Nginx: `sudo systemctl reload nginx`
5. On the DuoLink Android app, set the Signaling Server URL to:
   `wss://signal.yourdomain.com/ws`

