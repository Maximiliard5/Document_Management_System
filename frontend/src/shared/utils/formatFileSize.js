const UNITS = ['B', 'KB', 'MB', 'GB']

export function formatFileSize(bytes) {
  if (bytes < 1024) return `${bytes} B`

  let value = bytes
  let unitIndex = 0
  while (value >= 1024 && unitIndex < UNITS.length - 1) {
    value /= 1024
    unitIndex += 1
  }
  return `${value.toFixed(1)} ${UNITS[unitIndex]}`
}
