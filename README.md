# 麻將預約系統

LINE Bot + LIFF 麻將場次預約系統，適合小圈子（約 20 人）使用。

## 功能

### 用戶
- Rich Menu 快速入口：預約場次、我的預約、查看下一場
- 月曆選擇場次與桌位（點擊無場次日期可申請開場）
- 查看 / 取消自己的預約
- **場次申請**：向管理員申請在指定日期 / 時間開場，管理員核准後自動開放預約並推播通知
- 開打前 1 小時自動提醒推播

### 管理員
- 建立 / 取消場次（取消後同時段可重新建立）
- 追加 / 移除桌位
- 視覺化桌位配置（換桌 / 移桌）
- 封鎖日期
- 推播通知（每月 200 則上限）
- **管理員帳號管理**：從用戶名單直接設定 / 取消管理員身分
- **場次申請審核**：查看待審申請、核准（自動建立場次）/ 拒絕並推播通知申請者

### 開發人員帳號
- 透過環境變數 `DEVELOPER_USER_ID` 設定（LINE userId）
- 永久擁有所有管理員權限
- 無法被任何人移除或修改帳號身分
- 若同時設定在 DB admins 表中，`開發人員` 標籤會顯示在管理頁面

## 技術架構

| 層 | 技術 | 部署 | 管理介面 |
|----|------|------|----------|
| 後端 | Spring Boot 3.4 + MyBatis | [Render.com](https://render.com)（免費，不需信用卡） | [Dashboard](https://dashboard.render.com) |
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

### 後端（Render Environment Variables）

| 變數 | 說明 |
|------|------|
| `LINE_CHANNEL_TOKEN` | LINE Messaging API Channel Access Token |
| `LINE_CHANNEL_SECRET` | LINE Messaging API Channel Secret |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC 連線字串（`jdbc:postgresql://...`） |
| `SPRING_DATASOURCE_USERNAME` | DB 帳號 |
| `SPRING_DATASOURCE_PASSWORD` | DB 密碼 |
| `INITIAL_ADMIN_USER_ID` | 第一個管理員的 LINE userId（DB 空時自動建立） |
| `DEVELOPER_USER_ID` | 開發人員的 LINE userId（永久管理員，無法被移除） |
| `RICH_MENU_ID` | LINE Rich Menu ID |
| `CORS_ALLOWED_ORIGINS` | 前端網址（Vercel URL） |

### 前端（Vercel Environment Variables）

| 變數 | 說明 |
|------|------|
| `VITE_API_BASE_URL` | 後端 API 網址（`https://xxx.fly.dev`） |

## 資料庫遷移（已有舊資料庫時）

若已有舊版資料庫，需執行以下遷移 SQL：

```sql
-- 1. 移除舊的 inline UNIQUE 約束，改用 partial unique index
--    允許已取消的場次在同時段重新建立
ALTER TABLE sessions DROP CONSTRAINT IF EXISTS sessions_session_date_start_time_key;
CREATE UNIQUE INDEX IF NOT EXISTS idx_sessions_date_time_open
  ON sessions(session_date, start_time) WHERE status = 'OPEN';

-- 2. 新增場次申請表
CREATE TABLE IF NOT EXISTS session_requests (
  id           BIGSERIAL    PRIMARY KEY,
  line_user_id VARCHAR(64)  NOT NULL,
  request_date DATE         NOT NULL,
  request_time TIME         NOT NULL,
  note         VARCHAR(300),
  status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
  reviewed_by  VARCHAR(64),
  review_note  VARCHAR(200),
  session_id   BIGINT       REFERENCES sessions(id),
  created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
  updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_session_requests_user   ON session_requests(line_user_id);
CREATE INDEX IF NOT EXISTS idx_session_requests_status ON session_requests(status);
```

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

## API 端點摘要

### 用戶端

| 方法 | 路徑 | 說明 |
|------|------|------|
| `GET` | `/api/me` | 取得當前用戶資料（含 admin / developer 標記） |
| `GET` | `/api/sessions` | 查詢 OPEN 場次（月曆用） |
| `POST` | `/api/reservations` | 預約桌位 |
| `GET` | `/api/reservations/my` | 查詢我的預約 |
| `DELETE` | `/api/reservations/{id}` | 取消預約 |
| `POST` | `/api/session-requests` | 送出場次申請 |
| `GET` | `/api/session-requests/my` | 查詢我的申請記錄 |

### 管理員端

| 方法 | 路徑 | 說明 |
|------|------|------|
| `GET` | `/api/users` | 查詢所有用戶名單（管理員選取用） |
| `POST` | `/api/sessions` | 建立場次 |
| `DELETE` | `/api/sessions/{id}` | 取消場次 |
| `GET` | `/api/session-requests/pending` | 查詢待審核申請 |
| `GET` | `/api/session-requests` | 查詢所有申請 |
| `POST` | `/api/session-requests/{id}/review` | 核准或拒絕申請 |
| `GET` | `/api/admins` | 查詢管理員名單 |
| `POST` | `/api/admins/by-user/{lineUserId}` | 設定用戶為管理員 |
| `DELETE` | `/api/admins/by-user/{lineUserId}` | 取消用戶管理員身分 |

## 相關服務連結

| 服務 | 用途 | 連結 |
|------|------|------|
| Render Dashboard | 後端部署、logs、環境變數管理 | https://dashboard.render.com |
| UptimeRobot | 每5分鐘 ping 防止 Render 休眠 | https://uptimerobot.com |
| Supabase Dashboard | 資料庫管理、SQL Editor | https://supabase.com/dashboard |
| Vercel Dashboard | 前端部署、環境變數 | https://vercel.com/dashboard |
| LINE Developers Console | Bot 設定、LIFF、Webhook | https://developers.line.biz |
| LINE OA Manager | 回應設定、Rich Menu、推播 | https://manager.line.biz |

## 授權

MIT
