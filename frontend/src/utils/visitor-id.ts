const KEY = 'vote.visitor-id.v1'

export function getVisitorId(): string {
  try {
    const previous = localStorage.getItem(KEY)
    if (previous) return previous
    const value = crypto.randomUUID()
    localStorage.setItem(KEY, value)
    return value
  } catch {
    throw new Error('浏览器本地存储不可用，无法保证投票状态。请允许本站使用本地存储后重试。')
  }
}
