# 錄取決策與稽核驗收

## 錄取決策

- [x] 只有面試皆完成且至少一份評分已送出，才能進入 `DECISION_PENDING`。
- [x] HR 可查看評分彙整，但只有該部門用人主管或 ADMIN 可建立最終決策。
- [x] 決策支援 `HIRED`、`ON_HOLD`、`REJECTED`，且決策說明必填。
- [x] 決策、Application 狀態與階段歷程在同一交易更新。
- [x] Application 使用版本檢查，資料庫以唯一約束阻止重複決策。

## 稽核紀錄

- [x] 人力需求建立、修改、送出與審核會寫入 Audit Log。
- [x] Application 建立與階段轉換會寫入 Audit Log。
- [x] 面試安排、取消、完成、評分送出與解鎖會寫入 Audit Log。
- [x] 最終錄取決策會寫入 Audit Log。
- [x] 只有 ADMIN 可查詢全系統稽核紀錄。
- [x] 失敗操作會 Rollback，不留下誤導性的成功稽核紀錄。

## 驗證結果

- [x] 後端 34 個測試通過。
- [x] 前端 15 個測試通過。
- [x] Vite production build 通過。
- [x] PostgreSQL 17 成功套用 Flyway V5。
- [x] 真實 PostgreSQL 環境可完成 ADMIN 登入並查詢 Audit API。
