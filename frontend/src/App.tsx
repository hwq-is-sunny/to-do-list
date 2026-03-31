import { useRef, useState } from 'react'
import './App.css'

type ApplyResult = {
  new_version?: number
  output_filename?: string
  integrity_warnings?: string[]
  [key: string]: unknown
}

class ApiError extends Error {
  code?: string
  constructor(message: string, code?: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

declare global {
  interface Window {
    docformat?: {
      backendBaseUrl?: string
    }
  }
}

function App() {
  const backendBaseUrl = window.docformat?.backendBaseUrl ?? 'http://127.0.0.1:17860'
  const fileInputRef = useRef<HTMLInputElement | null>(null)

  const [file, setFile] = useState<File | null>(null)
  const [dragActive, setDragActive] = useState(false)
  const [taskId, setTaskId] = useState<string>('')
  const [userText, setUserText] = useState<string>('把正文改成宋体，小四，1.5 倍行距')
  const [applyResult, setApplyResult] = useState<ApplyResult | null>(null)
  const [latestRiskHints, setLatestRiskHints] = useState<string[]>([])
  const [latestIntegrityWarnings, setLatestIntegrityWarnings] = useState<string[]>([])
  const [busy, setBusy] = useState<string>('')
  const [errorText, setErrorText] = useState<string>('')
  const [statusText, setStatusText] = useState<string>('等待上传文档')

  function setSelectedFile(nextFile: File | null) {
    setFile(nextFile)
    setErrorText('')
    setApplyResult(null)
    setLatestRiskHints([])
    setLatestIntegrityWarnings([])
    setTaskId('')
    if (nextFile) {
      setStatusText(`已选择文件：${nextFile.name}，请点击“先上传”或直接“一键修改”`)
    } else {
      setStatusText('等待上传文档')
    }
  }

  async function requestJson(url: string, init?: RequestInit) {
    const r = await fetch(url, init)
    if (!r.ok) {
      let code: string | undefined
      let message = r.statusText
      const raw = await r.text()
      try {
        const data = raw ? JSON.parse(raw) : null
        const detail = data?.detail
        if (typeof detail === 'string') {
          message = detail
        } else if (detail && typeof detail === 'object') {
          code = typeof detail.code === 'string' ? detail.code : undefined
          message = typeof detail.message === 'string' ? detail.message : JSON.stringify(detail)
        } else if (data) {
          message = JSON.stringify(data)
        } else {
          message = raw || r.statusText
        }
      } catch {
        message = raw || r.statusText
      }
      throw new ApiError(`请求失败(${r.status}): ${message}`, code)
    }
    return r.json()
  }

  async function upload() {
    if (!file) return
    setBusy('upload')
    setErrorText('')
    try {
      const fd = new FormData()
      fd.append('file', file)
      const data = await requestJson(`${backendBaseUrl}/api/doc/upload`, {
        method: 'POST',
        body: fd,
      })
      setTaskId(data.task_id)
      setStatusText('文档已上传，等待提交指令')
    } catch (e) {
      setErrorText(e instanceof Error ? e.message : String(e))
      setStatusText('上传失败')
    } finally {
      setBusy('')
    }
  }

  async function oneClickModify() {
    if (!file) return
    setBusy('oneclick')
    setErrorText('')
    try {
      let currentTaskId = taskId
      if (!currentTaskId) {
        setStatusText('正在上传文档…')
        const fd = new FormData()
        fd.append('file', file)
        const uploadData = await requestJson(`${backendBaseUrl}/api/doc/upload`, {
          method: 'POST',
          body: fd,
        })
        currentTaskId = uploadData.task_id
        setTaskId(currentTaskId)
      }

      setStatusText('正在分析文档结构…')
      const summary = await requestJson(`${backendBaseUrl}/api/doc/analyze`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ task_id: currentTaskId }),
      })

      setStatusText('AI 正在理解你的指令…')
      const planData = await requestJson(`${backendBaseUrl}/api/ai/plan`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ task_id: currentTaskId, user_text: userText, doc_summary: summary }),
      })

      setStatusText('正在生成修改预览…')
      const previewData = await requestJson(`${backendBaseUrl}/api/doc/preview`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ task_id: currentTaskId, plan: planData }),
      })
      setLatestRiskHints((previewData.risk_hints as string[] | undefined) ?? [])
      setLatestIntegrityWarnings((previewData.integrity_warnings as string[] | undefined) ?? [])

      setStatusText('正在应用修改并导出…')
      const applyData = await requestJson(`${backendBaseUrl}/api/doc/apply`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ task_id: currentTaskId, plan: planData }),
      })
      setApplyResult(applyData)
      setLatestIntegrityWarnings((applyData.integrity_warnings as string[] | undefined) ?? [])
      setStatusText('已完成，可直接下载新文档')
    } catch (e) {
      const text = e instanceof Error ? e.message : String(e)
      setErrorText(text)
      setStatusText('执行失败，请检查接口配置')
    } finally {
      setBusy('')
    }
  }

  function onDropFile(e: React.DragEvent<HTMLDivElement>) {
    e.preventDefault()
    setDragActive(false)
    const f = e.dataTransfer.files?.[0]
    if (f && (f.name.toLowerCase().endsWith('.docx') || f.name.toLowerCase().endsWith('.doc'))) {
      setSelectedFile(f)
    } else {
      setErrorText('仅支持 .docx 或 .doc 文件')
    }
  }

  return (
    <div className="app">
      <main className="chat-shell">
        <section className="chat-card">
          <div className="brand-line">
            <div className="dot" />
            <div>
              <div className="title">DocFormat AI</div>
              <div className="subtitle">上传文档，输入一句话，一键生成修改版</div>
            </div>
          </div>

          <input
            ref={fileInputRef}
            className="hidden-input"
            type="file"
            accept=".doc,.docx"
            onChange={(e) => setSelectedFile(e.target.files?.[0] ?? null)}
          />
          <div
            className={`dropzone ${dragActive ? 'active' : ''}`}
            onDragOver={(e) => {
              e.preventDefault()
              setDragActive(true)
            }}
            onDragLeave={() => setDragActive(false)}
            onDrop={onDropFile}
            onClick={() => fileInputRef.current?.click()}
          >
            <div className="drop-title">拖拽或点击上传 Word 文件（.doc/.docx）</div>
            <div className="drop-sub">{file ? file.name : '仅需上传一次，后续可连续修改'}</div>
          </div>

          <textarea
            className="textarea"
            value={userText}
            onChange={(e) => setUserText(e.target.value)}
            placeholder="例如：把正文改成宋体小四，1.5 倍行距，首行缩进 2 字符"
          />

          <div className="row">
            <button className="btn ghost" disabled={!file || busy !== ''} onClick={upload}>
              {busy === 'upload' ? '上传中…' : '先上传'}
            </button>
            <button className="btn primary" disabled={!file || !userText || busy !== ''} onClick={oneClickModify}>
              {busy === 'oneclick' ? '处理中…' : '一键修改'}
            </button>
          </div>

          <div className="status-line">{statusText}</div>
          {errorText ? <div className="warn">{errorText}</div> : null}
          {errorText.includes('LIBREOFFICE_MISSING') || errorText.toLowerCase().includes('libreoffice') ? (
            <div className="guide-box">
              <div className="guide-title">检测到 .doc 转换组件缺失</div>
              <div className="drop-sub">请安装 LibreOffice（免费开源），安装后重启应用即可处理 .doc 文件。</div>
              <a
                className="download-btn"
                href="https://www.libreoffice.org/download/download-libreoffice/"
                target="_blank"
                rel="noreferrer"
              >
                下载 LibreOffice
              </a>
            </div>
          ) : null}

          {latestRiskHints.length > 0 ? (
            <details className="details">
              <summary>查看风险提示</summary>
              <div className="list">
                {latestRiskHints.map((x, i) => (
                  <div className="warn" key={`risk-${i}`}>
                    {x}
                  </div>
                ))}
              </div>
            </details>
          ) : null}

          {applyResult?.new_version ? (
            <div className="result-box">
              <div className="ok">已完成修改，可下载新文档</div>
              {latestIntegrityWarnings.length > 0 ? (
                <div className="list">
                  {latestIntegrityWarnings.map((x, i) => (
                    <div className="warn" key={`apply-integrity-${i}`}>
                      {x}
                    </div>
                  ))}
                </div>
              ) : (
                <div className="ok soft">结构完整性检查通过</div>
              )}
              <a
                className="download-btn"
                href={`${backendBaseUrl}/api/doc/download?task_id=${encodeURIComponent(
                  taskId,
                )}&version=${applyResult.new_version}`}
              >
                下载修改后的文档
              </a>
            </div>
          ) : null}
        </section>
      </main>
    </div>
  )
}

export default App
