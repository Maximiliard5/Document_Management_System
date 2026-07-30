import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../shared/auth/useAuth.js'
import { Button } from '../../shared/components/Button.jsx'
import { FormField } from '../../shared/components/FormField.jsx'
import { ApiError } from '../../shared/api/httpClient.js'
import styles from './RegisterPage.module.css'

// Mirrors backend RegisterRequest's Bean Validation constraints exactly - here
// (unlike login) the password length actually matters, since this is where
// it's chosen.
const registerSchema = z.object({
  firstName: z.string().min(1, 'First name is required').max(100, 'Must be 100 characters or fewer'),
  lastName: z.string().min(1, 'Last name is required').max(100, 'Must be 100 characters or fewer'),
  email: z.string().min(1, 'Email is required').email('Enter a valid email address').max(255),
  password: z.string().min(8, 'Password must be at least 8 characters').max(72, 'Password must be 72 characters or fewer'),
})

export function RegisterPage() {
  const { register: registerUser } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({ resolver: zodResolver(registerSchema) })

  const onSubmit = async (data) => {
    try {
      await registerUser(data)
      const redirectTo = location.state?.from?.pathname ?? '/projects'
      navigate(redirectTo, { replace: true })
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        // Email already registered - attach to the field at fault rather
        // than a generic form-level message.
        setError('email', { message: err.message })
        return
      }
      const message = err instanceof ApiError ? err.message : 'Something went wrong. Try again.'
      setError('root', { message })
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <h1 className={styles.brand}>Utrecht</h1>
        <p className={styles.subtitle}>Create your account</p>

        <form onSubmit={handleSubmit(onSubmit)} noValidate className={styles.form}>
          <div className={styles.row}>
            <FormField
              label="First name"
              autoComplete="given-name"
              error={errors.firstName?.message}
              {...register('firstName')}
            />
            <FormField
              label="Last name"
              autoComplete="family-name"
              error={errors.lastName?.message}
              {...register('lastName')}
            />
          </div>
          <FormField
            label="Email"
            type="email"
            autoComplete="email"
            error={errors.email?.message}
            {...register('email')}
          />
          <FormField
            label="Password"
            type="password"
            autoComplete="new-password"
            error={errors.password?.message}
            {...register('password')}
          />

          {errors.root && (
            <p className={styles.formError} role="alert">
              {errors.root.message}
            </p>
          )}

          <Button type="submit" className={styles.submit} disabled={isSubmitting}>
            {isSubmitting ? 'Creating account...' : 'Create account'}
          </Button>
        </form>

        <p className={styles.footer}>
          Already have an account? <Link to="/login">Log in</Link>
        </p>
      </div>
    </div>
  )
}
