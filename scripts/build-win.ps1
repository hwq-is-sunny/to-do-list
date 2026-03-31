param(
  [switch]$SkipInstall,
  [string]$LlmApiKey = "",
  [string]$LlmBaseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
  [string]$LlmModel = "gemini-2.0-flash",
  [string]$LlmProvider = "openai_compatible"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "backend"
$frontend = Join-Path $root "frontend"
$backendBin = Join-Path $frontend "backend-bin"
$frontendConfig = Join-Path $frontend "app-config.json"

Write-Host "==> Build backend executable"
Set-Location $backend

if (-not (Test-Path ".venv\Scripts\python.exe")) {
  throw "Python venv not found: backend\.venv. Please create it first."
}

if (-not $SkipInstall) {
  .\.venv\Scripts\python -m pip install -r requirements.txt
  .\.venv\Scripts\python -m pip install pyinstaller
}

.\.venv\Scripts\python -m PyInstaller `
  --noconfirm `
  --clean `
  --onefile `
  --name docformat-backend `
  app\run_backend.py

if (-not (Test-Path $backendBin)) {
  New-Item -ItemType Directory -Path $backendBin | Out-Null
}

Copy-Item -Force ".\dist\docformat-backend.exe" (Join-Path $backendBin "docformat-backend.exe")

Write-Host "==> Build Electron Windows installer"
Set-Location $frontend

if ($LlmApiKey -and $LlmApiKey.Trim().Length -gt 0) {
  $cfg = @{
    llm_provider = $LlmProvider
    llm_base_url = $LlmBaseUrl
    llm_api_key = $LlmApiKey
    llm_model = $LlmModel
  } | ConvertTo-Json
  Set-Content -Path $frontendConfig -Value $cfg -Encoding UTF8
  Write-Host "Wrote frontend\app-config.json with embedded LLM settings."
} else {
  Write-Host "No LLM key provided. Installer will require runtime env config or user app-config override."
}

if (-not $SkipInstall) {
  npm install
}

npm run dist:win

Write-Host ""
Write-Host "Build complete. Check frontend\dist for installer (.exe)."

