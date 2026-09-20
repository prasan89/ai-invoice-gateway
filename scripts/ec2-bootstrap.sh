#!/bin/bash
# EC2 bootstrap — run once as root on a fresh Ubuntu 24.04 instance
set -e

# Docker
apt-get update -y
apt-get install -y ca-certificates curl unzip
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
  https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  > /etc/apt/sources.list.d/docker.list
apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# AWS CLI v2
curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o /tmp/awscliv2.zip
unzip -q /tmp/awscliv2.zip -d /tmp
/tmp/aws/install
rm -rf /tmp/aws /tmp/awscliv2.zip

# App directory
mkdir -p /opt/ai-invoice-gateway
cd /opt/ai-invoice-gateway

# Systemd service so the app restarts on reboot
cat > /etc/systemd/system/ai-invoice.service <<'SERVICE'
[Unit]
Description=AI Invoice Gateway
Requires=docker.service
After=docker.service network-online.target

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/opt/ai-invoice-gateway
EnvironmentFile=/opt/ai-invoice-gateway/.env
ExecStart=/usr/bin/docker compose -f docker-compose.prod.yml up -d --remove-orphans
ExecStop=/usr/bin/docker compose -f docker-compose.prod.yml down
TimeoutStartSec=120

[Install]
WantedBy=multi-user.target
SERVICE

systemctl daemon-reload
systemctl enable ai-invoice.service

echo "Bootstrap complete. Copy docker-compose.prod.yml and .env to /opt/ai-invoice-gateway/ then start with: systemctl start ai-invoice"
