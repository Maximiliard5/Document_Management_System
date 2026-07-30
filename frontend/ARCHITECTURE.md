# Utrecht Frontend — Architecture & Design System

Branded "Utrecht" in the UI (a pun on YouTrack) as of the "Second round" changes below;
this doc, the repo folder, and internal identifiers still say DMS/document-management-system
throughout - only the user-facing name changed.

This documents the frontend's design decisions and structure, and the reasoning behind
them. It's kept up to date as the app is built (see task list / plan for what's actually
implemented so far). For the API this frontend consumes, see `../backend/ARCHITECTURE.md`.

## Current status

All 25 steps of the implementation plan (`~/.claude/plans/cached-finding-key.md`) are
done. Every feature works end-to-end against the real backend: auth, projects
(list/detail/members), the full Kanban board, documents (list/upload/download/delete), and
admin user management (list/change role/deactivate, `/admin/users` gated to `ADMIN` only).
This was the last step in the original plan.

A follow-up polish pass (`~/.claude/plans/velvety-crunching-cat.md`) is also done: the
register page overflow, a sidebar restyle with a per-user recent-projects list, the Kanban
drag-and-drop snap-back flash, priority-sorted columns, and a document preview modal. See
"Polish pass" below for what actually shipped and two real bugs found while building it.

A second round of improvements (`~/.claude/plans/scalable-hopping-truffle.md`) is also
done: a deadline countdown on task cards (plus the date input needed to actually set one),
owner-only edit/delete buttons on project tiles, and a collapse toggle for the sidebar's
recent-projects list. See "Second round" below - this batch needed no backend changes at
all, since `TaskEntity.deadline` and the project update/delete endpoints already existed
and were simply never wired up to any UI.

Project 2 ("Website Redesign") also has one real test document (`test-doc.txt`, uploaded
by `second.test@example.com` while verifying Step 24), left in place as a fixture.

`step13.test@example.com` is now an `ADMIN` (promoted via a one-time direct DB update
during Step 25's testing, documented below - there is no self-service or API path to
create the first admin). `second.test@example.com` remains a plain `USER`.

