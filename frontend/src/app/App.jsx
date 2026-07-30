import { Outlet } from 'react-router-dom'
import { Sidebar } from './Sidebar.jsx'
import { Topbar } from './Topbar.jsx'
import styles from './App.module.css'

// The shell for every protected route: fixed sidebar, topbar, and whatever
// the current route renders in the content area.
export function App() {
  return (
    <div className={styles.shell}>
      <Sidebar />
      <Topbar />
      <main className={styles.content}>
        <Outlet />
      </main>
    </div>
  )
}
