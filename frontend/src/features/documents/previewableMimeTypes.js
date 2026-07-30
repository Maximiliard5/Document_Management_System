// Scoped to natively browser-renderable types only - no third-party viewer,
// so no document content ever leaves the backend beyond what download
// already does. Anything else keeps Download-only, no Preview button.
export const PREVIEWABLE_MIME_TYPES = new Set([
  'application/pdf',
  'image/jpeg',
  'image/png',
  'image/gif',
  'image/webp',
  'text/plain',
  'text/csv',
])
