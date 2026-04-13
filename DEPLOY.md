# 部署指南

## 架構

```
LINE App ──webhook──▶ Render.com (Spring Boot, 免費)
                              │
                              ▼
                      Supabase PostgreSQL（免費）
LIFF (Vercel) ───API──▶ Render.com
UptimeRobot ─────ping every 5min──▶ Render（防止休眠）
```

---

## 1. Supabase（PostgreSQL）

1. 前往 https://supabase.com，建立帳號 + 新 Project
2. **Settings → Database → Connection string** 選 **URI** 格式複製：
   - 格式：`postgresql://postgres:[密碼]@db.xxx.supabase.co:5432/postgres`
3. **SQL Editor** → 貼入 `backend/src/main/resources/schema.sql` → **Run**

---

## 2. Render.com（後端，免費，不需信用卡）

### 2-1 建立帳號

前往 https://render.com → **Get Started for Free** → 用 GitHub 登入

### 2-2 建立 Web Service（Blueprint 方式）

1. Dashboard → **New** → **Blueprint**
2. 連接 GitHub → 選擇 `gosssen/mahjong-booking`
3. Render 會自動讀取 `render.yaml`，顯示 `mahjong-backend` 服務
4. 點 **Apply**

### 2-3 設定環境變數

Blueprint 建立後，到 **Dashboard → mahjong-backend → Environment** 填入：

| 變數 | 值 |
|------|----|
| `LINE_CHANNEL_TOKEN` | LINE Channel Access Token |
| `LINE_CHANNEL_SECRET` | LINE Channel Secret |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://db.xxx.supabase.co:5432/postgres` |
| `SPRING_DATASOURCE_USERNAME` | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Supabase 密碼 |
| `INITIAL_ADMIN_USER_ID` | 你的 LINE userId（`Uxxxxxxxxxx`） |
| `RICH_MENU_ID` | LINE Rich Menu ID |
| `CORS_ALLOWED_ORIGINS` | Vercel 前端網址（Step 4 完成後填入） |

填完點 **Save Changes** → Render 自動重新部署

### 2-4 取得後端網址

部署完成後（約 5-10 分鐘），網址格式為：
```
https://mahjong-backend.onrender.com
```

（Render 預設用服務名稱，若有衝突會加隨機字元）

---

## 3. UptimeRobot（防止 Render 休眠，免費）

Render 免費方案 15 分鐘無流量會休眠，用 UptimeRobot 每 5 分鐘 ping 一次解決。

1. 前往 https://uptimerobot.com → **Register for FREE**（不需信用卡）
2. Dashboard → **Add New Monitor**
3. 設定：
   - Monitor Type：**HTTP(S)**
   - Friendly Name：`Mahjong Backend`
   - URL：`https://mahjong-backend.onrender.com/health`
   - Monitoring Interval：**5 minutes**
4. **Create Monitor**

設定完成後服務永不休眠。

---

## 4. LINE Webhook 設定

1. [LINE Developers Console](https://developers.line.biz) → Messaging API → **Webhook settings**
2. Webhook URL：`https://mahjong-backend.onrender.com/callback`
3. 開啟 **Use webhook** → **Verify**（應回傳 200）

---

## 5. Vercel（前端 LIFF）

```bash
cd frontend
vercel --prod
```

取得 Vercel URL 後，到 **Vercel Dashboard → Settings → Environment Variables** 新增：

| 變數 | 值 |
|------|----|
| `VITE_API_BASE_URL` | `https://mahjong-backend.onrender.com` |
| `VITE_LIFF_ID` | `2009787261-yiDOnOFw` |

設定完重新部署：`vercel --prod`

---

## 6. 回填 CORS 設定

Vercel URL 確定後，回到 **Render → mahjong-backend → Environment** 更新：

```
CORS_ALLOWED_ORIGINS=https://你的網址.vercel.app
```

Save Changes → 自動重新部署。

---

## 7. LIFF Endpoint 設定

1. [LINE Developers Console](https://developers.line.biz) → LINE Login Channel → **LIFF**
2. 找到 LIFF ID `2009787261-yiDOnOFw`
3. **Endpoint URL** 設為 Vercel 網址

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
# 將 ngrok URL 設為 LINE Webhook URL（暫時）
```

---

## 日常維護

```bash
# 查看 logs
# Render Dashboard → mahjong-backend → Logs

# 部署新版本（push 到 GitHub main 即自動觸發）
git push
```

---

## 環境變數一覽

### 後端（Render Environment Variables）

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
| `VITE_API_BASE_URL` | `https://mahjong-backend.onrender.com` |
| `VITE_LIFF_ID` | LINE LIFF ID |
