import { useState } from 'react'
import { NavLink } from 'react-router-dom'
import { useAuth } from '../shared/auth/useAuth.js'
import { Role } from '../shared/constants/roles.js'
import { useRecentProjects } from '../features/projects/useRecentProjects.js'
import styles from './Sidebar.module.css'

// Whether to show the recent-projects quick-access list is a per-user UI
// preference, not data - unlike useRecentProjects.js's list *contents*, no
// other component needs to read or react to it, so plain useState +
// localStorage is enough (doesn't need that hook's TanStack Query treatment).
// Still scoped by user.id though, same lesson as that hook: on a shared
// machine, one account's collapse choice shouldn't visibly drive another's.
const EXPANDED_STORAGE_PREFIX = 'dms.sidebarProjectsExpanded'

function expandedStorageKey(userId) {
  return `${EXPANDED_STORAGE_PREFIX}.${userId}`
}

function readExpanded(userId) {
  if (!userId) return true
  try {
    const raw = localStorage.getItem(expandedStorageKey(userId))
    return raw === null ? true : raw === 'true'
  } catch {
    return true
  }
}

// Hand-drawn to match `public/favicon.svg` (same rounded-square + "U" bracket
// shape, hardcoded to the same hex there since a standalone favicon file can't
// read this app's CSS custom properties) - no image-generation tool available,
// and a simple geometric mark fits this app's existing icon style better than
// a raster logo would anyway.
function BrandMark() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
      <rect width="24" height="24" rx="6" fill="var(--color-primary)" />
      <path
        d="M7 6.5v7a5 5 0 0 0 10 0v-7"
        stroke="#fff"
        strokeWidth="2.6"
        strokeLinecap="round"
        fill="none"
      />
    </svg>
  )
}

// No icon library in the project (Documents deliberately avoided adding one -
// see frontend/ARCHITECTURE.md) - these are the two nav icons as tiny local
// SVG components, same no-new-dependency approach.
function ProjectsIcon() {
  return (
    <svg viewBox="0 0 20 20" width="16" height="16" fill="none" aria-hidden="true">
      <path
        d="M2.5 5.5a1 1 0 0 1 1-1h4l1.5 1.75h7.5a1 1 0 0 1 1 1V15a1 1 0 0 1-1 1h-13a1 1 0 0 1-1-1V5.5Z"
        stroke="currentColor"
        strokeWidth="1.4"
        strokeLinejoin="round"
      />
    </svg>
  )
}

function ChevronIcon() {
  return (
    <svg viewBox="0 0 20 20" width="12" height="12" fill="none" aria-hidden="true">
      <path d="M5 12.5 10 7.5l5 5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function AdminIcon() {
  return (
    <svg viewBox="0 0 20 20" width="16" height="16" fill="none" aria-hidden="true">
      <path
        d="M10 2.5 16 5v4.5c0 4.14-2.6 7.36-6 8.5-3.4-1.14-6-4.36-6-8.5V5l6-2.5Z"
        stroke="currentColor"
        strokeWidth="1.4"
        strokeLinejoin="round"
      />
    </svg>
  )
}

// Global top-level nav only (Projects / Admin). Per-project navigation
// (Board/Documents/Members) is a sub-nav owned by ProjectDetailLayout
// (Step 17), not this sidebar - it doesn't make sense outside a project.
export function Sidebar() {
  const { user } = useAuth()
  const { data: recentProjects } = useRecentProjects()
  const [isExpanded, setExpanded] = useState(() => readExpanded(user?.id))

  // Re-sync if the logged-in user changes without a full page reload (same
  // account-switch scenario that caused the recent-projects cross-account leak).
  // Adjusted during render rather than in an effect - React's documented
  // pattern for "reset state when a prop changes" - so this doesn't trigger
  // the extra cascading render an effect-based setState would (flagged by
  // eslint-plugin-react-hooks' set-state-in-effect rule elsewhere in this app,
  // see the document-preview-modal note in the "Polish pass" section above).
  const [lastUserId, setLastUserId] = useState(user?.id)
  if (user?.id !== lastUserId) {
    setLastUserId(user?.id)
    setExpanded(readExpanded(user?.id))
  }

  function toggleExpanded() {
    const next = !isExpanded
    setExpanded(next)
    if (user?.id) {
      try {
        localStorage.setItem(expandedStorageKey(user.id), String(next))
      } catch {
        // ignore - preference just won't persist
      }
    }
  }

  const hasRecentProjects = recentProjects?.length > 0

  return (
    <nav className={styles.sidebar} aria-label="Main">
      <div className={styles.brand}>
        <BrandMark />
        Utrecht
      </div>
      <div className={styles.projectsRow}>
        <NavLink
          to="/projects"
          className={({ isActive }) => (isActive ? styles.activeLink : styles.link)}
        >
          <ProjectsIcon />
          Projects
        </NavLink>
        {hasRecentProjects && (
          <button
            type="button"
            className={isExpanded ? styles.expandToggle : `${styles.expandToggle} ${styles.collapsed}`}
            onClick={toggleExpanded}
            aria-expanded={isExpanded}
            aria-controls="recent-projects-list"
            aria-label={isExpanded ? 'Hide recent projects' : 'Show recent projects'}
          >
            <ChevronIcon />
          </button>
        )}
      </div>
      {hasRecentProjects && isExpanded && (
        <ul id="recent-projects-list" className={styles.recentList} aria-label="Recently visited projects">
          {recentProjects.map((project) => (
            <li key={project.id}>
              <NavLink
                to={`/projects/${project.id}`}
                className={({ isActive }) => (isActive ? styles.activeRecentLink : styles.recentLink)}
              >
                {project.name}
              </NavLink>
            </li>
          ))}
        </ul>
      )}
      {user?.role === Role.ADMIN && (
        <NavLink
          to="/admin/users"
          className={({ isActive }) => (isActive ? styles.activeLink : styles.link)}
        >
          <AdminIcon />
          Admin
        </NavLink>
      )}
    </nav>
  )
}
