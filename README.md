# 麻將預約系統

LINE Bot + LIFF 麻將場次預約系統，適合小圈子（約 20 人）使用。

## 功能

### 用戶
- Rich Menu 快速入口：預約場次、我的預約、查看下一場
- 月曆選擇場次與桌位
- 查看 / 取消自己的預約
- 開打前 1 小時自動提醒推播

### 管理員
- 建立 / 取消場次
- 追加 / 移除桌位
- 視覺化桌位配置（換桌 / 移桌）
- 封鎖日期
- 推播通知（每月 200 則上限）
- 管理員帳號管理

## 技術架構

| 層 | 技術 | 部署 | 管理介面 |
|----|------|------|----------|
| 後端 | Spring Boot 3.4 + MyBatis | [Koyeb](https://app.koyeb.com)（免費） | [Dashboard](https://app.koyeb.com) |
| 資料庫 | PostgreSQL 16 | [Supabase](https://supabase.com)（免費） | [Dashboard](https://supabase.com/dashboard) |
| 前端 | React 18 + TypeScript + Vite + Tailwind | [Vercel](https://vercel.com)（免費） | [Dashboard](https://vercel.com/dashboard) |
| Bot / LIFF | LINE Messaging API SDK v9 / @line/liff v2 | LINE | [Developers Console](https://developers.line.biz) |
| LINE OA 設定 | 回應設定、Rich Menu | — | [OA Manager](https://manager.line.biz) |

## 快速開始

### 1. 環境需求

- Java 17+
- Node.js 20+
- Maven 3.9+

### 2. 後端本地開發

```bash
cd backend

# 建立本地設定檔（不會 commit）
cat > src/main/resources/application-local.yml << 'EOF'
line:
  bot:
    channel-token: "<LINE Channel Access Token>"
    channel-secret: "<LINE Channel Secret>"
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mahjong_db
    username: mahjong
    password: mahjong123
EOF

mvn spring-boot:run -Dspring.profiles.active=local
```

### 3. 前端本地開發

```bash
cd frontend
npm install
echo "VITE_API_BASE_URL=http://localhost:8080" > .env.local
npm run dev
```

## 部署

詳見 [DEPLOY.md](DEPLOY.md)。

## 環境變數

### 後端（Koyeb Environment Variables）

| 變數 | 說明 |
|------|------|
| `LINE_CHANNEL_TOKEN` | LINE Messaging API Channel Access Token |
| `LINE_CHANNEL_SECRET` | LINE Messaging API Channel Secret |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC 連線字串（`jdbc:postgresql://...`） |
| `SPRING_DATASOURCE_USERNAME` | DB 帳號 |
| `SPRING_DATASOURCE_PASSWORD` | DB 密碼 |
| `INITIAL_ADMIN_USER_ID` | 第一個管理員的 LINE userId |
| `RICH_MENU_ID` | LINE Rich Menu ID |
| `CORS_ALLOWED_ORIGINS` | 前端網址（Vercel URL） |

### 前端（Vercel Environment Variables）

| 變數 | 說明 |
|------|------|
| `VITE_API_BASE_URL` | 後端 API 網址（`https://xxx.fly.dev`） |

## LINE 設定

1. **Messaging API Channel**（[LINE Developers Console](https://developers.line.biz)）
   - 建立 Provider → Messaging API Channel
   - 取得 Channel Access Token 與 Channel Secret
   - Webhook URL 設定為 `https://<your-app>.fly.dev/callback`
   - 關閉自動回覆：[LINE OA Manager](https://manager.line.biz) → 回應設定 → Bot 模式

2. **LINE Login Channel + LIFF**（[LINE Developers Console](https://developers.line.biz)）
   - 建立 LINE Login Channel
   - 新增 LIFF App，Endpoint URL 設為 Vercel 網址
   - 記下 LIFF ID（格式：`xxxxxxxxx-xxxxxxxx`）

3. **Rich Menu**
   - 設定環境變數後執行 `python3 setup-richmenu.py`
   - 或透過 [LINE OA Manager](https://manager.line.biz) → 圖文選單 手動設定

## 相關服務連結

| 服務 | 用途 | 連結 |
|------|------|------|
| Koyeb Dashboard | 後端部署、logs、環境變數管理 | https://app.koyeb.com |
| Supabase Dashboard | 資料庫管理、SQL Editor | https://supabase.com/dashboard |
| Vercel Dashboard | 前端部署、環境變數 | https://vercel.com/dashboard |
| LINE Developers Console | Bot 設定、LIFF、Webhook | https://developers.line.biz |
| LINE OA Manager | 回應設定、Rich Menu、推播 | https://manager.line.biz |

## 授權

MIT
