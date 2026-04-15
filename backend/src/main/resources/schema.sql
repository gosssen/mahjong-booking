-- 麻將預約系統 DDL
-- 使用 IF NOT EXISTS 保冪，可安全重啟

CREATE TABLE IF NOT EXISTS users (
  id           BIGSERIAL    PRIMARY KEY,
  line_user_id VARCHAR(64)  NOT NULL UNIQUE,
  display_name VARCHAR(100) NOT NULL,
  picture_url  VARCHAR(500),
  created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
  updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS admins (
  id           BIGSERIAL    PRIMARY KEY,
  line_user_id VARCHAR(64)  NOT NULL UNIQUE,
  added_by     VARCHAR(64),
  created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS blocked_dates (
  id           BIGSERIAL    PRIMARY KEY,
  blocked_date DATE         NOT NULL UNIQUE,
  reason       VARCHAR(200),
  blocked_by   VARCHAR(64)  NOT NULL,
  created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 場次（一個日期可有多個不同時間的場次）
-- 注意：同日同時間不允許重複的 OPEN 場次（使用 partial unique index）
-- 已取消的場次不受限，允許在同時段重新建立
CREATE TABLE IF NOT EXISTS sessions (
  id            BIGSERIAL    PRIMARY KEY,
  session_date  DATE         NOT NULL,
  start_time    TIME         NOT NULL,
  status        VARCHAR(20)  NOT NULL DEFAULT 'OPEN',  -- OPEN / CANCELLED
  created_by    VARCHAR(64)  NOT NULL,
  cancel_reason VARCHAR(200),
  reminder_sent BOOLEAN      NOT NULL DEFAULT FALSE,   -- 開打前1小時提醒是否已發
  created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 桌（每場次預設 1 桌，管理員可追加）
CREATE TABLE IF NOT EXISTS mahjong_tables (
  id           BIGSERIAL    PRIMARY KEY,
  session_id   BIGINT       NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
  table_number SMALLINT     NOT NULL,
  UNIQUE (session_id, table_number)
);

-- 預約
CREATE TABLE IF NOT EXISTS reservations (
  id           BIGSERIAL    PRIMARY KEY,
  session_id   BIGINT       NOT NULL REFERENCES sessions(id),
  table_id     BIGINT       NOT NULL REFERENCES mahjong_tables(id),
  line_user_id VARCHAR(64)  NOT NULL,
  guest_count  SMALLINT     NOT NULL DEFAULT 0,            -- 額外攜帶的朋友人數（不含本人）
  status       VARCHAR(20)  NOT NULL DEFAULT 'CONFIRMED',  -- CONFIRMED / CANCELLED
  cancelled_by VARCHAR(64),
  cancel_note  VARCHAR(200),
  created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
  updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
  UNIQUE (session_id, line_user_id)  -- 同場次每人只能一筆
);

-- 推播用量（月累計）
CREATE TABLE IF NOT EXISTS push_log (
  id         BIGSERIAL    PRIMARY KEY,
  year_month CHAR(7)      NOT NULL UNIQUE,  -- '2026-04'
  count      INT          NOT NULL DEFAULT 0
);

-- 場次申請（一般用戶申請開場，由管理員審核）
CREATE TABLE IF NOT EXISTS session_requests (
  id           BIGSERIAL    PRIMARY KEY,
  line_user_id VARCHAR(64)  NOT NULL,
  request_date DATE         NOT NULL,
  request_time TIME         NOT NULL,
  note         VARCHAR(300),
  status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',  -- PENDING / APPROVED / REJECTED
  reviewed_by  VARCHAR(64),
  review_note  VARCHAR(200),
  session_id   BIGINT       REFERENCES sessions(id),     -- 核准後建立的場次
  created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
  updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_sessions_date         ON sessions(session_date);
CREATE INDEX IF NOT EXISTS idx_sessions_reminder      ON sessions(session_date, start_time, status, reminder_sent);
CREATE INDEX IF NOT EXISTS idx_tables_session         ON mahjong_tables(session_id);
CREATE INDEX IF NOT EXISTS idx_reservations_session   ON reservations(session_id);
CREATE INDEX IF NOT EXISTS idx_reservations_user      ON reservations(line_user_id);
CREATE INDEX IF NOT EXISTS idx_reservations_table     ON reservations(table_id);
CREATE INDEX IF NOT EXISTS idx_reservations_status    ON reservations(session_id, status);
CREATE INDEX IF NOT EXISTS idx_session_requests_user  ON session_requests(line_user_id);
CREATE INDEX IF NOT EXISTS idx_session_requests_status ON session_requests(status);

-- Partial unique index：同日同時間只允許一個 OPEN 場次
-- 已取消的場次不受限，允許在同時段重新建立
CREATE UNIQUE INDEX IF NOT EXISTS idx_sessions_date_time_open
  ON sessions(session_date, start_time)
  WHERE status = 'OPEN';

-- ============================================================
-- 遷移腳本（已有舊資料庫時執行，新建不需要）
-- ============================================================
-- 若舊資料庫有 inline UNIQUE 約束，需先刪除再套用：
--   ALTER TABLE sessions DROP CONSTRAINT IF EXISTS sessions_session_date_start_time_key;
--   CREATE UNIQUE INDEX IF NOT EXISTS idx_sessions_date_time_open
--     ON sessions(session_date, start_time) WHERE status = 'OPEN';
--   CREATE TABLE IF NOT EXISTS session_requests ( ... 同上 ... );
-- 加入攜伴欄位（2026-04 之後的遷移）：
--   ALTER TABLE reservations ADD COLUMN IF NOT EXISTS guest_count SMALLINT NOT NULL DEFAULT 0;
-- ============================================================
