# 公開職缺與求職者入口

## CI 與 API 文件

- GitHub Actions 會分別執行後端測試、前端測試與前端建置。
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`
- Demo 初始化器會在空資料庫建立一筆公開的 Backend Engineer 職缺。

## 公開職缺與線上應徵

- 公開職缺列表：`http://localhost:5173/careers`
- 求職者可填基本資料、技能、自傳、作品集與多筆工作經歷。
- HR／用人主管可在應徵者資料庫開啟完整履歷；用人主管仍受部門資料權限限制。
- 同一個 Email 不可重複應徵同一職缺。
- 履歷送出後產生不可猜測的查詢 token；資料庫只保存 SHA-256 雜湊。

## 求職者進度入口

- 送出履歷後會導向專屬進度頁。
- 進度由內部 `ApplicationStage` 轉成求職者可理解的文字。
- 公開回應刻意排除評分、內部備註、操作人與淘汰原因。
- 查詢 token 預設 30 天到期，可用 `CANDIDATE_PORTAL_TOKEN_TTL_DAYS` 調整。