Project 2 ("Website Redesign") now has four real test tasks spanning all three
statuses/priorities, left in place as fixtures - see the Step 20/22 decisions below for
how they got there:
- `DMS-1` "Set up design tokens" — TODO / LOW
- `DMS-2` "Build the Kanban board" — IN_PROGRESS / LOW (edited during Step 22's testing)
- `DMS-3` "Wire up auth flow" — DONE / MEDIUM
- `DMS-4` "Ship the landing page" — DONE / MEDIUM (created directly into Done while
  testing Step 22)

Two throwaway test accounts exist in the dev database from testing, safe to keep using:
- `step13.test@example.com` / `correcthorse123` — owns the "Website Redesign" project
  (id 2), used as the primary account for walkthroughs.
- `second.test@example.com` / `correcthorse123` — a plain member of that project, used to
  test non-owner views and the add/remove-member flow.

Reminder: the backend runs in Docker and does **not** hot-reload — after any backend
source change, it needs `docker compose up -d --build` (from the repo root) to actually
pick it up. Claude doesn't have Docker permissions in this environment, so this is always
a manual step for the user.

## Tech stack

- **React 19 + Vite**, plain **JavaScript/JSX** — no TypeScript. The scaffold defaults to
  TS via `npm create vite`; it was converted to plain JS because the person building this
  hasn't learned TypeScript yet. No build-time type checking; shapes that would otherwise
  be a TS interface are documented here in prose instead.
- **Plain CSS**, one stylesheet per component (CSS Modules once components exist) — no
  Tailwind, no CSS-in-JS. Explicit choice: styling stays close to vanilla CSS rather than
  behind a utility-class or runtime abstraction.
- **react-router-dom** — client-side routing (URL → component tree, entirely in the
  browser; no server-side routes for pages, unlike `@GetMapping` on the backend).
- **@tanstack/react-query** — server-state/data-fetching. Gives every API-backed page a
  cache keyed by a query key, automatic loading/error state, and a "mutation" API that
  invalidates/refetches after a write, instead of hand-rolling that per page.
- **react-hook-form + zod** — form state and validation. `react-hook-form` avoids
  re-rendering the whole form on every keystroke (uses refs, not `useState`, for field
  values); `zod` schemas validate input client-side, mirroring what `@Valid`/Bean
  Validation already does server-side. Client validation is UX only — the backend's own
  validation remains the actual security/correctness boundary.
- **@dnd-kit/core + @dnd-kit/sortable + @dnd-kit/utilities** — Kanban drag-and-drop.
  `react-beautiful-dnd` was deliberately avoided; it's unmaintained.
- **@fontsource/ibm-plex-sans + @fontsource/ibm-plex-mono** — self-hosted font files
  (no runtime call to Google Fonts or any other font CDN).

## Design system

Reference point: **Height** (calm neutral surfaces, restrained single accent, soft
elevation), adapted for a corporate document/task tool — not Jira's density, not Linear's
near-monochrome starkness. No gradients. Deliberately avoiding the current "AI-generated
web design" defaults: no cream-background+serif hero, no near-black+neon accent, no
broadsheet hairline-rule layout.

### Palette

Defined once as CSS custom properties in `src/shared/styles/tokens.css`:

| Token | Hex | Use |
|---|---|---|
| `--color-canvas` | `#F5F6F8` | App background |
| `--color-surface` | `#FFFFFF` | Cards, panels, modals |
| `--color-border` | `#E2E4E9` | Hairlines, dividers |
| `--color-ink` | `#14171C` | Primary text |
| `--color-ink-muted` | `#5B6270` | Secondary text, captions |
| `--color-primary` | `#0F6B5C` | Brand accent — primary buttons, active nav, links |
| `--color-primary-hover` | `#0C5A4D` | Primary hover/active state |
| `--priority-high` | `#C1443B` | Task priority = HIGH |
| `--priority-medium` | `#B8860B` | Task priority = MEDIUM |
| `--priority-low` | `#4C7A5E` | Task priority = LOW |
| `--status-todo` | `#8A8F98` | Task status = TODO |
| `--status-in-progress` | `#2563A8` | Task status = IN_PROGRESS |
| `--status-done` | `#2F8558` | Task status = DONE |

These aren't decorative — priority/status colors are the *only* color-coding in the app,
so they carry real meaning and nothing else competes with them for attention.

### Typography

- **IBM Plex Sans** for all UI text — headings and body, one family, weight/size scale
  does the work. This is an app shell, not an editorial page, so a second display face
  would just be noise; a distinct display+body pairing is a decision for content-led
  pages, not a dense internal tool.
- **IBM Plex Mono** for anything data-shaped: file sizes, timestamps. (Task cards used to
  carry a monospace `DMS-142` key badge too - dropped in the "Second round" changes below,
  since surfacing a raw sequential database id to end users isn't something to do by
  default without a reason to.)
- Both self-hosted via `@fontsource`; only the weights actually used are imported
  (sans 400/500/600/700, mono 400/500) rather than every weight the package ships.

### Layout

Fixed left sidebar (global nav only: Projects, Admin if the user is an admin), a top bar
(breadcrumb slot + user info), content area. Corners: moderate `6–8px` radius throughout
(`--radius` / `--radius-sm` tokens) — not maximally rounded, not sharp.

Per-project navigation (Board / Documents / Members) is **not** part of the global
sidebar — it doesn't mean anything outside a specific project, so it's a sub-nav owned by
`ProjectDetailLayout` (Step 17) instead, scoped to `/projects/:id/*`. (Earlier drafts of
this doc had it in the sidebar and also mentioned a topbar search box and a "Settings" nav
item - both dropped, since no step in the plan actually scopes a settings page or search
feature; corrected once the shell was actually built, Step 10.)

### Signature element

Every task card carries a 3px left-edge bar in its priority color, reused identically on
the board and in the drag overlay — the one recognizable device the app is built around;
everything else stays quiet. (This section originally also cited a monospace task-key
badge as part of the same device; that badge was removed - see "Second round" below.)

## Frontend architecture

Feature-based modules — module boundaries are the domain areas (auth, projects, tasks,
documents, users), not technical layers. This was an explicit ask from the person
building this app's mentor: modularize to keep the code clean and debuggable as it
grows, rather than organizing purely by technical layer (all API calls together, all
components together, etc.) the way the backend does.

```
frontend/src/
  app/              # routes.jsx, App.jsx (shell), providers.jsx
  features/
    auth/           # LoginPage, RegisterPage, authApi, useAuth
    projects/       # list/detail pages, projectsApi, hooks, components/
    tasks/          # Kanban board page, tasksApi, hooks, components/
    documents/      # documents page, documentsApi, hooks, components/
    users/          # admin user list + role/deactivate, usersApi
  shared/
    components/     # Button, Modal, Avatar, Badge, EmptyState, Spinner, ConfirmDialog, PageHeader
    api/            # httpClient.js (fetch wrapper), auth header injection, error normalization
    auth/           # AuthContext, ProtectedRoute
    styles/         # tokens.css, reset.css, global.css
    constants/      # plain-JS enum objects mirroring backend values (TaskStatus, TaskPriority)
    utils/          # formatDate, formatFileSize, etc.
```

## Dev environment: the CORS problem and why a proxy fixes it

The backend has no CORS configuration at all (confirmed in `backend/ARCHITECTURE.md`).
Once the frontend calls the API from the browser, `localhost:5173` (Vite) and
`localhost:8080` (Spring Boot) count as different origins, and the browser would block
reading the response — worse once requests carry an `Authorization` header, which
triggers a CORS preflight the backend doesn't handle either.

Fix: `vite.config.js` proxies `/auth`, `/users`, `/projects` (the three top-level path
prefixes the backend owns — `/projects` also covers nested `/projects/{id}/tasks` and
`/projects/{id}/documents`) to `localhost:8080`. The browser only ever talks to
`localhost:5173`; Vite forwards matching requests to the backend server-side, where
same-origin policy doesn't apply. This is a dev-only convenience — a real deployment
would need its own answer to this (proper CORS config, or serving both from one origin),
which is out of scope for now.

**Gotcha found while testing Step 14**: the frontend's own client-side route `/projects`
(`ProjectsListPage`) collides with the backend's `/projects` prefix. A full browser
navigation to `/projects` (typing the URL, a hard refresh) sent a plain `GET` that the
proxy forwarded straight to the backend instead of letting Vite serve the SPA shell,
which then got rejected since it carried no `Authorization` header. Fixed with a `bypass`
function on each proxy entry (Vite's own documented fix for this): browser navigations
send `Accept: text/html`; in-app `fetch()` calls don't, so that's what distinguishes them.

## Decisions made along the way (not just defaults)

- **No TypeScript** — plain JS/JSX throughout; see Tech stack above.
- **Plain CSS, not Tailwind/CSS-in-JS** — explicit preference, kept the styling
  approach close to vanilla CSS.
- **Kanban board, not a simple list** — chosen deliberately to match the Jira/Height-like
  reference, at the cost of needing a drag-and-drop library.
- **`GET /users/search` added to the backend** — `GET /users` (needed to look up someone
  to invite to a project) was ADMIN-only; a regular project owner had no way to find a
  user's ID. Added a narrow, non-admin endpoint instead of working around it client-side.
- **Fixed a real backend bug while in there**: manual authorization checks
  (`checkOwner`/`checkMemberOrOwner`) were throwing an exception type with no registered
  handler, so "you're not a member of this project" was surfacing as HTTP 500 instead of
  403. Fixed in `GlobalExceptionHandler` (see `backend/ARCHITECTURE.md` section 7).
- **Fixed a proxy/routing collision found while testing Step 14**: the frontend's own
  `/projects` route collided with the backend's `/projects` API prefix in the Vite dev
  proxy, so a hard browser navigation to `/projects` got forwarded to the backend instead
  of Vite serving the SPA. Fixed with a `bypass` function on each proxy entry in
  `vite.config.js` (Vite's own documented fix for this).
- **Topbar's breadcrumb reads `useParams()`/`useProject()` directly** (Step 17), rather
  than a dedicated breadcrumb context that `ProjectDetailLayout` would push into. This
  means `app/Topbar.jsx` (the shell) imports a hook from `features/projects/` - a
  deliberate call that this is the same kind of dependency `app/routes.jsx` already has on
  every feature's page components (the shell composing features), not the kind of
  boundary the mentor's modularization ask was protecting against (features reaching into
  each other's internals). `useProject()` dedupes against `ProjectDetailLayout`'s own
  fetch via the same TanStack Query key, so this costs no extra request - confirmed by
  checking the network log directly (one `GET /projects/{id}`, not two).
- **Two real bugs found while testing Step 18 (`MembersSection`)**, both worth remembering
  for Steps 19-25's own query hooks (tasks/documents/users will build more of these):
  - **`Modal`'s focus effect was keyed on `[isOpen, onClose]`**, and called
    `panelRef.current?.focus()` every time it ran. Any parent passing an inline/unmemoized
    `onClose` (a new function reference every render) caused the effect to re-run on every
    render of that parent - and typing into a *controlled* input inside the modal
    re-renders the parent on every keystroke, so focus got stolen from the input back to
    the modal panel after the very first character. Split into two effects: one keyed only
    on `isOpen` (focuses once, on the actual open transition), one keyed on
    `[isOpen, onClose]` for the Escape-key listener (re-subscribing that one on every
    render is harmless). Didn't show up in Step 16's `CreateProjectModal` since
    `react-hook-form`'s uncontrolled inputs don't re-render the parent per keystroke -
    `AddMemberModal`'s live-search box was the first controlled input inside a `Modal`.
  - **`projectKeys.detail(projectId)` silently failed to invalidate** when called with a
    numeric `project.id` (from a fetched `ProjectResponse`) while the live query was
    registered with the string `projectId` from `useParams()` (URL params are always
    strings). TanStack Query matches keys by structural equality *including type*, so
    `['projects','detail',2]` and `['projects','detail','2']` are different keys - the
    add/remove-member mutations appeared to succeed (network tab showed 200s) but the UI
    never updated without a full reload, no error anywhere. Fixed by coercing to `String()`
    inside the key factory itself, so every call site is correct regardless of which type
    it happens to pass.
- **Shell content area caused whole-page horizontal scroll, found testing Step 20's
  `TaskBoardPage`**: this was the app's first genuinely wide content (a row of Kanban
  columns wider than the viewport). CSS grid items get an implicit `min-width: auto` by
  default, so `App.module.css`'s `.content` grid cell (no explicit `min-width`) grew to fit
  its child instead of clipping it - the *whole* shell grid scrolled horizontally,
  including the fixed sidebar, rather than just the board. Fixed by adding
  `min-width: 0; overflow: auto` to `.content`. Same class every page's content renders
  into, so this was a real, previously-latent shell bug, not something scoped to tasks.
- **Drag-and-drop split into three small pieces (Step 21)**: `TaskCard` stayed
  presentation-only; a new `DraggableTaskCard` wraps it with `useDraggable` (kept separate
  so `TaskCard` stays reusable without dnd-kit attached, e.g. for Step 22's form preview);
  `TaskColumn` itself became the drop target via `useDroppable`, since columns - not
  individual cards - are what a dragged card can be dropped *onto*. `TaskBoardPage` owns
  the single `DndContext`, tracks the actively-dragged task in state for `DragOverlay`, and
  on drop only fires the update mutation if the task's status actually changed (dropping
  back in the same column, or outside any column, is a no-op - confirmed via the network
  tab that no `PUT` fires in that case). `PointerSensor`'s
  `activationConstraint: { distance: 8 }` distinguishes a drag from a future plain click on
  a card (Step 22 will open the edit modal on click). Verified end-to-end against the real
  backend: dragging a card to a different column persists after a full page reload,
  confirmed via the network tab (`PUT /projects/2/tasks/1` → 200).
- **`TaskFormModal` (Step 22) creates directly into any column, not just TODO**: the
  backend's `CreateTaskRequest` has no `status` field (a new task is always server-side
  `TODO` - see backend/ARCHITECTURE.md), but each `TaskColumn` has its own "+ Add task"
  affordance and "status set implicitly by column on create" was the whole point per the
  plan. Reconciled by having the modal, on create, call `createTask` and then - only if the
  clicked column wasn't TODO - immediately follow with an `updateTask` status change on the
  newly-created id. Verified via the network tab: clicking "+ Add task" in the Done column
  produced `POST /projects/2/tasks` (201) followed by `PUT /projects/2/tasks/4` (200), and
  the card appeared directly in Done, no visible TODO flash.
- **`FormField` extended with an `as` prop** (`'input' | 'textarea' | 'select'`) rather than
  writing a one-off styled `<textarea>`/`<select>` for this form - same label/error layout,
  same `register()`-compatible ref forwarding, reusable for the next form that needs a
  multi-line or enum field (e.g. Step 25's role select).
- **`TaskCard` gained an optional `onClick`** (role="button", keyboard-activatable) rather
  than a separate "click wrapper" component - clicking a card opens `TaskFormModal` in edit
  mode. Works alongside dragging without extra logic: dnd-kit's `PointerSensor`
  `activationConstraint: { distance: 8 }` (already in place from Step 21) only starts a
  drag once the pointer has moved 8px, so a plain click-and-release still fires the
  browser's native click event on the same element.
- **`httpClient.js` generalized for Step 23 instead of a documents-specific escape hatch**:
  upload is `multipart/form-data`, and download returns a raw binary stream, neither of
  which fit the existing JSON-only `request()`. Rather than writing one-off fetch calls in
  `documentsApi.js`, `request()` itself now special-cases a `FormData` body (skips
  `JSON.stringify` and lets the browser set its own `Content-Type` with the multipart
  boundary - manually setting `multipart/form-data` yourself omits the boundary and breaks
  the request), and a new `httpClient.getBlob()` handles the download path (auth header +
  the same error normalization, but resolves to a `Blob` instead of parsing JSON). Both
  changes live in the one shared client file everything already goes through, not duplicated
  per feature. `downloadDocument()` deliberately returns just the `Blob` - the caller
  already has the display name from `DocumentResponse.name` (the list response), so there's
  no need to parse the backend's unescaped `Content-Disposition` header (see
  backend/ARCHITECTURE.md section 10) to name the saved file.
- **No `useDownloadDocument` query hook** - unlike list/upload/delete, a download doesn't
  mutate anything the cache tracks, so `documentsApi.downloadDocument()` is called directly
  from the page (Step 24), not wrapped in a TanStack Query hook that would have no cache to
  invalidate.
- **Verified the new multipart/blob contract directly against the backend via `curl`
  (upload, list, download, delete)** before building `DocumentsPage` in Step 24 - all
  matched `backend/ARCHITECTURE.md` section 8 exactly (201 with the expected
  `DocumentResponse` shape, download headers, 204 on delete). Full page-level browser
  verification (an actual `<input type="file">`, a real download click) is Step 24's job,
  once a consuming page exists - same split as Steps 15/19 (API + hooks verified by
  contract, UI verified once the page exists).
- **`DocumentsPage` (Step 24) resolves `DocumentResponse.ownerId` against the project's own
  `owner`/`members`** (already loaded by `ProjectDetailLayout`, same data `MembersSection`
  uses) rather than a separate lookup request - `ownerId` is a bare `Long` with no nested
  user object (a documented gap, see backend/ARCHITECTURE.md section 10), but the project
  detail response already has every user who could possibly be the uploader.
- **File type shown as a small text badge (e.g. "PDF", "DOCX"), not an icon** - deliberately
  no icon library was added; `shared/components/Badge` already exists and a short mono-ish
  label reads clearly in a table row. Kept neutral/uncolored, consistent with the design
  system's rule that priority/status colors are the only real color-coding in the app (see
  Design system above) - a file-type badge isn't semantic in the same way, so it doesn't get
  a color.
