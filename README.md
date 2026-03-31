## DocFormat AI（MVP）

桌面端优先（Electron + React + TypeScript），内置本地后端（Python FastAPI）对 `.docx` 做结构分析、格式修改、版本导出与历史回滚。

### 目录结构

- `backend/`: FastAPI 服务（文档任务、版本链、清理策略、LLM provider 抽象、JSON plan 校验、OpenXML 补丁式写入）
- `frontend/`: Electron + React UI（MVP：上传/聊天/预览摘要/应用/导出）

### 后端开发启动

1) 进入 `backend/`，创建虚拟环境并安装依赖

2) 启动服务：

```bash
python -m uvicorn app.main:app --reload --port 17860
```

### 说明（MVP 默认策略）

- 文档与版本文件保存于用户应用数据目录（Windows：`%APPDATA%\\DocFormatAI\\`）
- 支持上传 `.docx` 与 `.doc`：
  - `.docx` 直接处理
  - `.doc` 上传时会先自动转换为 `.docx`（依赖 LibreOffice）
- 清理策略：
  - 最多保留最近 20 个版本
  - 超过 7 天的临时/历史文件自动清理
  - 当前正在编辑/最近一次成功导出的版本不清理
  - 清理失败不影响主流程

### Windows 安装包（双击可用）

目标形态：用户下载 `.exe` 安装包后，双击图标即可使用，不需要手动启动后端。

#### 本机构建安装包

在项目根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File ".\scripts\build-win.ps1"
```

如果你希望“终端用户无需手动输入 API Key”，可在打包时内置：

```powershell
powershell -ExecutionPolicy Bypass -File ".\scripts\build-win.ps1" `
  -LlmApiKey "YOUR_API_KEY" `
  -LlmBaseUrl "https://generativelanguage.googleapis.com/v1beta/openai" `
  -LlmModel "gemini-2.0-flash"
```

这会生成 `frontend\app-config.json` 并打进安装包。应用启动后会自动读取该配置并注入后端进程。

> 注意：客户端内置 Key 可被提取，适合内测或小规模场景。正式商用建议使用你自己的服务端中转。

构建完成后，安装包在 `frontend\dist\` 目录（`nsis` 安装器）。

#### 运行方式（最终用户）

- 双击安装包安装
- 从开始菜单或桌面图标启动 `DocFormat AI`
- 应用会自动启动内置后端（无需手动打开 Python 服务）

