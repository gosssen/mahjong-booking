---
paths:
  - "backend/src/**/*.java"
  - "backend/src/main/resources/**/*.xml"
  - "backend/src/main/resources/**/*.yml"
  - "backend/pom.xml"
---

# Java 後端規範

## 程式碼風格（Google Java Style Guide）
- 縮排：2 格空白
- 行寬：100 字元
- 大括號：Egyptian style
- import：禁止萬用字元

## ORM 規則（MyBatis XML Mapper 模式）
- **不使用** `@Select`/`@Insert` 等 annotations，一律在 XML Mapper 檔案寫 SQL
- resultMap 要明確對應所有欄位，不依賴自動映射
- 新增欄位時要同時更新 `SessionMapper.xml` 和 `ReservationMapper.xml` 兩個 resultMap（共用欄位）

## 座位計算
- 座位數 = `Σ(1 + guest_count)` for each CONFIRMED reservation
- 永遠用 `ReservationService.guestSeats(r)` helper，不要重複寫 null-check

## 時區
- 所有時間判斷以 **Asia/Taipei** 為準
- SQL 中用 `(CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Taipei')::date`
- Java 中用 `ZoneId.of("Asia/Taipei")`，不用 `LocalDate.now()` 裸調

## 驗證規則
- guest_count：後端強制 0~3，超出丟 400
- 建立場次：不允許過去時間（台灣時區），時間需 10 分鐘單位

## 測試
- 修改 ReservationService 後執行 `mvn test`，確認 69 個 tests 全過