- **New `shared/utils/formatFileSize.js` and `formatDate.js`** - the plan's folder layout
  already reserved `shared/utils/` for exactly this (see Frontend architecture above); this
  is the first page that needed to format either.
- **Upload's real-backend validation was verified, not assumed**: uploading the same
  filename twice surfaced the backend's actual 409 message ("A document named 'test-doc.txt'
  already exists in this project") in the page's error banner, confirming
  `ApiError`/`setActionError` propagate the real `GlobalExceptionHandler` message rather
  than a generic fallback. Verified end-to-end in the browser: upload, download (network tab
  showed the correct blob response), delete, and the duplicate-name 409 all work against the
  real backend and survive a full page reload.
- **New `AdminRoute` guard (Step 25)**, nested inside `ProtectedRoute` in `routes.jsx`
  around just the `/admin/users` route: redirects to `/projects` if `user.role !== ADMIN`
  rather than showing a 403 page (none exists) - mirrors the backend's own approach of
  simply not exposing these three endpoints to a non-admin, no dedicated error state either
  side. `Sidebar`'s conditional "Admin" link (already written in Step 10) needed no changes.
- **No self-service or API path exists to create the first admin** - `AuthService.register`
  hardcodes `Role.USER`, and promoting to `ADMIN` requires an existing admin to call
  `PUT /users/{id}/role` (see backend/ARCHITECTURE.md section 6). With zero admins in the
  dev database, this is a real chicken-and-egg gap, not a frontend concern - bootstrapped
  by promoting `step13.test@example.com` directly in Postgres (`UPDATE users SET role =
  'ADMIN' WHERE email = '...'`, piped via stdin to `psql` inside the `dms-postgres`
  container - a one-time manual step the user ran, same shape as the Docker-rebuild
  reminder elsewhere in this doc). A real deployment would seed its first admin some other
  way (a migration, a setup script) - out of scope here.
