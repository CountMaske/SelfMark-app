# 前后端接口交付物

此目录保存提交到 GitHub、交给 Flutter/Vue 前端使用的接口交付物：

- `slice-XX-模块名.openapi.json`：从 Apifox 导出的 JSON 接口文档，必须包含请求体、响应体和错误响应。
- `错误码表.md`：按业务模块维护错误码，新增模块不得复用已有业务码。

认证模块字段约定：请求使用 `mobile` 表示手机号账号，响应使用 `account`，且当前 `account=mobile`；`username` 仅表示用户展示名。

Apifox 的受保护接口统一使用 Bearer Auth，Token 值为环境变量 `{{bearerToken}}`。变量本地值只填写 JWT 本身，不含 `Bearer ` 前缀；Auth 面板会自动生成 `Authorization` 请求头，Headers 面板不得重复维护该请求头。

OpenAPI 的维护源文件仍位于 `api/docs/openapi/selfmark.yaml`。每次接口变更后，先更新 YAML 并测试，再导入 Apifox，最后导出 JSON 到本目录。

错误响应不能让所有 HTTP 状态共用一个带固定 `code` 示例的 schema。共享的 `ApiResponseError` 只定义基础结构，各 HTTP response 引用按状态拆分的 schema（例如 `ApiResponseBadRequest`、`ApiResponseUnauthorized`、`ApiResponseConflict`），并在接口响应处提供与业务场景匹配的完整示例。这样 Apifox 展示和契约校验都不会把 400、401、409 混淆。
