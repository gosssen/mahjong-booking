# 部署指南

## 架構

```
LINE App ──webhook──▶ GCP e2-micro (nginx + Spring Boot)
                              │
                              ▼
                      Supabase PostgreSQL
LIFF (Vercel) ───API──▶ GCP e2-micro
```

---

## 1. Supabase（PostgreSQL）

1. 到 https://supabase.com 建立帳號 + 新 Project（選離台灣最近的區域，例如 ap-southeast-1）
2. **Settings → Database → Connection string** 選 **URI** 格式複製
   - 格式：`postgresql://postgres:[密碼]@db.xxx.supabase.co:5432/postgres`
3. **SQL Editor** → 貼入 `backend/src/main/resources/schema.sql` → Run

---

## 2. GCP e2-micro（後端，永久免費）

> GCP Always Free：us-central1 / us-west1 / us-east1 的 e2-micro 每月永久免費。
> 需要信用卡驗證身份，但符合免費條件不會扣款。

### 2-1 建立 GCP 帳號與專案

1. 前往 https://console.cloud.google.com 登入 Google 帳號
2. **建立專案**：點右上角專案選單 → New Project → 輸入 `mahjong-booking`
3. 左側選單 → **Compute Engine** → Enable API（第一次需要）

### 2-2 建立 VM

1. Compute Engine → **VM instances** → **Create Instance**
2. 設定如下：

   | 欄位 | 值 |
   |------|----|
   | Name | `mahjong-backend` |
   | Region | `us-central1`（**必須選這三個之一才免費**） |
   | Zone | `us-central1-a` |
   | Machine type | **e2-micro**（必須選這個才免費） |
   | Boot disk OS | Debian GNU/Linux 12 (Bookworm) |
   | Boot disk size | 30 GB Standard persistent disk |
   | Firewall | ✅ Allow HTTP traffic、✅ Allow HTTPS traffic |

3. **Create** → 等待 VM 建立完成

### 2-3 設定靜態外部 IP

1. 左側選單 → **VPC network → IP addresses**
2. 找到剛建立 VM 的 External IP，點 **Reserve static address**
3. 名稱：`mahjong-ip`，Network Service Tier：**Standard**（免費）→ Reserve
4. 記下這個固定 IP（後面會用到）

### 2-4 開放防火牆 8080 Port（僅測試用，之後 nginx 上線可移除）

1. VPC network → **Firewall** → **Create Firewall Rule**
2. Name: `allow-8080`、Targets: All instances、Source IP: `0.0.0.0/0`
   Protocols and ports: TCP `8080`

### 2-5 SSH 進入 VM

```bash
# 安裝 gcloud CLI（macOS）
brew install google-cloud-sdk
gcloud auth login
gcloud config set project mahjong-booking

# SSH 進入 VM
gcloud compute ssh mahjong-backend --zone=us-central1-a
```

### 2-6 VM 初始化

在 **VM 裡**執行：

```bash
# 上傳並執行初始化腳本
# 先在本機執行：
gcloud compute scp gcp/setup.sh mahjong-backend:/tmp/ --zone=us-central1-a
gcloud compute ssh mahjong-backend --zone=us-central1-a -- bash /tmp/setup.sh
```

### 2-7 填入環境變數

SSH 進 VM 後編輯設定：

```bash
nano /opt/mahjong/.env
```

填入以下內容：

```env
LINE_CHANNEL_TOKEN=你的LINE_Channel_Access_Token
LINE_CHANNEL_SECRET=你的LINE_Channel_Secret
SPRING_DATASOURCE_URL=jdbc:postgresql://db.xxx.supabase.co:5432/postgres
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=你的Supabase密碼
INITIAL_ADMIN_USER_ID=你的LINE_userId
RICH_MENU_ID=你的RichMenu_ID
CORS_ALLOWED_ORIGINS=https://你的vercel網址.vercel.app
```

### 2-8 本機建置並部署 JAR

在**本機**執行：

```bash
# 設定 VM IP（換成你的靜態 IP）
export GCP_VM_IP=1.2.3.4
export GCP_USER=$(gcloud config get-value account | cut -d@ -f1)

chmod +x gcp/deploy.sh
./gcp/deploy.sh
```

---

## 3. DuckDNS 免費網域（HTTPS 必需）

LINE Webhook 要求 HTTPS，需要一個 domain 來申請 Let's Encrypt 憑證。