- **Role changes via a plain native `<select>` per row, not `FormField`/react-hook-form**:
  each row commits instantly on change (no submit step), so there's no form state or
  validation to manage - `FormField` is for actual forms (Task/Project/Register), this is
  closer to `MembersSection`'s instant remove-member action.
- **Verified all three admin endpoints against the real backend, including the
  last-active-admin guard**: with only one admin (`step13.test@example.com`) in the
  database, clicking "Deactivate" on that account surfaced the backend's real 409 -
  "Cannot deactivate the last active admin." - directly in the page's error banner, and the
  account correctly stayed Active. Promoted `second.test@example.com` to `ADMIN` (200,
  table updated), then demoted it back to `USER` to leave the dev database as found. Also
  confirmed the negative case: logged in as a plain `USER` and navigated straight to
  `/admin/users` by URL - `AdminRoute` redirected to `/projects` before the page ever
  rendered.

## Polish pass (register overflow, sidebar, board DnD, document preview)

Four-item follow-up pass after the 25-step build (plan at
`~/.claude/plans/velvety-crunching-cat.md`), done as one batch rather than numbered steps.

- **Register page overflow fixed the same way as Step 20's board-overflow bug**: CSS grid
  items get an implicit `min-width: auto`, so `RegisterPage.module.css`'s `.row` (a
  `grid-template-columns: 1fr 1fr` wrapping the First/Last name `FormField`s) let the Last
  name field grow past the card's edge. Fixed with `.row > * { min-width: 0 }`, plus
  `width: 100%` added to `FormField.module.css`'s `.input` so it explicitly fills its
  container (no visible change to any single-column usage - only matters once two fields
  share a row, which today is only this `.row`).
