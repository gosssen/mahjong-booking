# 部署指南

## 準備工具

```bash
# 安裝 Vercel CLI
npm i -g vercel
```

---

## 1. Supabase（PostgreSQL）

1. 到 https://supabase.com 建立帳號 + 新 Project
2. **Settings → Database → Connection string** 選 **URI** 格式複製備用
   - 格式：`postgresql://postgres:[密碼]@db.xxx.supabase.co:5432/postgres`
   - 轉成 JDBC 格式：`jdbc:postgresql://db.xxx.supabase.co:5432/postgres`
3. **SQL Editor** → 貼入 `backend/src/main/resources/schema.sql` → Run

---

## 2. Koyeb（後端）

> Koyeb 免費方案：1 nano instance，不休眠，自動 HTTPS，不需信用卡

### 2-1 建立帳號

前往 https://app.koyeb.com 免費註冊（GitHub 登入即可）

### 2-2 建立 App

1. **Create App** → **Deploy from GitHub**
2. 選擇 repository：`gosssen/mahjong-booking`
3. **Service name**：`mahjong-backend`
4. **Build & deployment**：
   - Builder：**Dockerfile**
   - Dockerfile location：`backend/Dockerfile`（預設從 repo 根目錄算）
   - Branch：`main`
5. **Exposed ports**：`8080`（HTTP）
6. **Health check**：Path = `/health`，Port = `8080`

### 2-3 設定環境變數

在 **Environment variables** 區塊加入以下變數：

| 變數 | 值 |
|------|----|
| `LINE_CHANNEL_TOKEN` | LINE Messaging API Channel Access Token |
| `LINE_CHANNEL_SECRET` | LINE Messaging API Channel Secret |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://db.xxx.supabase.co:5432/postgres` |
| `SPRING_DATASOURCE_USERNAME` | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Supabase 資料庫密碼 |
| `INITIAL_ADMIN_USER_ID` | 你的 LINE userId（`Uxxxxxxxxxx`） |
| `RICH_MENU_ID` | LINE Rich Menu ID |
| `CORS_ALLOWED_ORIGINS` | Vercel 前端網址（Step 3 完成後回填） |

### 2-4 部署

點 **Deploy** → 等待 build（約 3-5 分鐘）

部署完成後取得後端網址，格式：
`https://mahjong-backend-<hash>-<org>.koyeb.app`

### 2-5 查看 Logs

Koyeb Dashboard → App → Runtime logs

---

## 3. LINE Webhook 設定

1. [LINE Developers Console](https://developers.line.biz) → Messaging API → **Webhook settings**
2. Webhook URL：`https://mahjong-backend-xxx.koyeb.app/callback`
3. 開啟 **Use webhook** → **Verify** 確認回傳 200

---

## 4. Vercel（前端 LIFF）

```bash
cd frontend

# 部署
vercel --prod
```

取得 Vercel URL（例如 `https://mahjong-booking-xxx.vercel.app`）

**設定環境變數**（Vercel Dashboard → Settings → Environment Variables）：

| 變數 | 值 |
|------|----|
| `VITE_API_BASE_URL` | `https://mahjong-backend-xxx.koyeb.app` |
| `VITE_LIFF_ID` | `2009787261-yiDOnOFw` |

設定完後重新部署：`vercel --prod`

---

## 5. 回填 CORS 設定

Vercel URL 確定後，回到 Koyeb → Environment variables → 更新：

```
CORS_ALLOWED_ORIGINS=https://mahjong-booking-xxx.vercel.app
```

儲存後 Koyeb 會自動重新部署。

---

## 6. LIFF Endpoint 設定

1. [LINE Developers Console](https://developers.line.biz) → LINE Login Channel → **LIFF**
2. 找到 LIFF ID `2009787261-yiDOnOFw`
3. **Endpoint URL** 設為：Vercel 網址

---

## 7. 本地開發測試

```bash
# 後端
cd backend
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
# 填入本地 DB 設定
mvn spring-boot:run -Dspring.profiles.active=local

# 前端（另開終端機）
cd frontend
echo "VITE_API_BASE_URL=http://localhost:8080" > .env.local
echo "VITE_LIFF_ID=2009787261-yiDOnOFw" >> .env.local
npm install
npm run dev

# 若需測試 LINE Webhook（另開終端機）
ngrok http 8080
# 將 ngrok URL 設為 LINE Webhook URL（暫時）
```

---

## 環境變數一覽

### 後端（Koyeb Environment Variables）

| 變數 | 說明 |
|------|------|
| `LINE_CHANNEL_TOKEN` | LINE Channel Access Token |
| `LINE_CHANNEL_SECRET` | LINE Channel Secret |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL（`jdbc:postgresql://...`） |
| `SPRING_DATASOURCE_USERNAME` | DB 帳號 |
| `SPRING_DATASOURCE_PASSWORD` | DB 密碼 |
| `INITIAL_ADMIN_USER_ID` | 初始管理員 LINE userId |
| `RICH_MENU_ID` | LINE Rich Menu ID |
| `CORS_ALLOWED_ORIGINS` | 前端 Vercel 網址 |

### 前端（Vercel Environment Variables）

| 變數 | 說明 |
|------|------|
| `VITE_API_BASE_URL` | 後端 Koyeb 網址 |
| `VITE_LIFF_ID` | LINE LIFF ID |
