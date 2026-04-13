#!/bin/bash
# ============================================================
# GCP e2-micro VM 初始化腳本
# 在 VM 上執行一次即可
# ============================================================
set -e

echo "==> 更新套件清單..."
sudo apt-get update -q

echo "==> 安裝 Java 17、Nginx、Certbot..."
sudo apt-get install -y -q \
  openjdk-17-jre-headless \
  nginx \
  certbot \
  python3-certbot-nginx

echo "==> 建立應用程式目錄..."
sudo mkdir -p /opt/mahjong
sudo chown "$USER":"$USER" /opt/mahjong

echo "==> 建立環境變數設定檔（請稍後填入實際值）..."
cat > /opt/mahjong/.env << 'EOF'
LINE_CHANNEL_TOKEN=
LINE_CHANNEL_SECRET=
SPRING_DATASOURCE_URL=jdbc:postgresql://db.xxx.supabase.co:5432/postgres
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=
INITIAL_ADMIN_USER_ID=
RICH_MENU_ID=
CORS_ALLOWED_ORIGINS=
EOF
chmod 600 /opt/mahjong/.env

echo "==> 安裝 systemd 服務..."
sudo tee /etc/systemd/system/mahjong-backend.service > /dev/null << EOF
[Unit]
Description=Mahjong Booking Backend
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=/opt/mahjong
EnvironmentFile=/opt/mahjong/.env
ExecStart=/usr/bin/java -Xmx384m -XX:+UseContainerSupport -jar /opt/mahjong/app.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable mahjong-backend

echo ""
echo "✅ 初始化完成！接下來："
echo "   1. 編輯環境變數：nano /opt/mahjong/.env"
echo "   2. 在本機執行 deploy.sh 上傳 JAR"
echo "   3. 設定 DuckDNS，再執行 certbot 取得 SSL 憑證"
