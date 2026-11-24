# Day 2 Checklist

## 已完成

- [x] 四種 Demo 角色與帳號可由啟動程序安全、重複執行地建立。
- [x] 密碼由 BCrypt 雜湊保存，不回傳密碼或雜湊。
- [x] JSON 登入、目前使用者與登出 API。
- [x] 登入狀態保存在伺服器 Session。
- [x] 未登入受保護 API 回傳 401，角色不足回傳 403。
- [x] Session 型狀態修改請求具備 CSRF 防護。
- [x] Vue 登入頁、Session 還原、路由守衛、403 頁面與登出。
- [x] 導覽依角色顯示，後端保留最終授權權威。
- [x] 後端登入整合測試、前端服務測試與 production build 通過。

## 設計說明

Authentication 回答「目前是誰」，登入成功後由 Spring Security 將身分保存到伺服器 Session。Authorization 回答「這個人能不能做這件事」，後端依角色判斷，不信任前端隱藏按鈕的結果。

- 401：尚未登入或登入狀態失效。
- 403：已登入，但角色不允許存取目標功能。
- BCrypt：同一密碼每次產生不同雜湊，資料庫不保存可還原的明文密碼。
- CSRF：避免其他網站利用瀏覽器既有 Session 冒用使用者送出狀態修改請求。

## 下一步

Day 3 實作人力需求的 DRAFT、SUBMITTED、APPROVED、RETURNED 與 REJECTED 狀態，以及核准時同一交易建立 OPEN 職缺。
