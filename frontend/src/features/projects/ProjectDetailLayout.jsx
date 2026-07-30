import { NavLink, Outlet, useParams } from 'react-router-dom'
import { useProject } from './useProjects.js'
import { useRecordProjectVisit } from './useRecentProjects.js'
import { Spinner } from '../../shared/components/Spinner.jsx'
import styles from './ProjectDetailLayout.module.css'

function linkClassName({ isActive }) {
  return isActive ? styles.activeLink : styles.link
}

export function ProjectDetailLayout() {
  const { projectId } = useParams()
  const { data: project, isLoading, isError, error } = useProject(projectId)

  useRecordProjectVisit(project)

  if (isLoading) {
    return <Spinner label="Loading project" />
  }

  if (isError) {
    return (
      <p role="alert" className={styles.error}>
        {error.message}
      </p>
    )
  }

  return (
    <div>
      <nav className={styles.subnav} aria-label="Project">
        <NavLink to="board" className={linkClassName}>
          Board
        </NavLink>
        <NavLink to="documents" className={linkClassName}>
          Documents
        </NavLink>
        <NavLink to="members" className={linkClassName}>
          Members
        </NavLink>
      </nav>
      {/* Passed via outlet context so child routes (MembersSection etc.) don't
          each need to re-fetch/re-derive the same project. */}
      <Outlet context={{ project }} />
    </div>
  )
}
