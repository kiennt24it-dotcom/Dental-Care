import { api, unwrap } from "./client";
import type { AdminUser, ApiResponse, Appointment, ChatContact, ChatMessage, DentalService, Doctor, DoctorProfileDraft, DoctorUnavailability, FollowUp, MedicalRecord, NotificationFeed, PageResult, Specialty, User } from "../types";

export const clinicApi = {
  services: () => unwrap(api.get<ApiResponse<DentalService[]>>("/public/services")),
  doctors: () => unwrap(api.get<ApiResponse<Doctor[]>>("/public/doctors")),
  patients: (search:string) => unwrap(api.get<ApiResponse<{id:number;fullName:string;email:string;phone:string|null}[]>>("/users/patients",{params:{search}})),
  appointments: (params?: { date?: string; from?: string; to?: string; status?: string }) => unwrap(api.get<ApiResponse<Appointment[]>>("/appointments", { params })),
  appointmentsPage: (params:{page:number;size:number;date?:string;from?:string;to?:string;status?:string;search?:string}) =>
    unwrap(api.get<ApiResponse<PageResult<Appointment>>>("/appointments/page",{params})),
  slots: (doctorId: number, serviceId: number, date: string) => unwrap(api.get<ApiResponse<{ slots: { start: string; end: string }[] }>>("/appointments/available-slots", { params: { doctorId, serviceId, date } })),
  book: (payload: { doctorId: number; serviceId: number; startAt: string; symptoms: string; patientId?:number }) =>
    unwrap(api.post<ApiResponse<Appointment>>("/appointments", payload)),
  changeStatus: (id: number, status: string, internalNote?: string) =>
    unwrap(api.patch<ApiResponse<Appointment>>(`/appointments/${id}/status`, { status, internalNote })),
  medicalRecords: () => unwrap(api.get<ApiResponse<MedicalRecord[]>>("/medical-records")),
  followUps: () => unwrap(api.get<ApiResponse<FollowUp[]>>("/follow-ups")),
  notifications: () => unwrap(api.get<ApiResponse<NotificationFeed>>("/notifications")),
  markNotificationRead: (id:number) => unwrap(api.patch<ApiResponse<void>>(`/notifications/${id}/read`)),
  markAllNotificationsRead: () => unwrap(api.patch<ApiResponse<void>>("/notifications/read-all")),
  myProfile: () => unwrap(api.get<ApiResponse<User>>("/users/me")),
  updateProfile: (payload:{fullName:string;phone:string;avatarUrl:string}) => unwrap(api.put<ApiResponse<User>>("/users/me",payload)),
  uploadAvatar: (file:File) => {const body=new FormData();body.append("file",file);return unwrap(api.post<ApiResponse<User>>("/users/me/avatar",body,{headers:{"Content-Type":"multipart/form-data"}}));},
  createMedicalRecord: (payload:{appointmentId:number;diagnosis:string;treatment:string;prescription:string;notes:string;followUpDate?:string}) => unwrap(api.post<ApiResponse<MedicalRecord>>("/medical-records",payload)),
  analyzeSymptoms: (symptoms: string, consentToExternalProcessing: boolean) => unwrap(api.post<ApiResponse<{ possible_concern: string; suggested_service: string; explanation: string; note: string }>>("/ai/symptoms", { symptoms, consentToExternalProcessing })),
  chatContacts: (search:string) => unwrap(api.get<ApiResponse<ChatContact[]>>("/chat/contacts",{params:{search}})),
  chatMessages: (contactId:number) => unwrap(api.get<ApiResponse<ChatMessage[]>>(`/chat/conversations/${contactId}`)),
  sendChatMessage: (recipientId:number,content:string) => unwrap(api.post<ApiResponse<ChatMessage>>("/chat/messages",{recipientId,content})),
  markChatRead: (contactId:number) => unwrap(api.patch<ApiResponse<number>>(`/chat/conversations/${contactId}/read`)),
  statistics: () => unwrap(api.get<ApiResponse<{ patients:number;doctors:number;appointmentsToday:number;completed:number;cancelled:number;noShow:number;noShowRate:number;revenue:number;appointmentsByDay:{label:string;appointments:number}[];popularServices:{name:string;appointments:number}[] }>>("/statistics/overview")),
  specialties: () => unwrap(api.get<ApiResponse<Specialty[]>>("/public/specialties")),
  adminServices: () => unwrap(api.get<ApiResponse<DentalService[]>>("/services")),
  adminUsers: (search:string) => unwrap(api.get<ApiResponse<AdminUser[]>>("/users",{params:{search}})),
  createService: (payload:{name:string;category:string;price:number;durationMinutes:number;description:string}) => unwrap(api.post<ApiResponse<DentalService>>("/services",payload)),
  updateService: (id:number,payload:{name:string;category:string;price:number;durationMinutes:number;description:string}) => unwrap(api.put<ApiResponse<DentalService>>(`/services/${id}`,payload)),
  setServiceActive: (id:number,active:boolean) => unwrap(api.patch<ApiResponse<DentalService>>(`/services/${id}/active`,{active})),
  createSpecialty: (payload:{name:string;description:string}) => unwrap(api.post<ApiResponse<Specialty>>("/specialties",payload)),
  setSchedule: (payload:{doctorId:number;dayOfWeek:string;startTime:string;endTime:string;breakStart:string;breakEnd:string;active:boolean}) => unwrap(api.post<ApiResponse<string>>("/schedules",payload)),
  doctorUnavailabilities: (doctorId:number) => unwrap(api.get<ApiResponse<DoctorUnavailability[]>>("/unavailabilities",{params:{doctorId}})),
  blockDoctorTime: (payload:{doctorId:number;startAt:string;endAt:string;reason:string}) => unwrap(api.post<ApiResponse<DoctorUnavailability>>("/unavailabilities",payload)),
  myDoctorProfile: () => unwrap(api.get<ApiResponse<DoctorProfileDraft>>("/doctor-profiles/me")),
  submitDoctorProfile: (payload:{specialtyId:number;experienceYears:number;qualification:string;description:string;achievements:string}) =>
    unwrap(api.put<ApiResponse<DoctorProfileDraft>>("/doctor-profiles/me",payload)),
  pendingDoctorProfiles: () => unwrap(api.get<ApiResponse<DoctorProfileDraft[]>>("/doctor-profiles/pending")),
  reviewDoctorProfile: (doctorId:number,decision:"APPROVE"|"REJECT",reviewNote:string) =>
    unwrap(api.patch<ApiResponse<DoctorProfileDraft>>(`/doctor-profiles/${doctorId}/review`,{decision,reviewNote})),
  setUserRole: (id:number,role:string) => unwrap(api.patch<ApiResponse<AdminUser>>(`/users/${id}/role`,{role})),
  setUserActive: (id:number,active:boolean) => unwrap(api.patch<ApiResponse<AdminUser>>(`/users/${id}/active`,{active})),
};
