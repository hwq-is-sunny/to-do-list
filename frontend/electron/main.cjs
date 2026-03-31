const { app, BrowserWindow } = require("electron");
const fs = require("fs");
const path = require("path");
const { spawn } = require("child_process");

let backendProc = null;
const BACKEND_PORT = app.isPackaged ? "17861" : "17860";
const BACKEND_BASE_URL = `http://127.0.0.1:${BACKEND_PORT}`;

function loadAppConfig() {
  // Priority:
  // 1) User override: %APPDATA%/DocFormatAI/app-config.json
  // 2) Packaged app config: resources/app-config.json
  // 3) Dev config: frontend/app-config.json
  const candidates = [
    path.join(app.getPath("appData"), "DocFormatAI", "app-config.json"),
    app.isPackaged ? path.join(process.resourcesPath, "app-config.json") : path.join(__dirname, "..", "app-config.json"),
  ];
  for (const p of candidates) {
    try {
      if (!fs.existsSync(p)) continue;
      const raw = fs.readFileSync(p, "utf-8");
      const data = JSON.parse(raw);
      if (data && typeof data === "object") return data;
    } catch {
      // ignore invalid config and continue
    }
  }
  return {};
}

function startBackendBestEffort() {
  if (process.env.DOCFORMAT_NO_BACKEND === "1") return;
  if (backendProc) return;
  const appConfig = loadAppConfig();

  let cmd;
  let args = [];
  let cwd;

  if (app.isPackaged) {
    // In production package, backend executable is shipped in resources/backend/.
    cmd = path.join(process.resourcesPath, "backend", "docformat-backend.exe");
    cwd = path.dirname(cmd);
  } else {
    // Dev mode uses Python venv in workspace.
    const backendRoot = path.resolve(__dirname, "..", "..", "backend");
    cmd = path.join(backendRoot, ".venv", "Scripts", "python.exe");
    args = ["-m", "uvicorn", "app.main:app", "--port", BACKEND_PORT];
    cwd = backendRoot;
  }

  try {
    const childEnv = { ...process.env };
    childEnv.DOCFORMAT_PORT = BACKEND_PORT;
    childEnv.DOCFORMAT_BACKEND_BASE_URL = BACKEND_BASE_URL;
    if (typeof appConfig.llm_provider === "string") childEnv.DOCFORMAT_LLM_PROVIDER = appConfig.llm_provider;
    if (typeof appConfig.llm_base_url === "string") childEnv.DOCFORMAT_LLM_BASE_URL = appConfig.llm_base_url;
    if (typeof appConfig.llm_api_key === "string") childEnv.DOCFORMAT_LLM_API_KEY = appConfig.llm_api_key;
    if (typeof appConfig.llm_model === "string") childEnv.DOCFORMAT_LLM_MODEL = appConfig.llm_model;

    backendProc = spawn(cmd, args, {
      cwd,
      stdio: "ignore",
      windowsHide: true,
      env: childEnv,
    });
  } catch {
    backendProc = null;
  }
}

function createWindow() {
  process.env.DOCFORMAT_BACKEND_BASE_URL = BACKEND_BASE_URL;
  const win = new BrowserWindow({
    width: 1200,
    height: 800,
    backgroundColor: "#0b0f14",
    title: "DocFormat AI",
    webPreferences: {
      preload: path.join(__dirname, "preload.cjs"),
    },
  });

  if (app.isPackaged) {
    win.loadFile(path.join(__dirname, "..", "dist", "index.html"));
  } else {
    const devUrl = process.env.VITE_DEV_SERVER_URL || "http://127.0.0.1:5173";
    win.loadURL(devUrl);
  }
}

app.whenReady().then(() => {
  startBackendBestEffort();
  createWindow();
  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") app.quit();
});

app.on("before-quit", () => {
  try {
    backendProc?.kill();
  } catch {
    // ignore
  }
});

