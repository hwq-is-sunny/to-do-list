const { contextBridge } = require("electron");

const backendBaseUrl = process.env.DOCFORMAT_BACKEND_BASE_URL || "http://127.0.0.1:17860";

contextBridge.exposeInMainWorld("docformat", {
  backendBaseUrl,
});

