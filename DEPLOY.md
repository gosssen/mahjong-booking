# 部署指南

## 準備工具

```bash
# 安裝 Fly.io CLI
brew install flyctl

# 安裝 Vercel CLI
npm i -g vercel
```

---

## 1. Supabase（PostgreSQL）

1. 到 https://supabase.com 建立帳號 + 新 Project
2. Settings → Database → Connection string（URI 格式）複製備用
   - 格式：`postgresql://postgres:[密碼]@db.xxx.supabase.co:5432/postgres`
3. 執行 schema：SQL Editor → 貼入 `backend/src/main/resources/schema.sql` → Run

---

## 2. Fly.io（後端）

```bash
cd backend

# 登入
flyctl auth login

# 建立 app（第一次）
flyctl launch --no-deploy --name mahjong-booking --region nrt

# 設定環境變數
flyctl secrets set \
  LINE_CHANNEL_TOKEN="<LINE Messaging API Channel Access Token>" \
  LINE_CHANNEL_SECRET="<LINE Messaging API Channel Secret>" \
  SPRING_DATASOURCE_URL="jdbc:postgresql://db.xxx.supabase.co:5432/postgres" \
  SPRING_DATASOURCE_USERNAME="postgres" \
  SPRING_DATASOURCE_PASSWORD="<Supabase 密碼>" \
  INITIAL_ADMIN_USER_ID="<你的 LINE userId (Uxxxxxxxxx)>" \
  RICH_MENU_ID="<Rich Menu ID>" \
  CORS_ALLOWED_ORIGINS="<Vercel 前端網址>"

# 打包並部署
mvn clean package -DskipTests
flyctl deploy

# 查看 logs
flyctl logs
```

後端網址：`https://mahjong-booking.fly.dev`

---

## 3. LINE Webhook 設定

1. LINE Developers Console → Messaging API → Webhook settings
2. Webhook URL：`https://mahjong-booking.fly.dev/callback`
3. 開啟 Use webhook

---

## 4. Vercel（前端 LIFF）

```bash
cd frontend

# 設定環境變數（建立 .env.production）
echo "VITE_API_BASE_URL=https://mahjong-booking.fly.dev" > .env.production

# 部署
vercel --prod
```

取得 Vercel URL（例如 `https://mahjong-booking.vercel.app`）

---

## 5. LIFF Endpoint 設定

1. LINE Developers Console → LINE Login → LIFF
2. 找到 LIFF ID `2009787261-yiDOnOFw`
3. Endpoint URL 設為：`https://mahjong-booking.vercel.app`

---

## 6. 本地開發測試

```bash
# 後端（需要 PostgreSQL）
cd backend
mvn spring-boot:run

# 前端（另開終端機）
cd frontend
npm run dev

# ngrok 暴露本地 webhook（另開終端機）
ngrok http 8080
# 將 ngrok URL 設為 LINE Webhook URL（暫時）
```

---

## 環境變數一覽

| 變數 | 說明 |
|------|------|
| `LINE_CHANNEL_TOKEN` | LINE Channel Access Token |
| `LINE_CHANNEL_SECRET` | LINE Channel Secret |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | DB 帳號 |
| `SPRING_DATASOURCE_PASSWORD` | DB 密碼 |
| `APP_INITIAL_ADMIN_USER_ID` | 初始管理員 LINE userId |
| `VITE_API_BASE_URL` | 前端呼叫後端的 base URL |
