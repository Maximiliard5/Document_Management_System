# DMS Frontend

React + Vite frontend for the document/task/project management backend in `../backend`.
Plain JavaScript/JSX — no TypeScript.

## Getting started

```bash
npm install
npm run dev
```

The dev server proxies API requests to the backend (see `vite.config.js`) — start the
backend first (`docker compose up` from the repo root).

## Scripts

- `npm run dev` — start the Vite dev server with HMR
- `npm run build` — production build to `dist/`
- `npm run lint` — ESLint
- `npm run preview` — preview the production build locally

See `../backend/ARCHITECTURE.md` for the API surface this frontend consumes.
