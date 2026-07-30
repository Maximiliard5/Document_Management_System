const MS_PER_HOUR = 1000 * 60 * 60
const MS_PER_DAY = MS_PER_HOUR * 24

// Backend deadline is a date only (LocalDate, "YYYY-MM-DD"), no time-of-day.
// The due moment is treated as the end of that day (23:59:59 local time) - a
// task due "today" should still show hours left until midnight, not read as
// already overdue the moment the day starts.
export function getDeadlineCountdown(deadline) {
  if (!deadline) return null

  const target = new Date(`${deadline}T23:59:59`)
  const diffMs = target.getTime() - Date.now()
  const isOverdue = diffMs < 0
  const absMs = Math.abs(diffMs)

  return {
    days: Math.floor(absMs / MS_PER_DAY),
    hours: Math.floor((absMs % MS_PER_DAY) / MS_PER_HOUR),
    isOverdue,
  }
}
