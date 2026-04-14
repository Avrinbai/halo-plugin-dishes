/**
 * 与 Halo 插件后端通信。
 * - 设置 `VITE_API_BASE_URL`（如 https://your-blog.com）时，开发与生产均请求该根地址；
 * - 未设置时请求相对路径（同域）。
 *
 * 约定：插件前台 API 前缀为 `/plugins/dishes/public`（非资源型路径，便于匿名访问授权）。
 */
function apiBase(): string {
  const raw = import.meta.env.VITE_API_BASE_URL ?? ''
  return typeof raw === 'string' ? raw.replace(/\/+$/, '') : ''
}

function apiPrefix(): string {
  const raw = import.meta.env.VITE_API_PREFIX ?? '/plugins/dishes/public'
  const s = typeof raw === 'string' ? raw : '/plugins/dishes/public'
  return `/${s}`.replace(/\/+$/, '').replace(/\/{2,}/g, '/')
}

const ACCESS_TOKEN_KEY = 'dishes_access_token'
export class ApiError extends Error {
  readonly code?: string

  constructor(message: string, code?: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

type ErrorCodeMessageMap = Partial<Record<string, string>>

function accessToken(): string {
  try {
    return localStorage.getItem(ACCESS_TOKEN_KEY) ?? ''
  } catch {
    return ''
  }
}

export function setAccessToken(token: string) {
  try {
    if (token.trim()) localStorage.setItem(ACCESS_TOKEN_KEY, token.trim())
    else localStorage.removeItem(ACCESS_TOKEN_KEY)
  } catch {
    // ignore
  }
}

function withAccessHeaders(init: HeadersInit): HeadersInit {
  const token = accessToken()
  if (!token) return init
  return {
    ...init,
    'X-Dishes-Access-Token': token,
  }
}

function readCookie(name: string): string {
  try {
    const source = typeof document !== 'undefined' ? document.cookie ?? '' : ''
    if (!source) return ''
    const parts = source.split(';')
    for (const p of parts) {
      const [k, ...rest] = p.trim().split('=')
      if (k === name) return decodeURIComponent(rest.join('='))
    }
    return ''
  } catch {
    return ''
  }
}

function readMeta(name: string): string {
  try {
    if (typeof document === 'undefined') return ''
    const el = document.querySelector(`meta[name="${name}"]`)
    return (el?.getAttribute('content') ?? '').trim()
  } catch {
    return ''
  }
}

function resolveCsrf(): { headerName: string; token: string } | null {
  try {
    const fromWindow = (window as unknown as { __DISHES_CSRF__?: { headerName?: string; token?: string } })
      .__DISHES_CSRF__
    const h1 = (fromWindow?.headerName ?? '').trim()
    const t1 = (fromWindow?.token ?? '').trim()
    if (h1 && t1) return { headerName: h1, token: t1 }
  } catch {
    // ignore
  }

  const h2 = readMeta('dishes-csrf-header')
  const t2 = readMeta('dishes-csrf-token')
  if (h2 && t2) return { headerName: h2, token: t2 }

  const token =
    readCookie('XSRF-TOKEN') || readCookie('CSRF-TOKEN') || readCookie('X-CSRF-TOKEN')
  if (token) return { headerName: 'X-XSRF-TOKEN', token }

  return null
}

export async function apiGet<T>(path: string): Promise<T> {
  const base = apiBase()
  const prefix = apiPrefix()
  const res = await fetch(`${base}${prefix}${path}`, {
    headers: withAccessHeaders({ Accept: 'application/json' }),
    credentials: 'include',
  })
  const { json, parseNote } = await readJsonBody(res)

  if (!res.ok || !isOkEnvelope(json)) {
    throw toApiError(res, json, parseNote)
  }

  return (json as { data: T }).data
}

export async function apiPost<T>(path: string, body: unknown): Promise<T> {
  const base = apiBase()
  const prefix = apiPrefix()
  const headers: HeadersInit = withAccessHeaders({
    Accept: 'application/json',
    'Content-Type': 'application/json',
  })
  const csrf = resolveCsrf()
  if (csrf) {
    ;(headers as Record<string, string>)[csrf.headerName] = csrf.token
    // 兼容部分服务端网关只认 X-XSRF-TOKEN
    if (csrf.headerName !== 'X-XSRF-TOKEN') {
      ;(headers as Record<string, string>)['X-XSRF-TOKEN'] = csrf.token
    }
  }
  const res = await fetch(`${base}${prefix}${path}`, {
    method: 'POST',
    headers,
    body: JSON.stringify(body),
    credentials: 'include',
  })
  const { json, parseNote } = await readJsonBody(res)

  if (!res.ok || !isOkEnvelope(json)) {
    throw toApiError(res, json, parseNote)
  }

  return (json as { data: T }).data
}

function isOkEnvelope(v: unknown): v is { ok: true; data: unknown } {
  return typeof v === 'object' && v !== null && (v as { ok?: unknown }).ok === true
}

async function readJsonBody(res: Response): Promise<{
  json: unknown
  parseNote?: string
}> {
  const text = await res.text()
  if (!text.trim()) {
    return { json: {}, parseNote: '响应体为空' }
  }
  try {
    return { json: JSON.parse(text) as unknown }
  } catch {
    const hint =
      text.trimStart().startsWith('<') || text.includes('<!DOCTYPE')
        ? '收到 HTML 而非 JSON，多为未登录或接口无匿名访问权限'
        : '响应不是合法 JSON'
    return { json: {}, parseNote: hint }
  }
}

function resolveApiErrorMessage(res: Response, json: unknown, parseNote?: string): string {
  if (
    typeof json === 'object' &&
    json !== null &&
    'message' in json &&
    typeof (json as { message: unknown }).message === 'string'
  ) {
    return (json as { message: string }).message
  }
  if (parseNote) {
    return parseNote
  }
  return res.statusText || '请求失败'
}

function resolveApiErrorCode(json: unknown): string | undefined {
  if (
    typeof json === 'object' &&
    json !== null &&
    'code' in json &&
    typeof (json as { code: unknown }).code === 'string'
  ) {
    return (json as { code: string }).code
  }
  return undefined
}

function toApiError(res: Response, json: unknown, parseNote?: string): ApiError {
  return new ApiError(resolveApiErrorMessage(res, json, parseNote), resolveApiErrorCode(json))
}

export function getApiErrorMessage(
  error: unknown,
  fallback: string,
  codeMessageMap?: ErrorCodeMessageMap,
): string {
  if (error instanceof ApiError) {
    if (error.code && codeMessageMap?.[error.code]) return codeMessageMap[error.code] as string
    return error.message || fallback
  }
  if (error instanceof Error) return error.message || fallback
  return fallback
}
