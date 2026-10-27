export type Role = "PATIENT" | "RECEPTIONIST" | "DOCTOR" | "ADMIN";
export interface User {
  id: number;
  fullName: string;
  email: string;
  phone: string | null;
  avatarUrl: string | null;
  role: Role;
}
export interface AuthTokens { accessToken: string; refreshToken: string; user: User }
export interface DentalService {
  id: number; name: string; category: string | null; price: number; durationMinutes: number; description: string | null; active:boolean;
}
export interface Doctor {
  id: number; fullName: string; avatarUrl: string | null; specialty: string | null;
  experienceYears: number; qualification: string | null; description: string | null; achievements: string | null;
}
export type DoctorProfileStatus = "NOT_SUBMITTED" | "PENDING" | "APPROVED" | "REJECTED";
export interface DoctorProfileDraft {
  doctorId: number; doctorName: string; avatarUrl: string | null; specialtyId: number | null; specialty: string | null;
  experienceYears: number; qualification: string | null; description: string | null; achievements: string | null;
  status: DoctorProfileStatus; reviewNote: string | null; submittedAt: string | null;
}
export interface Specialty { id:number;name:string;description:string|null }
export interface AdminUser { id:number;fullName:string;email:string;phone:string|null;role:Role;active:boolean }
export type AppointmentStatus = "PENDING" | "CONFIRMED" | "CANCELLED" | "COMPLETED" | "NO_SHOW" | "REJECTED";
export interface Appointment {
  id: number; patientId: number; patientName: string; doctorId: number; doctorName: string;
  serviceId: number; serviceName: string; startAt: string; endAt: string; status: AppointmentStatus;
  symptoms: string | null; internalNote: string | null;
}
export interface MedicalRecord {
  id: number; patientId: number; patientName: string; doctorId: number; doctorName: string;
  appointmentId: number; examinationDate: string; symptoms: string | null; diagnosis: string | null;
  treatment: string | null; prescription: string | null; notes: string | null; followUpDate: string | null;
}
export interface FollowUp { id:number;patientId:number;patientName:string;doctorId:number;doctorName:string;previousAppointmentId:number;scheduledAt:string;status:string;reason:string;notes:string|null }
export interface DoctorUnavailability { id:number;doctorId:number;doctorName:string;startAt:string;endAt:string;reason:string|null }
export interface ClinicNotification { id:number;title:string;message:string;actionPath:string|null;createdAt:string;readAt:string|null }
export interface ChatContact {
  id:number;fullName:string;avatarUrl:string|null;role:Role;
  lastMessage:string|null;lastMessageAt:string|null;unreadCount:number;
}
export interface ChatMessage {
  id:number;senderId:number;senderName:string;recipientId:number;recipientName:string;
  content:string;createdAt:string;readAt:string|null;
}
export interface NotificationFeed { items:ClinicNotification[];unreadCount:number }
export interface ApiResponse<T> { success: boolean; data: T; message: string | null }
export interface PageResult<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
