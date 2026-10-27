import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";
import type { ApiResponse, AuthTokens } from "../types";

const baseURL = import.meta.env.VITE_API_URL ?? "http://localhost:8080/api";
export const api = axios.create({ baseURL, headers: { "Content-Type": "application/json" } });
let refreshPromise: Promise<string | null> | null = null;

export function mediaUrl(path: string | null): string {
  if (!path) return "";
  try {
    return new URL(path, new URL(baseURL, window.location.origin)).toString();
  } catch {
    return "";
  }
}

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem("dentalcare.access");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined;
    if (error.response?.status !== 401 || !original || original._retried || original.url?.includes("/auth/")) throw error;
    original._retried = true;
    refreshPromise ??= api.post<ApiResponse<AuthTokens>>("/auth/refresh", {
      refreshToken: localStorage.getItem("dentalcare.refresh"),
    }).then(({ data }) => {
      localStorage.setItem("dentalcare.access", data.data.accessToken);
      localStorage.setItem("dentalcare.refresh", data.data.refreshToken);
      return data.data.accessToken;
    }).catch(() => {
      localStorage.removeItem("dentalcare.access");
      localStorage.removeItem("dentalcare.refresh");
      localStorage.removeItem("dentalcare.user");
      window.dispatchEvent(new Event("dentalcare:session-expired"));
      return null;
    }).finally(() => { refreshPromise = null; });
    const token = await refreshPromise;
    if (!token) throw error;
    original.headers.Authorization = `Bearer ${token}`;
    return api(original);
  },
);

export function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  return promise.then(({ data }) => {
    if (!data.success) throw new Error(data.message ?? "Yêu cầu không thành công");
    return data.data;
  });
}
export function apiError(error: unknown): string {
  if (axios.isAxiosError<ApiResponse<unknown>>(error)) return error.response?.data?.message ?? "Không thể kết nối máy chủ. Vui lòng thử lại.";
  return error instanceof Error ? error.message : "Đã xảy ra lỗi không xác định.";
}