- **Sidebar restyle**: two tiny local inline SVG icon components added directly in
  `Sidebar.jsx` (no icon library, same as Documents' file-type badges) next to
  Projects/Admin, plus tighter spacing in `Sidebar.module.css`.
- **Recent-projects quick-access, and a real cross-account data leak found and fixed while
  building it**: `frontend/src/features/projects/useRecentProjects.js` tracks up to 5
  most-recently-visited projects client-side (no backend last-accessed tracking exists),
  recorded via `useRecordProjectVisit(project)` called from `ProjectDetailLayout` in an
  effect keyed on the visited project, and read via `useRecentProjects()` for the sidebar's
  indented sub-list under "Projects". Backed by TanStack Query + `localStorage`, same as
  the rest of the app's state.
  **The bug**: the first version stored everything under one shared `localStorage` key
  (`dms.recentProjects`) and one shared query key (`['recentProjects']`), with no per-user
  scoping. On a machine used to test with multiple accounts - exactly how this project gets
  tested, two throwaway accounts in the same browser - logging in as a second user still
  showed the first user's recently-visited project names and links in the sidebar. The
  backend correctly rejected navigating into a project the second user has no access to
  ("Access denied"), so this wasn't an actual data-access break, but project names/existence
  had already leaked into the UI, and it persisted across full page reloads and new tabs
  (unlike the rest of the app's in-memory TanStack Query cache, which resets on reload) -
  strictly worse than a same-tab-only leak.
  **The fix**: both the `localStorage` key and the TanStack Query key are now scoped by
  `user.id` (`dms.recentProjects.<userId>` / `['recentProjects', userId]`). Switching
  accounts - even without a page reload - lands on a structurally different cache entry and
  a different storage key, not a shared one that gets overwritten; `useRecentProjects` is
  also `enabled: Boolean(user)` so there's nothing to show while logged out. The old
  unscoped key is deleted on load (module-level side effect in `useRecentProjects.js`) since
  it already mixed history across accounts and there was nothing safe to migrate out of it.
  **Lesson for any future client-side-persisted feature on this app**: `localStorage` (and
  any query key backed by it) must be scoped per-user from the start - this app is routinely
  tested with multiple accounts in one browser, so an unscoped key is a cross-account leak
  waiting to happen, not just a theoretical edge case.
- **Kanban drag-and-drop snap-back flash - two attempts, only the second one actually
  fixed it**: `handleDragEnd` clears `activeTask` (removing the `DragOverlay` ghost)
  synchronously, but the underlying `tasks` array didn't reflect the new status until the
  `PUT` resolved and the query refetched, so the dropped card visually snapped back to its
  old column before jumping to the new one a moment later.
  The first fix attempt added `onMutate`/`onError` to `useUpdateTask` (the textbook
  TanStack Query optimistic-update pattern) to write the new status into the cache
  immediately. This did **not** fix the flash: TanStack Query always defers `onMutate` by
  at least a microtask relative to the synchronous `.mutate()` call, so React still rendered
  one frame with the overlay gone but the task list unchanged before the optimistic write
  landed - same visible bug, confirmed by testing in the browser after shipping the
  "textbook" fix.
  **The actual fix**: moved the optimistic write out of `onMutate` entirely, into a plain
  synchronous function `optimisticallyUpdateTask(queryClient, projectId, taskId, data)`
  (`useTasks.js`), called directly inside `TaskBoardPage`'s `handleDragEnd` in the same
  synchronous handler as `setActiveTask(null)`. Both state updates now happen in the same
  tick, so React 18/19's automatic batching folds them into a single render - there's no
  intermediate frame with stale data to snap back to. `useUpdateTask` itself went back to
  a plain mutation (`onSuccess` invalidate only); rollback on a failed `PUT` uses
  `rollbackTaskLists(queryClient, previousLists)` with a snapshot taken synchronously
  alongside the optimistic write, passed through `mutate`'s per-call `onError` rather than
  the hook-level one. Verified end-to-end: dragging a card lands immediately with no visual
  snap-back, and the move persists after a full page reload.
- **Priority sort**: `TASK_PRIORITY_ORDER = [HIGH, MEDIUM, LOW]` added next to the existing
  `TaskPriority` enum object in `taskPriority.js`; `TaskBoardPage` chains a stable `.sort()`
  onto each column's `.filter()`. No due-date field exists on tasks, so priority is the
  only "urgency" signal available to sort by.
- **Document preview, scoped to natively browser-renderable types only** (PDF, images,
  `text/plain`/`text/csv`) - deliberately no third-party viewer, so no document content
  ever leaves the backend beyond what download already does. New
  `features/documents/components/DocumentPreviewModal.jsx` reuses `downloadDocument()`
  (already returns a real `Blob` with the backend-detected MIME type) and
  `URL.createObjectURL`; the allow-list lives in its own `previewableMimeTypes.js` file
  (not co-located in the modal component) because ESLint's `react-refresh/only-export
  -components` rule flags a file that exports both a component and a plain constant.
  `DocumentsPage` renders a "Preview" button next to Download only when
  `PREVIEWABLE_MIME_TYPES.has(doc.type)`.
  The modal is remounted per document (`key={previewDocument?.id ?? 'closed'}` from
  `DocumentsPage`) rather than reset via an effect-body `setState('loading')` call -
  ESLint's `react-hooks/set-state-in-effect` rule (from `eslint-plugin-react-hooks` 7.x,
  the React Compiler team's stricter rule set) flags synchronous `setState` at the top of
  an effect body as an avoidable extra render. The initial `'loading'` state is instead
  computed once via a lazy `useState` initializer at mount, and the effect only calls
  `setState` from inside the `.then`/`.catch` callbacks of the download, which the rule
  doesn't flag.
  `Modal` (`shared/components/Modal.jsx`) gained an optional `size="large"` prop
  (`max-width: 800px` vs. the default 480px) since the default panel was too narrow for a
  real image preview - backward compatible, every existing caller is unaffected.
  **PDFs were dropped from the modal after a first pass looked bad in real use**: an
  `<iframe>` rendering the browser's native PDF viewer inside even the enlarged 800px modal
  still boxed in the PDF viewer's own toolbar/zoom/page-nav controls, and felt cramped for
  anything but a single small page. Rather than adding a PDF-rendering library (`pdfjs-dist`
  et al. - real bundle weight, and against the project's own no-third-party-viewer stance),
  `DocumentsPage`'s `handlePreview` now opens PDFs in a new browser tab instead of the
  modal: `window.open('', '_blank')` is called synchronously in the click handler (calling
  it only after the `downloadDocument()` await resolves loses the user-gesture context and
  gets blocked as a popup in most browsers), then once the blob resolves the new tab is
  redirected to the object URL via `previewTab.location.href`. The object URL is revoked on
  a 60-second timeout rather than immediately, since - unlike the modal's preview URLs,
  which unmount and clean up with the component - this tab stays open independently of
  `DocumentsPage`'s lifecycle. `DocumentPreviewModal` itself only ever renders images and
  text/csv now; the PDF branch was removed rather than left as dead code.
- **Task cards show their description inline** (`TaskCard.jsx`): previously a task's
  description was only visible after opening the edit modal, which read as clunky for
  something users check constantly while scanning the board. Now rendered directly under
  the title, clamped to 3 lines via `-webkit-line-clamp` with a native `title` attribute
  tooltip for the full text on hover - kept a plain CSS clamp rather than a "show more"
  toggle or a separate expanded state, since a Kanban card is meant to stay compact and the
  full text is one click away in the edit modal regardless.

## Second round (deadline countdown, project edit/delete, sidebar collapse toggle)

Three-item follow-up (plan at `~/.claude/plans/scalable-hopping-truffle.md`). Unlike the
prior two passes, this one turned out to need almost no new plumbing - the backend already
had everything, and even some frontend hooks already existed but were never wired to a UI.

- **Task deadline countdown**: `TaskEntity.deadline` (`LocalDate`) and its DTOs were already
  end-to-end on the backend, and `tasksApi.js`/`useTasks.js` already threaded a `deadline`
  field through every request - but no task form actually exposed a way to set one, so no
  task ever had one. Added a `type="date"` `FormField` to `TaskFormModal.jsx`; the native
  date input yields `''` when cleared, which is normalized to `null` right before the
  `createTask`/`updateTask` call (the backend's `LocalDate` deserializer rejects an empty
  string, only `null` or a real date).
  New `shared/utils/deadlineCountdown.js` computes `{ days, hours, isOverdue }` against the
  **end of the deadline day (23:59:59 local time)**, not midnight - a task due "today" reads
  as hours-remaining, not instantly overdue. Computed at render time only, no `setInterval`
  ticker - this isn't a live dashboard, and a countdown accurate as of the last render/query
  refetch is enough. `TaskCard.jsx` renders it as a `Badge` (reusing
  `shared/components/Badge.jsx`, same component priority badges use elsewhere) only when
  `task.deadline` is set, colored via `--priority-high` when overdue and left neutral
  otherwise.
- **Project owner edit/delete buttons**: `projectsApi.js`/`useProjects.js` already had
  `updateProject`/`deleteProject` and their hooks (`useUpdateProject`, `useDeleteProject`)
  fully wired to the right query invalidations - written during the original build for a
  UI that was never built. `ProjectCard.jsx` (the `/projects` grid tile) had to be
  restructured to add them: previously the entire card *was* a `<Link>`, and a `<button>`
  can't nest inside an `<a>`. Now the outer `.card` is a plain `position: relative` `<div>`,
  the title/description/member-count are wrapped in an unstyled `.cardLink` `<Link>` inside
  it, and the pencil/trash buttons are absolutely-positioned top-right *siblings* of that
  Link rather than nested inside it - no `stopPropagation` workaround needed, since they're
  never inside the clickable element to begin with. Because the hover/focus border-and-shadow
  feedback used to live on the `<Link>` itself, `.card:has(.cardLink:hover)` /
  `:has(.cardLink:focus-visible)` reproduce the same whole-tile feedback from the outer div
  (`:has()` support is already assumed elsewhere in this app, e.g. Badge.jsx's `color-mix`).
  Visibility is gated on `project.owner.id === user.id` (`useAuth()`), matching the same
  ownership-check idiom used in `MembersSection.jsx`. New `EditProjectModal.jsx` mirrors
  `CreateProjectModal.jsx`'s form almost exactly, just pre-filled via `reset()` on open (the
  same pattern `TaskFormModal.jsx` already uses for its edit mode) and pointed at
  `useUpdateProject` instead of `useCreateProject`. Delete reuses the existing shared
  `ConfirmDialog` (same pattern as `DocumentsPage.jsx`'s document-delete flow) rather than
  introducing a new confirmation UI.
- **Sidebar recent-projects collapse toggle**: a `^` chevron button next to "Projects" in
  `Sidebar.jsx` shows/hides the recent-projects quick-access list added in the polish pass,
  for anyone who'd rather not see it. Deliberately **not** built on `useRecentProjects.js`'s
  TanStack Query treatment - that hook needs to be shared, reactive state (multiple things
  read the list and need to agree on it), whereas whether the list is shown at all is a
  private, purely-local UI preference that nothing else reads. Plain `useState` seeded
  lazily from `localStorage`, keyed `dms.sidebarProjectsExpanded.<userId>` - same per-user
  scoping lesson as `useRecentProjects.js`'s cross-account leak above, even though the
  consequence of getting this one wrong would be far milder (a stale UI preference, not
  leaked project names/existence). Re-reads on `user?.id` changing, so switching accounts in
  the same tab without a reload can't leave one account's toggle state visibly driving
  another's. Defaults to expanded (`true`) when nothing is stored yet, matching the existing
  behavior for everyone until they actively collapse it.

**Four more follow-ups after user feedback on the Second round itself:**

- **Renamed "DMS" to "Utrecht"** (a pun on YouTrack) in every user-facing spot: the
  `<title>` in `index.html`, `Sidebar.jsx`'s brand row, and the `LoginPage.jsx`/
  `RegisterPage.jsx` card headings. Cosmetic only - the repo folder, this doc's own
  filename, and internal identifiers (DTOs, routes, `dms.*` localStorage keys) are
  unaffected; renaming those would be a much bigger, unrelated change than a brand label.
- **Brand mark / favicon**: no image-generation tool is available, so this is a small
  hand-authored SVG (same technique as `ProjectsIcon`/`AdminIcon` in `Sidebar.jsx`) rather
  than a raster logo - a rounded-square tile in `--color-primary` with a white "U" bracket
  glyph. Used two ways: inline as `BrandMark()` next to the "Utrecht" wordmark in
  `Sidebar.jsx` (reads `var(--color-primary)`, since it's live DOM and inherits the app's
  CSS), and as `public/favicon.svg` with the same color hardcoded as a literal hex - a
  standalone favicon file is loaded directly by the browser, outside the page's CSSOM, so
  it can't reference this app's custom properties.
- **Task cards dropped the `DMS-142` key badge**: showing a task's raw, sequential
  database id to any user who can see the board isn't something to do by default without
  a reason to - removed from `TaskCard.jsx` along with the now-unused `.key` CSS class.
  The "Design system" section above (Typography, Signature element) is corrected to match.
- **Task assignment** — this was fully wired on the backend and through `tasksApi.js`
  already (see the backend feature audit that prompted this), just never exposed in any
  form. `TaskFormModal.jsx` gained an "Assignee" `<select>` (same create-and-edit modal, so
  this works both when creating a task and editing one), and `TaskCard.jsx` now shows the
  assignee's `Avatar` plus their full name in a footer row next to the deadline badge.
  Deliberately scoped to the project's own members (`project.owner` + `project.members`,
  the same list `MembersSection.jsx` already merges) rather than a site-wide user search
  like `AddMemberModal.jsx` uses for adding project members - the backend itself permits
  assigning to any user, member or not (see `TaskEntity`'s own doc comment), but assigning
  a task to someone with no access to the board at all would be confusing, and this way
  needs no new search UI or API call, since the member list is already loaded on this page.
  `TaskFormModal` now takes a `project` prop (not just `projectId`) to get at that list;
  `TaskBoardPage.jsx` is its only call site and already had `project` from outlet context.
