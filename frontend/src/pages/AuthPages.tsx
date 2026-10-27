import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { HeartPulse, ShieldCheck, ArrowLeft, Mail, LockKeyhole, UserRound, Phone } from "lucide-react";
import { useAuth } from "../features/auth/AuthContext";
import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import { api, apiError, unwrap } from "../api/client";
import type { ApiResponse } from "../types";

const loginSchema = z.object({ email: z.string().email("Nhập đúng địa chỉ email"), password: z.string().min(1,"Nhập mật khẩu") });
const registerSchema = z.object({ fullName: z.string().min(2,"Vui lòng nhập họ tên"), email: z.string().email("Nhập đúng địa chỉ email"), phone: z.string().min(9,"Số điện thoại chưa hợp lệ"), password: z.string().min(8,"Mật khẩu cần ít nhất 8 ký tự") });
type LoginValues = z.infer<typeof loginSchema>;
type RegisterValues = z.infer<typeof registerSchema>;
function AuthFrame({ title, subtitle, children }: { title: string; subtitle: string; children: ReactNode }) {
  return <div className="auth-page"><Link to="/" className="auth-back"><ArrowLeft size={17} /> Về trang chủ</Link><section className="auth-card"><Link to="/" className="brand"><span className="brand-mark"><HeartPulse size={21} /></span><span>Dental<span>Care</span></span></Link><div className="auth-heading"><span className="eyebrow">CHĂM SÓC NỤ CƯỜI CỦA BẠN</span><h1>{title}</h1><p>{subtitle}</p></div>{children}<div className="auth-trust"><ShieldCheck size={16} /> Thông tin cá nhân của bạn luôn được bảo mật</div></section><div className="auth-art"><div className="art-orb orb-one" /><div className="art-orb orb-two" /><div className="art-copy"><span className="art-kicker">NỤ CƯỜI KHỎE MẠNH, BẮT ĐẦU TỪ HÔM NAY</span><h2>Chăm sóc tận tâm.<br />Nụ cười tự tin.</h2><p>Đội ngũ bác sĩ chuyên môn cao luôn đồng hành cùng bạn trên hành trình chăm sóc sức khỏe răng miệng.</p>  <div className="art-stat"><strong>Care<span>+</span></strong><div>★★★★★<small>Đồng hành cùng bạn trong từng lịch khám</small></div></div></div><span className="art-caption">DentalCare · Chăm sóc tận tâm</span></div></div>;
}
export function LoginPage() {
  const { user, login } = useAuth(); const navigate=useNavigate(); const location=useLocation(); const [error,setError]=useState("");
  const returnTo=typeof location.state==="object"&&location.state!==null&&"returnTo" in location.state&&typeof location.state.returnTo==="string"&&location.state.returnTo.startsWith("/")&&!location.state.returnTo.startsWith("//")?location.state.returnTo:"/app";
  const {register,handleSubmit,formState:{errors,isSubmitting}}=useForm<LoginValues>({resolver:zodResolver(loginSchema)});
  if(user) return <Navigate to={returnTo} replace />;
  const submit=async (values:LoginValues)=>{try{setError("");await login(values.email,values.password);navigate(returnTo,{replace:true});}catch(e){setError(apiError(e));}};
  return <AuthFrame title="Chào mừng trở lại" subtitle="Đăng nhập để quản lý lịch khám và hồ sơ điều trị của bạn.">
    <form className="auth-form" onSubmit={handleSubmit(submit)}><label>Email<input {...register("email")} type="email" autoComplete="email" placeholder="ban@email.com" /><Mail size={17} /></label>{errors.email&&<small className="field-error">{errors.email.message}</small>}
      <label>Mật khẩu<input {...register("password")} type="password" autoComplete="current-password" placeholder="Nhập mật khẩu" /><LockKeyhole size={17} /></label>{errors.password&&<small className="field-error">{errors.password.message}</small>}
      <div className="form-between"><label className="check-label"><input type="checkbox" /> Ghi nhớ đăng nhập</label><Link to="/forgot-password">Quên mật khẩu?</Link></div>
      {error&&<div className="form-alert">{error}</div>}<button className="button button-primary button-full" disabled={isSubmitting}>{isSubmitting?"Đang đăng nhập...":"Đăng nhập tài khoản"} <span>→</span></button>
      <div className="auth-divider"><span />hoặc<span /></div><a className="button google-button" href={`${import.meta.env.VITE_API_URL?.replace(/\/api\/?$/,"") ?? "http://localhost:8080"}/oauth2/authorization/google`}><b>G</b> Tiếp tục với Google</a>
      <p className="auth-switch">Bạn chưa có tài khoản? <Link to="/register">Đăng ký miễn phí</Link></p></form></AuthFrame>;
}
export function RegisterPage() {
  const {user,register:signUp}=useAuth();const navigate=useNavigate();const location=useLocation();const[error,setError]=useState("");
  const returnTo=typeof location.state==="object"&&location.state!==null&&"returnTo" in location.state&&typeof location.state.returnTo==="string"&&location.state.returnTo.startsWith("/")&&!location.state.returnTo.startsWith("//")?location.state.returnTo:"/app";
  const {register,handleSubmit,formState:{errors,isSubmitting}}=useForm<RegisterValues>({resolver:zodResolver(registerSchema)});
  if(user)return <Navigate to={returnTo} replace />;
  const submit=async(values:RegisterValues)=>{try{setError("");await signUp({...values,phone:values.phone});navigate(returnTo,{replace:true});}catch(e){setError(apiError(e));}};
  return <AuthFrame title="Tạo tài khoản" subtitle="Bắt đầu hành trình chăm sóc nụ cười cùng DentalCare."><form className="auth-form" onSubmit={handleSubmit(submit)}>
    <label>Họ và tên<input {...register("fullName")} autoComplete="name" placeholder="Nguyễn Minh Anh" /><UserRound size={17} /></label>{errors.fullName&&<small className="field-error">{errors.fullName.message}</small>}
    <label>Email<input {...register("email")} type="email" autoComplete="email" placeholder="ban@email.com" /><Mail size={17} /></label>{errors.email&&<small className="field-error">{errors.email.message}</small>}
    <label>Số điện thoại<input {...register("phone")} autoComplete="tel" placeholder="090 123 4567" /><Phone size={17} /></label>{errors.phone&&<small className="field-error">{errors.phone.message}</small>}
    <label>Mật khẩu<input {...register("password")} type="password" autoComplete="new-password" placeholder="Tối thiểu 8 ký tự" /><LockKeyhole size={17} /></label>{errors.password&&<small className="field-error">{errors.password.message}</small>}
    <label className="check-label terms"><input type="checkbox" required /> Tôi đồng ý với điều khoản sử dụng và chính sách bảo mật.</label>
    {error&&<div className="form-alert">{error}</div>}<button className="button button-primary button-full" disabled={isSubmitting}>{isSubmitting?"Đang tạo tài khoản...":"Tạo tài khoản"} <span>→</span></button><p className="auth-switch">Đã có tài khoản? <Link to="/login">Đăng nhập</Link></p>
  </form></AuthFrame>;
}
export function ForgotPasswordPage() {
  const [email,setEmail]=useState("");const [message,setMessage]=useState("");const [error,setError]=useState("");const [busy,setBusy]=useState(false);
  const submit=async(e:FormEvent)=>{e.preventDefault();setBusy(true);setError("");try{await unwrap(api.post<ApiResponse<null>>("/auth/forgot-password",{email}));setMessage("Nếu email thuộc một tài khoản đang hoạt động, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu.");}catch(err){setError(apiError(err));}finally{setBusy(false);}};
  return <AuthFrame title="Đặt lại mật khẩu" subtitle="Nhập email đã đăng ký. Chúng tôi sẽ gửi hướng dẫn bảo mật đến hộp thư của bạn."><form className="auth-form" onSubmit={submit}><label>Email<input type="email" required value={email} onChange={(e)=>setEmail(e.target.value)} placeholder="ban@email.com"/><Mail size={17}/></label>{message&&<div className="success-alert">{message}</div>}{error&&<div className="form-alert">{error}</div>}<button className="button button-primary button-full" disabled={busy}>{busy?"Đang gửi...":"Gửi hướng dẫn đặt lại"} <ArrowLeft size={16}/></button><p className="auth-switch"><Link to="/login">Quay lại đăng nhập</Link></p></form></AuthFrame>;
}
export function ResetPasswordPage() {
  const token=new URLSearchParams(location.search).get("token")??"";const navigate=useNavigate();const[password,setPassword]=useState("");const[message,setMessage]=useState("");const[error,setError]=useState("");const[busy,setBusy]=useState(false);
  const submit=async(e:FormEvent)=>{e.preventDefault();setError("");setBusy(true);try{await unwrap(api.post<ApiResponse<null>>("/auth/reset-password",{token,password}));setMessage("Mật khẩu đã được cập nhật. Bạn có thể đăng nhập bằng mật khẩu mới.");window.setTimeout(()=>navigate("/login"),1500);}catch(err){setError(apiError(err));}finally{setBusy(false);}};
  return <AuthFrame title="Tạo mật khẩu mới" subtitle="Chọn mật khẩu mạnh để bảo vệ tài khoản DentalCare."><form className="auth-form" onSubmit={submit}><label>Mật khẩu mới<input type="password" minLength={8} required value={password} onChange={(e)=>setPassword(e.target.value)} placeholder="Tối thiểu 8 ký tự"/><LockKeyhole size={17}/></label>{!token&&<div className="form-alert">Liên kết đặt lại mật khẩu không hợp lệ hoặc bị thiếu.</div>}{message&&<div className="success-alert">{message}</div>}{error&&<div className="form-alert">{error}</div>}<button className="button button-primary button-full" disabled={!token||busy}>{busy?"Đang cập nhật...":"Cập nhật mật khẩu"} <ArrowLeft size={16}/></button></form></AuthFrame>;
}
export function OAuthCallbackPage() {
  const {acceptOAuth}=useAuth();const navigate=useNavigate();const[error,setError]=useState("");
  useEffect(()=>{const params=new URLSearchParams(location.hash.slice(1));const access=params.get("accessToken");const refresh=params.get("refreshToken");window.history.replaceState(null,"",window.location.pathname+window.location.search);if(!access||!refresh){setError("Không thể hoàn tất đăng nhập Google.");return;}void acceptOAuth(access,refresh).then(()=>navigate("/app",{replace:true})).catch((e)=>{setError(apiError(e));});},[acceptOAuth,navigate]);
  return <div className="callback-screen">{error?<div className="form-alert">{error} <Link to="/login">Quay lại đăng nhập</Link></div>:<div className="loading-line">Đang hoàn tất đăng nhập an toàn...</div>}</div>;
}
