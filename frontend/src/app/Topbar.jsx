import { Link, useParams } from 'react-router-dom'
import { useAuth } from '../shared/auth/useAuth.js'
import { useProject } from '../features/projects/useProjects.js'
import { Avatar } from '../shared/components/Avatar.jsx'
import { Button } from '../shared/components/Button.jsx'
import styles from './Topbar.module.css'

// Reads projectId straight from the URL rather than via a breadcrumb
// context/registration system - React Router's params are available to any
// component rendered within the matched route tree regardless of nesting
// depth, and useProject() dedupes against ProjectDetailLayout's own fetch (same
// query key, same cache), so this costs nothing extra and needed no new
// pattern. This does mean app/ (the shell) knows about the projects feature,
// but routes.jsx already does the same (imports every feature's pages) - a
// shell composing features isn't the boundary the mentor's modularization ask
// was protecting against; features reaching into each other's internals is.
// Worth a second opinion if that reasoning doesn't hold up.
//
// No dropdown here, just an inline "Log out" - a menu is only worth building
// once there's a second action to put in it, and nothing else is planned.
// Logging out doesn't need to navigate anywhere itself: ProtectedRoute (which
// wraps this whole shell) re-renders on the same auth state and redirects to
// /login on its own once isAuthenticated flips to false.
export function Topbar() {
  const { user, logout } = useAuth()
  const { projectId } = useParams()
  const { data: project } = useProject(projectId)

  return (
    <header className={styles.topbar}>
      <div className={styles.breadcrumb}>
        {project && (
          <>
            <Link to="/projects" className={styles.breadcrumbLink}>
              Projects
            </Link>
            <span className={styles.breadcrumbSeparator}>/</span>
            <span>{project.name}</span>
          </>
        )}
      </div>
      {user && (
        <div className={styles.user}>
          <Avatar firstName={user.firstName} lastName={user.lastName} size={28} />
          <span className={styles.name}>
            {user.firstName} {user.lastName}
          </span>
          <Button variant="secondary" className={styles.logout} onClick={logout}>
            Log out
          </Button>
        </div>
      )}
    </header>
  )
}
