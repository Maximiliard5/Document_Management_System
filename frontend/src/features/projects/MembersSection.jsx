import { useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import { useAuth } from '../../shared/auth/useAuth.js'
import { Avatar } from '../../shared/components/Avatar.jsx'
import { Badge } from '../../shared/components/Badge.jsx'
import { Button } from '../../shared/components/Button.jsx'
import { PageHeader } from '../../shared/components/PageHeader.jsx'
import { ConfirmDialog } from '../../shared/components/ConfirmDialog.jsx'
import { AddMemberModal } from './components/AddMemberModal.jsx'
import { useRemoveMember } from './useProjects.js'
import styles from './MembersSection.module.css'

export function MembersSection() {
  const { project } = useOutletContext()
  const { user } = useAuth()
  const isOwner = project.owner.id === user.id
  const removeMember = useRemoveMember(project.id)

  const [isAddOpen, setAddOpen] = useState(false)
  const [memberPendingRemoval, setMemberPendingRemoval] = useState(null)
  const [removeError, setRemoveError] = useState(null)

  // ProjectResponse.members excludes the owner (see backend/ARCHITECTURE.md) -
  // merge them into one list so owner and members render the same way, just
  // tagged differently.
  const allMembers = [
    { ...project.owner, isOwner: true },
    ...project.members.map((member) => ({ ...member, isOwner: false })),
  ]
  const existingIds = allMembers.map((member) => member.id)

  async function handleConfirmRemove() {
    if (!memberPendingRemoval) return
    setRemoveError(null)
    try {
      await removeMember.mutateAsync(memberPendingRemoval.id)
      setMemberPendingRemoval(null)
    } catch (err) {
      setRemoveError(err.message ?? 'Something went wrong. Try again.')
      setMemberPendingRemoval(null)
    }
  }

  return (
    <div>
      <PageHeader
        title="Members"
        description={`${allMembers.length} ${allMembers.length === 1 ? 'person has' : 'people have'} access to this project.`}
        actions={isOwner ? <Button onClick={() => setAddOpen(true)}>Add member</Button> : undefined}
      />

      {removeError && (
        <p role="alert" className={styles.error}>
          {removeError}
        </p>
      )}

      <ul className={styles.list}>
        {allMembers.map((member) => (
          <li key={member.id} className={styles.row}>
            <Avatar firstName={member.firstName} lastName={member.lastName} />
            <div className={styles.info}>
              <span className={styles.name}>
                {member.firstName} {member.lastName}
              </span>
              <span className={styles.email}>{member.email}</span>
            </div>
            {member.isOwner && <Badge color="var(--color-primary)">Owner</Badge>}
            {isOwner && !member.isOwner && (
              <Button
                variant="danger"
                className={styles.removeButton}
                onClick={() => setMemberPendingRemoval(member)}
              >
                Remove
              </Button>
            )}
          </li>
        ))}
      </ul>

      {isOwner && (
        <AddMemberModal
          isOpen={isAddOpen}
          onClose={() => setAddOpen(false)}
          projectId={project.id}
          excludeUserIds={existingIds}
        />
      )}

      <ConfirmDialog
        isOpen={Boolean(memberPendingRemoval)}
        title="Remove member?"
        message={
          memberPendingRemoval
            ? `${memberPendingRemoval.firstName} ${memberPendingRemoval.lastName} will lose access to this project.`
            : ''
        }
        confirmLabel="Remove"
        isDangerous
        onConfirm={handleConfirmRemove}
        onCancel={() => setMemberPendingRemoval(null)}
      />
    </div>
  )
}
