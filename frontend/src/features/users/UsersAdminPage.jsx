import { useState } from 'react'
import { PageHeader } from '../../shared/components/PageHeader.jsx'
import { Button } from '../../shared/components/Button.jsx'
import { Badge } from '../../shared/components/Badge.jsx'
import { Spinner } from '../../shared/components/Spinner.jsx'
import { ConfirmDialog } from '../../shared/components/ConfirmDialog.jsx'
import { ApiError } from '../../shared/api/httpClient.js'
import { Role } from '../../shared/constants/roles.js'
import { useUsers, useUpdateUserRole, useDeactivateUser } from './useUsers.js'
import styles from './UsersAdminPage.module.css'

export function UsersAdminPage() {
  const { data: users, isLoading, isError, error } = useUsers()
  const updateUserRole = useUpdateUserRole()
  const deactivateUser = useDeactivateUser()

  const [actionError, setActionError] = useState(null)
  const [userPendingDeactivation, setUserPendingDeactivation] = useState(null)

  async function handleRoleChange(user, role) {
    if (role === user.role) return
    setActionError(null)
    try {
      await updateUserRole.mutateAsync({ userId: user.id, role })
    } catch (err) {
      // Covers the backend's last-active-admin guard (409) as well as any
      // other failure - both are just shown as-is.
      setActionError(err instanceof ApiError ? err.message : 'Something went wrong. Try again.')
    }
  }

  async function handleConfirmDeactivate() {
    if (!userPendingDeactivation) return
    setActionError(null)
    try {
      await deactivateUser.mutateAsync(userPendingDeactivation.id)
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'Something went wrong. Try again.')
    } finally {
      setUserPendingDeactivation(null)
    }
  }

  return (
    <div>
      <PageHeader title="Users" description="Manage roles and account access." />

      {actionError && (
        <p role="alert" className={styles.error}>
          {actionError}
        </p>
      )}

      {isLoading && <Spinner label="Loading users" />}

      {isError && (
        <p role="alert" className={styles.error}>
          {error.message}
        </p>
      )}

      {!isLoading && !isError && (
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Status</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id}>
                <td>
                  {user.firstName} {user.lastName}
                </td>
                <td>{user.email}</td>
                <td>
                  <select
                    className={styles.roleSelect}
                    value={user.role}
                    onChange={(event) => handleRoleChange(user, event.target.value)}
                    disabled={updateUserRole.isPending}
                  >
                    <option value={Role.USER}>User</option>
                    <option value={Role.ADMIN}>Admin</option>
                  </select>
                </td>
                <td>
                  <Badge color={user.active ? undefined : 'var(--priority-high)'}>
                    {user.active ? 'Active' : 'Deactivated'}
                  </Badge>
                </td>
                <td className={styles.actions}>
                  <Button
                    variant="danger"
                    disabled={!user.active}
                    onClick={() => setUserPendingDeactivation(user)}
                  >
                    Deactivate
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <ConfirmDialog
        isOpen={Boolean(userPendingDeactivation)}
        title="Deactivate account?"
        message={
          userPendingDeactivation
            ? `${userPendingDeactivation.firstName} ${userPendingDeactivation.lastName} will lose access immediately. There is no way to reactivate an account from here.`
            : ''
        }
        confirmLabel="Deactivate"
        isDangerous
        onConfirm={handleConfirmDeactivate}
        onCancel={() => setUserPendingDeactivation(null)}
      />
    </div>
  )
}
