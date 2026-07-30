import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../../shared/auth/useAuth.js'
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog.jsx'
import { ApiError } from '../../../shared/api/httpClient.js'
import { useDeleteProject } from '../useProjects.js'
import { EditProjectModal } from './EditProjectModal.jsx'
import styles from './ProjectCard.module.css'

// Local inline SVGs, same no-icon-library approach as Sidebar.jsx's
// ProjectsIcon/AdminIcon (frontend/ARCHITECTURE.md) - only used here, so no
// shared icon module.
function PencilIcon() {
  return (
    <svg viewBox="0 0 20 20" width="14" height="14" fill="none" aria-hidden="true">
      <path
        d="M13.5 3.5 16.5 6.5 7 16H4v-3l9.5-9.5Z"
        stroke="currentColor"
        strokeWidth="1.4"
        strokeLinejoin="round"
      />
    </svg>
  )
}

function TrashIcon() {
  return (
    <svg viewBox="0 0 20 20" width="14" height="14" fill="none" aria-hidden="true">
      <path
        d="M4 6h12M8 6V4.5h4V6M6 6l.6 9.5A1 1 0 0 0 7.6 16.5h4.8a1 1 0 0 0 1-1L14 6"
        stroke="currentColor"
        strokeWidth="1.4"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

export function ProjectCard({ project }) {
  const { user } = useAuth()
  const deleteProject = useDeleteProject()
  const [isEditOpen, setEditOpen] = useState(false)
  const [isDeleteOpen, setDeleteOpen] = useState(false)
  const [deleteError, setDeleteError] = useState(null)

  const isOwner = project.owner.id === user?.id

  // ProjectResponse.members excludes the owner (see backend/ARCHITECTURE.md) - +1 to
  // count the whole team, not just the invited members.
  const memberCount = project.members.length + 1

  const handleConfirmDelete = async () => {
    try {
      await deleteProject.mutateAsync(project.id)
      setDeleteOpen(false)
    } catch (err) {
      setDeleteError(err instanceof ApiError ? err.message : 'Something went wrong. Try again.')
    }
  }

  return (
    <div className={styles.card}>
      {isOwner && (
        <div className={styles.ownerActions}>
          <button
            type="button"
            className={styles.iconButton}
            aria-label="Edit project"
            onClick={() => setEditOpen(true)}
          >
            <PencilIcon />
          </button>
          <button
            type="button"
            className={styles.iconButton}
            aria-label="Delete project"
            onClick={() => {
              setDeleteError(null)
              setDeleteOpen(true)
            }}
          >
            <TrashIcon />
          </button>
        </div>
      )}

      <Link to={`/projects/${project.id}`} className={styles.cardLink}>
        <h3 className={styles.name}>{project.name}</h3>
        {project.description && <p className={styles.description}>{project.description}</p>}
        <p className={styles.meta}>
          {memberCount} {memberCount === 1 ? 'member' : 'members'}
        </p>
      </Link>

      {isOwner && (
        <>
          <EditProjectModal isOpen={isEditOpen} onClose={() => setEditOpen(false)} project={project} />
          <ConfirmDialog
            isOpen={isDeleteOpen}
            title="Delete project?"
            message={
              deleteError ?? `"${project.name}" will be permanently deleted. This cannot be undone.`
            }
            confirmLabel="Delete"
            isDangerous
            onConfirm={handleConfirmDelete}
            onCancel={() => setDeleteOpen(false)}
          />
        </>
      )}
    </div>
  )
}
