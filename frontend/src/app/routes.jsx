import { createBrowserRouter, Navigate } from 'react-router-dom'
import { ProtectedRoute } from '../shared/auth/ProtectedRoute.jsx'
import { AdminRoute } from '../shared/auth/AdminRoute.jsx'
import { App } from './App.jsx'
import { LoginPage } from '../features/auth/LoginPage.jsx'
import { RegisterPage } from '../features/auth/RegisterPage.jsx'
import { ProjectsListPage } from '../features/projects/ProjectsListPage.jsx'
import { ProjectDetailLayout } from '../features/projects/ProjectDetailLayout.jsx'
import { MembersSection } from '../features/projects/MembersSection.jsx'
import { TaskBoardPage } from '../features/tasks/TaskBoardPage.jsx'
import { DocumentsPage } from '../features/documents/DocumentsPage.jsx'
import { UsersAdminPage } from '../features/users/UsersAdminPage.jsx'

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <App />,
        children: [
          { index: true, element: <Navigate to="/projects" replace /> },
          { path: 'projects', element: <ProjectsListPage /> },
          {
            path: 'projects/:projectId',
            element: <ProjectDetailLayout />,
            children: [
              { index: true, element: <Navigate to="board" replace /> },
              { path: 'board', element: <TaskBoardPage /> },
              { path: 'documents', element: <DocumentsPage /> },
              { path: 'members', element: <MembersSection /> },
            ],
          },
          {
            element: <AdminRoute />,
            children: [{ path: 'admin/users', element: <UsersAdminPage /> }],
          },
        ],
      },
    ],
  },
])
