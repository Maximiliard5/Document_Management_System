import { useCallback, useState } from 'react'
import { Modal } from '../../../shared/components/Modal.jsx'
import { Avatar } from '../../../shared/components/Avatar.jsx'
import { Button } from '../../../shared/components/Button.jsx'
import { FormField } from '../../../shared/components/FormField.jsx'
import { Spinner } from '../../../shared/components/Spinner.jsx'
import { useDebouncedValue } from '../../../shared/hooks/useDebouncedValue.js'
import { useUserSearch } from '../../users/useUsers.js'
import { useAddMember } from '../useProjects.js'
import styles from './AddMemberModal.module.css'

export function AddMemberModal({ isOpen, onClose, projectId, excludeUserIds }) {
  const [query, setQuery] = useState('')
  const debouncedQuery = useDebouncedValue(query, 300)
  const { data: results, isFetching } = useUserSearch(debouncedQuery)
  const addMember = useAddMember(projectId)
  const [error, setError] = useState(null)

  const handleClose = useCallback(() => {
    setQuery('')
    setError(null)
    onClose()
  }, [onClose])

  async function handleAdd(userId) {
    setError(null)
    try {
      await addMember.mutateAsync(userId)
      handleClose()
    } catch (err) {
      setError(err.message ?? 'Something went wrong. Try again.')
    }
  }

  const visibleResults = (results ?? []).filter((user) => !excludeUserIds.includes(user.id))
  const hasSearched = debouncedQuery.trim().length >= 2

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title="Add member">
      <FormField
        label="Search by email"
        autoFocus
        value={query}
        onChange={(event) => setQuery(event.target.value)}
      />

      {isFetching && <Spinner label="Searching" />}
      {error && (
        <p role="alert" className={styles.error}>
          {error}
        </p>
      )}

      {!isFetching && hasSearched && visibleResults.length === 0 && (
        <p className={styles.empty}>No matching users.</p>
      )}

      <ul className={styles.results}>
        {visibleResults.map((user) => (
          <li key={user.id} className={styles.result}>
            <Avatar firstName={user.firstName} lastName={user.lastName} size={28} />
            <div className={styles.resultInfo}>
              <span className={styles.resultName}>
                {user.firstName} {user.lastName}
              </span>
              <span className={styles.resultEmail}>{user.email}</span>
            </div>
            <Button
              variant="secondary"
              className={styles.addButton}
              onClick={() => handleAdd(user.id)}
              disabled={addMember.isPending}
            >
              Add
            </Button>
          </li>
        ))}
      </ul>
    </Modal>
  )
}
