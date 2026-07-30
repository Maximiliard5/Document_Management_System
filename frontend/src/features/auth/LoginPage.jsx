import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../shared/auth/useAuth.js'
import { Button } from '../../shared/components/Button.jsx'
import { FormField } from '../../shared/components/FormField.jsx'
import { ApiError } from '../../shared/api/httpClient.js'
import styles from './LoginPage.module.css'

const loginSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
  // Only checking presence here, not mirroring the backend's min(8)/max(72) -
  // those constraints matter when choosing a password at registration, not
  // when logging in with one that already exists.
  password: z.string().min(1, 'Password is required'),
})

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({ resolver: zodResolver(loginSchema) })

  const onSubmit = async (data) => {
    try {
      await login(data.email, data.password)
      const redirectTo = location.state?.from?.pathname ?? '/projects'
      navigate(redirectTo, { replace: true })
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Something went wrong. Try again.'
      setError('root', { message })
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <h1 className={styles.brand}>Utrecht</h1>
        <p className={styles.subtitle}>Log in to your account</p>

        <form onSubmit={handleSubmit(onSubmit)} noValidate className={styles.form}>
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
            autoComplete="current-password"
            error={errors.password?.message}
            {...register('password')}
          />

          {errors.root && (
            <p className={styles.formError} role="alert">
              {errors.root.message}
            </p>
          )}

          <Button type="submit" className={styles.submit} disabled={isSubmitting}>
            {isSubmitting ? 'Logging in...' : 'Log in'}
          </Button>
        </form>

        <p className={styles.footer}>
          Don't have an account? <Link to="/register">Register</Link>
        </p>
      </div>
    </div>
  )
}
