import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import type { Role } from "../types";
export function ProtectedRoute({ roles }: { roles?: Role[] }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/app" replace />;
  return <Outlet />;
}
