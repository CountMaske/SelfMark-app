# 前后端接口交付物

此目录保存提交到 GitHub、交给 Flutter/Vue 前端使用的接口交付物：

- `slice-XX-模块名.openapi.json`：从 Apifox 导出的 JSON 接口文档，必须包含请求体、响应体和错误响应。
- `错误码表.md`：按业务模块维护错误码，新增模块不得复用已有业务码。

OpenAPI 的维护源文件仍位于 `api/docs/openapi/selfmark.yaml`。每次接口变更后，先更新 YAML 并测试，再导入 Apifox，最后导出 JSON 到本目录。
