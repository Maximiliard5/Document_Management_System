import { useEffect, useState } from 'react'

// Delays reflecting `value` until it's stopped changing for `delayMs` -
// for search-as-you-type inputs where firing a request per keystroke would
// be wasteful.
export function useDebouncedValue(value, delayMs = 300) {
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const timeoutId = setTimeout(() => setDebounced(value), delayMs)
    return () => clearTimeout(timeoutId)
  }, [value, delayMs])

  return debounced
}
