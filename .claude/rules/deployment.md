---
paths:
  - ".github/**"
  - "render.yaml"
  - "backend/Dockerfile"
  - "frontend/vite.config.*"
  - "frontend/package.json"
---

# 部署規範

## 後端（Render）
- 推送到 GitHub main → 自動觸發 Render 部署（約 5-10 分鐘）
- 部署前確認 `mvn test` 全過

## 前端（Vercel）
- `vercel --prod`（在 frontend/ 目錄執行）
- GitHub push **不會**自動部署前端，需手動執行

## GitHub Push 注意事項
- 有 `.github/workflows/` 異動時需要 `workflow` scope：
  `gh auth refresh -h github.com -s workflow`
- Push 使用新 token：
  ```bash
  NEW_TOKEN=$(gh auth token)
  git remote set-url origin "https://gosssen:${NEW_TOKEN}@github.com/gosssen/mahjong-booking.git"
  git push origin main
  git remote set-url origin https://github.com/gosssen/mahjong-booking.git
  ```

## Supabase 遷移
- 每次 schema 有異動，到 Supabase SQL Editor 執行遷移 SQL
- 遷移語句記錄在 `backend/src/main/resources/schema.sql` 底部

## 目前待執行的 Supabase Migration
```sql
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS guest_count SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS guest_label VARCHAR(100);
```
