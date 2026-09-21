export type BusinessRangePreset = 'month' | 7 | 30

export function businessDateRange(preset: BusinessRangePreset, now = new Date()): [string, string] {
  const parts = new Intl.DateTimeFormat('en', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(now)
  const part = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((value) => value.type === type)!.value
  const today = `${part('year')}-${part('month')}-${part('day')}`
  if (preset === 'month') return [`${part('year')}-${part('month')}-01`, today]
  const start = new Date(`${today}T00:00:00Z`)
  start.setUTCDate(start.getUTCDate() - preset + 1)
  return [start.toISOString().slice(0, 10), today]
}
