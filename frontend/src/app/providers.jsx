import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider } from '../shared/auth/AuthContext.jsx'

const queryClient = new QueryClient()

export function Providers({ children }) {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>{children}</AuthProvider>
    </QueryClientProvider>
  )
}
