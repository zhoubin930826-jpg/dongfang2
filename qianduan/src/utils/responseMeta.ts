export const parseApiPayload = <T = any>(value: T | string): T => {
  if (typeof value === 'string') {
    return JSON.parse(value) as T
  }
  return value
}

export const getFetchedAt = (value: any): string | undefined => {
  return value?._meta?.fetchedAt
}

export const formatFetchedAt = (value: string | undefined | null): string => {
  if (!value) return '--'

  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return value

  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
}