1. 前往 https://www.duckdns.org，用 Google 帳號登入
2. 建立 subdomain，例如：`mahjong-booking`
   → 你會得到 `mahjong-booking.duckdns.org`
3. 將 IP 填入你的 GCP 靜態 IP → **Update IP**

---

## 4. Nginx + Let's Encrypt SSL

SSH 進 VM 後執行：

```bash
# 安裝 nginx 設定檔
sudo cp /tmp/nginx.conf /etc/nginx/sites-available/mahjong-backend

# 修改 domain（換成你的 duckdns domain）
sudo sed -i 's/YOUR_DOMAIN.duckdns.org/mahjong-booking.duckdns.org/' \
  /etc/nginx/sites-available/mahjong-backend

# 啟用設定
sudo ln -sf /etc/nginx/sites-available/mahjong-backend \
            /etc/nginx/sites-enabled/mahjong-backend
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx

# 取得 SSL 憑證（自動設定 nginx）
sudo certbot --nginx -d mahjong-booking.duckdns.org \
  --non-interactive --agree-tos -m your@email.com
```

先在本機上傳 nginx 設定：

```bash
gcloud compute scp gcp/nginx.conf mahjong-backend:/tmp/ --zone=us-central1-a
```

SSL 憑證每 90 天自動更新（certbot 會設定 cron）。

---

## 5. LINE Webhook 設定

後端網址：`https://mahjong-booking.duckdns.org`

1. [LINE Developers Console](https://developers.line.biz) → Messaging API → **Webhook settings**
2. Webhook URL：`https://mahjong-booking.duckdns.org/callback`
3. 開啟 **Use webhook** → **Verify** 確認回傳 200

---

## 6. Vercel（前端 LIFF）

```bash
cd frontend
vercel --prod
```

取得 Vercel URL 後，到 **Vercel Dashboard → Settings → Environment Variables** 設定：

| 變數 | 值 |
|------|----|
| `VITE_API_BASE_URL` | `https://mahjong-booking.duckdns.org` |
| `VITE_LIFF_ID` | `2009787261-yiDOnOFw` |

設定完重新部署：`vercel --prod`

---

## 7. 回填 CORS 設定

Vercel URL 確定後，更新 VM 上的 `.env`：

```bash
# SSH 進 VM
nano /opt/mahjong/.env
# 更新 CORS_ALLOWED_ORIGINS=https://你的網址.vercel.app
sudo systemctl restart mahjong-backend
```

---

## 8. LIFF Endpoint 設定

1. [LINE Developers Console](https://developers.line.biz) → LINE Login Channel → **LIFF**
2. 找到 LIFF ID `2009787261-yiDOnOFw`
3. **Endpoint URL** 設為：Vercel 網址

---

## 日常維護指令

```bash
# 查看服務狀態
sudo systemctl status mahjong-backend

# 查看即時 logs
sudo journalctl -u mahjong-backend -f

# 重啟服務
sudo systemctl restart mahjong-backend

# 部署新版本（在本機執行）
./gcp/deploy.sh
```

---

## 環境變數一覽

### 後端（/opt/mahjong/.env）

| 變數 | 說明 |
|------|------|
| `LINE_CHANNEL_TOKEN` | LINE Channel Access Token |
| `LINE_CHANNEL_SECRET` | LINE Channel Secret |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://db.xxx.supabase.co:5432/postgres` |
| `SPRING_DATASOURCE_USERNAME` | DB 帳號（通常是 `postgres`） |
| `SPRING_DATASOURCE_PASSWORD` | DB 密碼 |
| `INITIAL_ADMIN_USER_ID` | 初始管理員 LINE userId |
| `RICH_MENU_ID` | LINE Rich Menu ID |
| `CORS_ALLOWED_ORIGINS` | 前端 Vercel 網址 |

### 前端（Vercel Environment Variables）

| 變數 | 說明 |
|------|------|
| `VITE_API_BASE_URL` | `https://mahjong-booking.duckdns.org` |
| `VITE_LIFF_ID` | LINE LIFF ID |

---

## 本地開發

```bash
# 後端
cd backend
mvn spring-boot:run -Dspring.profiles.active=local

# 前端（另開終端機）
cd frontend
npm run dev

# 若需測試 LINE Webhook
ngrok http 8080
```
