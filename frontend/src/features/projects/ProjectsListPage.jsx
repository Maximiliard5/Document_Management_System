import { useState } from 'react'
import { useProjects } from './useProjects.js'
import { ProjectCard } from './components/ProjectCard.jsx'
import { CreateProjectModal } from './components/CreateProjectModal.jsx'
import { PageHeader } from '../../shared/components/PageHeader.jsx'
import { Button } from '../../shared/components/Button.jsx'
import { Spinner } from '../../shared/components/Spinner.jsx'
import { EmptyState } from '../../shared/components/EmptyState.jsx'
import styles from './ProjectsListPage.module.css'

export function ProjectsListPage() {
  const { data: projects, isLoading, isError, error } = useProjects()
  const [isModalOpen, setModalOpen] = useState(false)

  return (
    <div>
      <PageHeader
        title="Projects"
        description="Projects you own or are a member of."
        actions={<Button onClick={() => setModalOpen(true)}>New project</Button>}
      />

      {isLoading && <Spinner label="Loading projects" />}

      {isError && (
        <p role="alert" className={styles.error}>
          {error.message}
        </p>
      )}

      {!isLoading && !isError && projects.length === 0 && (
        <EmptyState
          title="No projects yet"
          description="Create a project to start organizing tasks and documents."
          action={<Button onClick={() => setModalOpen(true)}>New project</Button>}
        />
      )}

      {!isLoading && !isError && projects.length > 0 && (
        <div className={styles.grid}>
          {projects.map((project) => (
            <ProjectCard key={project.id} project={project} />
          ))}
        </div>
      )}

      <CreateProjectModal isOpen={isModalOpen} onClose={() => setModalOpen(false)} />
    </div>
  )
}
