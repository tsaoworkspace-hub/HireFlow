# Git Workflow

## Branch

- `main`：隨時可展示的穩定版本。
- `feature/<ticket>-<topic>`：功能開發。
- `fix/<ticket>-<topic>`：錯誤修正。

## Commit

使用 Conventional Commits：

```text
feat(requisition): add requisition submission
fix(interview): reject overlapping schedules
test(auth): cover unauthorized access
docs(readme): add local setup steps
```

一次 Commit 只做一件事。合併前至少確認後端測試、前端測試與前端 Build 通過。

