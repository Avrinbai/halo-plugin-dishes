/** 推荐星级展示（★ / ☆），与点菜页、首页一致 */

export function clamp(n: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, n))
}

export function stars(level: number): string {
  const c = clamp(Math.round(level), 1, 5)
  return '★'.repeat(c) + '☆'.repeat(5 - c)
}

export function dishRecommendationLevel(d: { recommendation_level?: number | null }): number {
  const n = d.recommendation_level
  if (n != null && Number.isFinite(n)) {
    return clamp(Math.round(n), 1, 5)
  }
  return 3
}
