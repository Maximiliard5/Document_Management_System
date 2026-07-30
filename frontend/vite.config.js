import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const BACKEND_URL = 'http://localhost:8080'

// The frontend also has a client-side route at /projects (ProjectsListPage),
// which collides with the backend's /projects prefix below. Without `bypass`,
// a full browser navigation to /projects (typing the URL, a hard refresh) - as
// opposed to an in-app fetch() call - would get forwarded to the backend
// instead of Vite serving the SPA shell, since the proxy matches on path alone
// and can't otherwise tell the two apart. Browser navigations send
// `Accept: text/html`; our fetch calls don't. This is Vite's own documented
// fix for exactly this case: https://vite.dev/config/server-options.html#server-proxy
function bypassBrowserNavigation(req) {
  if (req.headers.accept?.includes('html')) {
    return '/index.html'
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Backend routes are mounted at these three prefixes (see backend/ARCHITECTURE.md).
      // Proxying them here means the browser only ever talks to this dev server
      // (localhost:5173), so there's no cross-origin request for it to block - the
      // backend currently has no CORS configuration at all, so a direct fetch to
      // localhost:8080 from a page served on localhost:5173 would fail in the browser.
      '/auth': { target: BACKEND_URL, changeOrigin: true, bypass: bypassBrowserNavigation },
      '/users': { target: BACKEND_URL, changeOrigin: true, bypass: bypassBrowserNavigation },
      '/projects': { target: BACKEND_URL, changeOrigin: true, bypass: bypassBrowserNavigation },
    },
  },
})
