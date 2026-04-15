---
paths:
  - "frontend/src/**/*.tsx"
  - "frontend/src/**/*.ts"
---

# 前端規範

## 日期處理（時區關鍵）
- 永遠用 `toISODate(d: Date)` from `utils/format.ts`，**不要用** `d.toISOString().slice(0,10)`
- `toISODate` 使用本地時間（getFullYear/getMonth/getDate），避免 UTC 跨日偏移

## 座位計算（攜伴）
- 座位數 = `(reservations ?? []).reduce((sum, r) => sum + 1 + (r.guestCount ?? 0), 0)`
- **不要用** `reservations.length`，它不含攜伴人數

## API Types
- `Reservation.guestCount: number` — 攜伴數（0~3），預設 0
- `Reservation.guestLabel: string | null` — 朋友拆出後的名稱，null 表示正常 LINE 用戶
- 判斷是否為拆出的朋友記錄：`r.lineUserId.startsWith('guest_')`

## 樣式
- 使用 Tailwind class，不用 inline style
- 朋友格用淡藍色 `bg-blue-50 border-blue-100 text-blue-400`
- 攜伴選擇器用藍色主題 `bg-blue-500`

## 類型驗證
- 每次修改前執行 `npx tsc --noEmit` 確認無型別錯誤

## 部署
- 前端部署：`vercel --prod`（在 frontend/ 目錄下執行）
- 後端：push 到 GitHub main 即自動觸發 Render 部署
