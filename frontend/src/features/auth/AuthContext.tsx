import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { api, unwrap } from "../../api/client";
import type { ApiResponse, AuthTokens, User } from "../../types";

interface AuthContextValue { user: User | null; login: (email: string, password: string) => Promise<void>; register: (payload: { fullName: string; email: string; password: string; phone: string }) => Promise<void>; logout: () => Promise<void>; acceptOAuth: (accessToken:string,refreshToken:string)=>Promise<void>; updateUser:(user:User)=>void }
const AuthContext = createContext<AuthContextValue | null>(null);

function store(tokens: AuthTokens) {
  localStorage.setItem("dentalcare.access", tokens.accessToken);
  localStorage.setItem("dentalcare.refresh", tokens.refreshToken);
  localStorage.setItem("dentalcare.user", JSON.stringify(tokens.user));
}
function initialUser(): User | null {
  try { return JSON.parse(localStorage.getItem("dentalcare.user") ?? "null") as User | null; }
  catch { localStorage.removeItem("dentalcare.user"); return null; }
}
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(initialUser);
  useEffect(() => {
    const expireSession = () => setUser(null);
    window.addEventListener("dentalcare:session-expired", expireSession);
    return () => window.removeEventListener("dentalcare:session-expired", expireSession);
  }, []);
  const login = useCallback(async (email: string, password: string) => {
    const tokens = await unwrap(api.post<ApiResponse<AuthTokens>>("/auth/login", { email, password }));
    store(tokens); setUser(tokens.user);
  }, []);
  const register = useCallback(async (payload: { fullName: string; email: string; password: string; phone: string }) => {
    const tokens = await unwrap(api.post<ApiResponse<AuthTokens>>("/auth/register", payload));
    store(tokens); setUser(tokens.user);
  }, []);
  const logout = useCallback(async () => {
    const refreshToken = localStorage.getItem("dentalcare.refresh");
    try { if (refreshToken) await api.post("/auth/logout", { refreshToken }); }
    finally {
      localStorage.removeItem("dentalcare.access"); localStorage.removeItem("dentalcare.refresh");
      localStorage.removeItem("dentalcare.user"); setUser(null);
    }
  }, []);
  const acceptOAuth = useCallback(async(accessToken:string,refreshToken:string)=>{
    localStorage.setItem("dentalcare.access",accessToken);
    localStorage.setItem("dentalcare.refresh",refreshToken);
    const result=await unwrap(api.get<ApiResponse<User>>("/auth/me"));
    localStorage.setItem("dentalcare.user",JSON.stringify(result));setUser(result);
  },[]);
  const updateUser=useCallback((next:User)=>{localStorage.setItem("dentalcare.user",JSON.stringify(next));setUser(next);},[]);
  const value = useMemo(() => ({ user, login, register, logout, acceptOAuth, updateUser }), [user, login, register, logout, acceptOAuth,updateUser]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
