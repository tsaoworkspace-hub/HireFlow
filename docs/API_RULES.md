# API Rules

- 路徑以 `/api` 開頭，資源名稱使用複數名詞。
- 列表 API 統一接受 `page`、`size`、`sort` 與功能性篩選參數。
- 成功與失敗回應採一致 Envelope：`success`、`message`、`data`、`timestamp`。
- 輸入錯誤回傳 400、未登入 401、無權限 403、找不到資源 404、衝突 409。
- Request DTO 使用 Bean Validation；Entity 不直接作為 API Request／Response。
- 時間使用 ISO-8601；後端保存 `Instant` 或有明確時區的型別。
- 新增或修改 API 時，同步更新測試與文件。

