
export function resolveMediaUrl(raw: string | null | undefined): string {
  if (raw == null) return ''
  const s = String(raw).trim()
  if (!s) return ''
  if (/^https?:\/\//i.test(s)) return s
  if (s.startsWith('//')) return s

  const origin = (import.meta.env.VITE_MEDIA_ORIGIN ?? '').trim().replace(/\/+$/, '')
  if (!origin) return s
  if (s.startsWith('/upload/')) {
    return `${origin}${s}`
  }
  return s
}
