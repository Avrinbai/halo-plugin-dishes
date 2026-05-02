import axios from 'axios'

export const client = axios.create({
  baseURL: '/apis/plugins/dishes/admin',
  headers: { Accept: 'application/json' },
})

export type Envelope<T> = { ok: boolean; data: T; message?: string | null; code?: string | null }

export class ApiError extends Error {
  readonly code?: string

  constructor(message: string, code?: string | null) {
    super(message)
    this.name = 'ApiError'
    this.code = code ?? undefined
  }
}

type ErrorCodeMessageMap = Partial<Record<string, string>>

function toApiError<T>(envelope?: Envelope<T>): ApiError {
  const message = envelope?.message || '请求失败'
  return new ApiError(message, envelope?.code)
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

export async function getData<T>(path: string): Promise<T> {
  const res = await client.get<Envelope<T>>(path)
  if (!res.data?.ok) throw toApiError(res.data)
  return res.data.data
}

export async function postData<T>(path: string, body: unknown): Promise<T> {
  const res = await client.post<Envelope<T>>(path, body, {
    headers: { 'Content-Type': 'application/json' },
  })
  if (!res.data?.ok) throw toApiError(res.data)
  return res.data.data
}

export async function putData<T>(path: string, body: unknown): Promise<T> {
  const res = await client.put<Envelope<T>>(path, body, {
    headers: { 'Content-Type': 'application/json' },
  })
  if (!res.data?.ok) throw toApiError(res.data)
  return res.data.data
}

export async function deleteData<T>(path: string): Promise<T> {
  const res = await client.delete<Envelope<T>>(path)
  if (!res.data?.ok) throw toApiError(res.data)
  return res.data.data
}

export async function downloadBackupZip(params: { includeOrders: boolean }): Promise<Blob> {
  const res = await client.get('/backup/export', {
    params: { includeOrders: params.includeOrders },
    /** 默认实例带 Accept: application/json，与 application/zip 冲突会 406 */
    headers: { Accept: 'application/zip, application/octet-stream, */*' },
    responseType: 'blob',
    validateStatus: () => true,
  })
  const ct = String(res.headers['content-type'] || '')
  if (!ct.includes('zip') && !ct.includes('octet-stream')) {
    const blob = res.data as Blob
    const text = await blob.text()
    try {
      const env = JSON.parse(text) as Envelope<unknown>
      if (!env.ok) throw toApiError(env)
    } catch (e) {
      if (e instanceof ApiError) throw e
      throw new ApiError(text || '导出失败')
    }
    throw new ApiError('导出失败')
  }
  return res.data as Blob
}

export async function importBackupZip(file: File): Promise<{
  imported_categories: number
  imported_dishes: number
  imported_orders: number
}> {
  const fd = new FormData()
  fd.append('file', file)
  const res = await client.post<
    Envelope<{ imported_categories: number; imported_dishes: number; imported_orders: number }>
  >('/backup/import', fd, {
    /**
     * 若实例或全局 axios 合并了 Content-Type: application/json，会导致缺少 boundary，
     * Spring 无法解析 multipart 并返回 415。FormData 请求必须让浏览器自行带 boundary。
     */
    transformRequest: [
      (data, headers) => {
        if (data instanceof FormData) {
          const h = headers as Record<string, unknown> & { delete?: (k: string) => void }
          if (typeof h.delete === 'function') {
            h.delete('Content-Type')
          } else {
            delete (headers as Record<string, unknown>)['Content-Type']
          }
        }
        return data
      },
    ],
  })
  if (!res.data?.ok) throw toApiError(res.data)
  return res.data.data
}

