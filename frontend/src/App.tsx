import { lazy, Suspense } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { ToastProvider } from "./components/Toast";
import { AppShell, PublicShell } from "./components/Shell";
import { ProtectedRoute } from "./components/ProtectedRoute";
const HomePage = lazy(() => import("./pages/HomePage").then((module) => ({ default: module.HomePage })));
const LoginPage = lazy(() => import("./pages/AuthPages").then((module) => ({ default: module.LoginPage })));
const RegisterPage = lazy(() => import("./pages/AuthPages").then((module) => ({ default: module.RegisterPage })));
const ForgotPasswordPage = lazy(() => import("./pages/AuthPages").then((module) => ({ default: module.ForgotPasswordPage })));
const ResetPasswordPage = lazy(() => import("./pages/AuthPages").then((module) => ({ default: module.ResetPasswordPage })));
const OAuthCallbackPage = lazy(() => import("./pages/AuthPages").then((module) => ({ default: module.OAuthCallbackPage })));
const DashboardPage = lazy(() => import("./pages/AppPages").then((module) => ({ default: module.DashboardPage })));
const AppointmentsPage = lazy(() => import("./pages/AppPages").then((module) => ({ default: module.AppointmentsPage })));
const BookingPage = lazy(() => import("./pages/AppPages").then((module) => ({ default: module.BookingPage })));
const RecordsPage = lazy(() => import("./pages/AppPages").then((module) => ({ default: module.RecordsPage })));
const FollowUpsPage = lazy(() => import("./pages/AppPages").then((module) => ({ default: module.FollowUpsPage })));
const ProfilePage = lazy(() => import("./pages/AppPages").then((module) => ({ default: module.ProfilePage })));
const AssistantPage = lazy(() => import("./pages/AppPages").then((module) => ({ default: module.AssistantPage })));
const ChatPage = lazy(() => import("./pages/AppPages").then((module) => ({ default: module.ChatPage })));
const AdminPage = lazy(() => import("./pages/AppPages").then((module) => ({ default: module.AdminPage })));
const DoctorProfilePage = lazy(() => import("./pages/DoctorProfilePage").then((module) => ({ default: module.DoctorProfilePage })));

export default function App() {
  return <ToastProvider><Suspense fallback={<div className="page-loading">DentalCare đang tải...</div>}><Routes>
    <Route element={<PublicShell />}><Route index element={<HomePage />} /><Route path="/book" element={<BookingPage />} /></Route>
    <Route path="/login" element={<LoginPage />} />
    <Route path="/register" element={<RegisterPage />} />
    <Route path="/forgot-password" element={<ForgotPasswordPage />} />
    <Route path="/reset-password" element={<ResetPasswordPage />} />
    <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
    <Route element={<ProtectedRoute />}><Route path="/app" element={<AppShell />}>
      <Route index element={<DashboardPage />} />
      <Route element={<ProtectedRoute roles={["PATIENT","RECEPTIONIST","DOCTOR"]} />}><Route path="appointments" element={<AppointmentsPage />} /></Route>
      <Route element={<ProtectedRoute roles={["PATIENT","RECEPTIONIST","DOCTOR","ADMIN"]} />}><Route path="chat" element={<ChatPage />} /></Route>
      <Route element={<ProtectedRoute roles={["ADMIN"]} />}><Route path="admin" element={<AdminPage />} /></Route>
      <Route element={<ProtectedRoute roles={["PATIENT","RECEPTIONIST"]} />}><Route path="book" element={<BookingPage />} /></Route>
      <Route element={<ProtectedRoute roles={["PATIENT","DOCTOR"]} />}><Route path="records" element={<RecordsPage />} /></Route>
      <Route element={<ProtectedRoute roles={["PATIENT","DOCTOR"]} />}><Route path="follow-ups" element={<FollowUpsPage />} /></Route>
      <Route element={<ProtectedRoute roles={["DOCTOR"]} />}><Route path="doctor-profile" element={<DoctorProfilePage />} /></Route>
      <Route path="profile" element={<ProfilePage />} />
      <Route element={<ProtectedRoute roles={["PATIENT"]} />}><Route path="assistant" element={<AssistantPage />} /></Route>
    </Route></Route>
    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes></Suspense></ToastProvider>;
}
