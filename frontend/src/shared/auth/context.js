import { createContext } from 'react'

// Split into its own file (rather than living alongside AuthProvider in
// AuthContext.jsx) because a file exporting both a React context and a
// component breaks Vite's fast-refresh boundary.
export const AuthContext = createContext(null)
