import { useState } from "react";
import { Link, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell, CalendarDays, CheckCheck, ChevronDown, CircleHelp, HeartPulse, LogOut, Menu, MessageCircle, Settings2, ShieldCheck, Sparkles, UserRound, X, BadgeCheck } from "lucide-react";
import { clinicApi } from "../api/clinic";
import { useAuth } from "../features/auth/AuthContext";
import { apiError } from "../api/client";
import { useToast } from "./Toast";

export function PublicShell() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location=useLocation();
  return <><header className="public-header"><Link to="/" className="brand"><span className="brand-mark"><HeartPulse size={21} /></span><span>Dental<span>Care</span></span></Link>
    <nav className="public-nav"><a href="/#services">Dịch vụ</a><a href="/#doctors">Đội ngũ bác sĩ</a><a href="/#about">Về chúng tôi</a></nav>
    <div className="header-actions">{user ? <><Link className="button button-quiet" to="/app">Không gian của tôi</Link><button className="avatar-button" title="Đăng xuất" onClick={() => { void logout(); navigate("/"); }}><LogOut size={17} /></button></> :
      <><Link to="/login" state={{returnTo:location.pathname==="/book"?"/book":"/app"}} className="login-link">Đăng nhập</Link><Link to="/book" className="button button-primary">Đặt lịch khám <span>↗</span></Link></>}</div>
  </header><main><Outlet /></main><footer className="site-footer"><div className="brand"><span className="brand-mark"><HeartPulse size={20} /></span><span>Dental<span>Care</span></span></div><p>Chăm sóc nụ cười bằng sự tận tâm và chuyên môn.</p><span>© {new Date().getFullYear()} DentalCare Clinic</span></footer></>;
}

const nav = [
  { to: "/app", label: "Tổng quan", icon: ShieldCheck, roles: ["PATIENT","RECEPTIONIST","DOCTOR","ADMIN"] },
  { to: "/app/appointments", label: "Lịch hẹn", icon: CalendarDays, roles: ["PATIENT","RECEPTIONIST","DOCTOR"] },
  { to: "/app/chat", label: "Tin nhắn", icon: MessageCircle, roles: ["PATIENT","RECEPTIONIST","DOCTOR","ADMIN"] },
  { to: "/app/book", label: "Đặt lịch khám", icon: CalendarDays, roles: ["PATIENT"] },
  { to: "/app/book", label: "Tạo lịch hẹn cho bệnh nhân", icon: CalendarDays, roles: ["RECEPTIONIST"] },
  { to: "/app/records", label: "Hồ sơ điều trị", icon: HeartPulse, roles: ["PATIENT","DOCTOR"] },
  { to: "/app/follow-ups", label: "Lịch tái khám", icon: CalendarDays, roles: ["PATIENT","DOCTOR"] },
  { to: "/app/profile", label: "Thông tin cá nhân", icon: UserRound, roles: ["PATIENT","RECEPTIONIST","DOCTOR","ADMIN"] },
  { to: "/app/assistant", label: "Trợ lý nha khoa AI", icon: Sparkles, roles: ["PATIENT"] },
  { to: "/app/doctor-profile", label: "Hồ sơ công khai của tôi", icon: BadgeCheck, roles: ["DOCTOR"] },
  { to: "/app/admin", label: "Quản trị hệ thống", icon: Settings2, roles: ["ADMIN"] },
];
function NotificationMenu() {
  const [open,setOpen]=useState(false);
  const cache=useQueryClient();
  const toast=useToast();
  const navigate=useNavigate();
  const notifications=useQuery({queryKey:["notifications"],queryFn:clinicApi.notifications,refetchInterval:10000});
  const markRead=useMutation({mutationFn:clinicApi.markNotificationRead,
    onSuccess:()=>void cache.invalidateQueries({queryKey:["notifications"]}),
    onError:error=>toast(apiError(error),"error")});
  const markAll=useMutation({mutationFn:clinicApi.markAllNotificationsRead,
    onSuccess:()=>void cache.invalidateQueries({queryKey:["notifications"]}),
    onError:error=>toast(apiError(error),"error")});
  const openNotification=(item:NonNullable<typeof notifications.data>["items"][number])=>{
    if(!item.readAt)markRead.mutate(item.id);
    setOpen(false);
    const legacySender=item.title==="Tin nhắn mới"
      ?item.message.match(/^(.*?) \([^)]*\) đã gửi cho bạn một tin nhắn\.$/)?.[1]
      :undefined;
    const actionPath=item.actionPath
      ??(legacySender?`/app/chat?contactName=${encodeURIComponent(legacySender)}`:undefined)
      ??(item.title==="Yêu cầu đặt lịch mới"?"/app/appointments?status=PENDING":undefined);
    if(actionPath?.startsWith("/")&&!actionPath.startsWith("//"))navigate(actionPath);
  };
  return <div className="notification-menu">
    <button className="notification-trigger" aria-label={`Thông báo${notifications.data?.unreadCount ? `, ${notifications.data.unreadCount} chưa đọc` : ""}`}
      aria-expanded={open} onClick={()=>setOpen(value=>!value)}>
      <Bell size={19}/>{Boolean(notifications.data?.unreadCount)&&<span className="notification-count">{notifications.data?.unreadCount}</span>}
    </button>
    {open&&<section className="notification-popover" aria-label="Thông báo">
      <header><strong>Thông báo</strong><button className="notification-read-all" disabled={!notifications.data?.unreadCount||markAll.isPending}
        onClick={()=>markAll.mutate()}><CheckCheck size={15}/> Đọc tất cả</button></header>
      {notifications.isPending?<p className="notification-empty">Đang tải thông báo...</p>
        :notifications.isError?<p className="notification-empty">Không thể tải thông báo. Vui lòng thử lại.</p>
        :notifications.data.items.length===0?<p className="notification-empty">Bạn chưa có thông báo mới.</p>
        :<ul>{notifications.data.items.map(item=><li key={item.id} className={item.readAt?"":"notification-unread"}>
          <button type="button" className="notification-open" onClick={()=>openNotification(item)}><strong>{item.title}</strong><p>{item.message}</p><time dateTime={item.createdAt}>{new Intl.DateTimeFormat("vi-VN",{dateStyle:"short",timeStyle:"short"}).format(new Date(item.createdAt))}</time>{item.actionPath&&<span className="notification-action-hint">Mở nội dung →</span>}</button>
          {!item.readAt&&<button aria-label={`Đánh dấu đã đọc: ${item.title}`} onClick={()=>markRead.mutate(item.id)} disabled={markRead.isPending}>Đọc</button>}
        </li>)}</ul>}
    </section>}
  </div>;
}
export function AppShell() {
  const { user, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  if (!user) return null;
  const isPatient = user.role === "PATIENT";
  const roleLabel=user.role==="PATIENT"?"Bệnh nhân":user.role==="DOCTOR"?"Bác sĩ":user.role==="ADMIN"?"Quản trị viên":"Lễ tân";
  return <div className={`app-shell role-theme role-${user.role.toLowerCase()}`}>
    {isPatient ? <>
      <header className="patient-portal-header">
        <Link to="/" className="brand"><span className="brand-mark"><HeartPulse size={21} /></span><span>Dental<span>Care</span></span></Link>
        <nav aria-label="Điều hướng bệnh nhân">{nav.filter((item) => item.roles.includes(user.role) && item.to !== "/app/chat" && item.to !== "/app/assistant").map(({ to, label, icon: Icon }) =>
          <NavLink key={to} to={to} end={to === "/app"}><Icon size={17} />{label}</NavLink>)}</nav>
        <div className="patient-portal-actions"><NotificationMenu/><Link to="/app/profile" className="patient-account"><span className="user-avatar small">{user.fullName.slice(0,1).toUpperCase()}</span><span>{user.fullName}</span></Link>
          <button className="patient-logout" title="Đăng xuất khỏi tài khoản" aria-label="Đăng xuất" onClick={() => { void logout(); navigate("/"); }}><LogOut size={17} /></button>
        </div>
      </header>
      <main className="patient-portal-content"><Outlet /></main>
    </> : <>
      <aside className={`sidebar ${open ? "sidebar-open" : ""}`}>
        <Link to="/" className="brand"><span className="brand-mark"><HeartPulse size={21} /></span><span>Dental<span>Care</span></span></Link>
        <div className="clinic-switch"><span className="clinic-symbol">DC</span><span><strong>DentalCare Clinic</strong><small>Phòng khám trung tâm</small></span><ChevronDown size={15} /></div>
        <div className="nav-label">KHÔNG GIAN LÀM VIỆC</div>
        <nav className="side-nav">{nav.filter((item) => item.roles.includes(user.role)).map(({ to, label, icon: Icon }) =>
          <NavLink key={to} to={to} end={to === "/app"} onClick={() => setOpen(false)}><Icon size={18} />{label}</NavLink>)}</nav>
        <div className="sidebar-bottom"><div className="help-card"><CircleHelp size={19} /><strong>Cần hỗ trợ?</strong><span>Đội ngũ DentalCare luôn sẵn sàng giúp bạn.</span><span>Vui lòng liên hệ lễ tân phòng khám.</span></div>
          <button className="side-user" title="Đăng xuất khỏi tài khoản" onClick={() => { void logout(); navigate("/"); }}><span className="user-avatar">{user.fullName.slice(0,1).toUpperCase()}</span><span><strong>{user.fullName}</strong><small>{roleLabel}</small></span><LogOut size={17} /><span className="side-logout-label">Đăng xuất</span></button>
        </div>
      </aside>
      {open && <button className="sidebar-scrim" aria-label="Đóng menu" onClick={() => setOpen(false)} />}
      <div className="workspace"><header className="workspace-top"><button className="mobile-menu" onClick={() => setOpen(!open)} aria-label="Mở menu">{open ? <X /> : <Menu />}</button><div className="breadcrumbs"><span>DentalCare</span><span>/</span><strong>{location.pathname.split("/").pop() === "app" ? "Tổng quan" : "Không gian làm việc"}</strong></div><div className="top-profile"><NotificationMenu/><Link to="/app/profile" className="top-account"><span className="user-avatar small">{user.fullName.slice(0,1).toUpperCase()}</span><span>{user.fullName}</span></Link><button className="workspace-logout" title="Đăng xuất khỏi tài khoản" onClick={()=>{void logout();navigate("/");}}><LogOut size={16}/>Đăng xuất</button></div></header><div className="workspace-content"><Outlet /></div></div>
    </>}
    <div className="workspace-quick-actions" aria-label="Truy cập nhanh">
      {user.role === "PATIENT" && <Link className="quick-action quick-action-ai" to="/app/assistant" aria-label="Mở trợ lý nha khoa AI" title="Trợ lý nha khoa AI"><Sparkles size={22} /></Link>}
      <Link className="quick-action quick-action-chat" to="/app/chat" aria-label="Mở tin nhắn" title="Tin nhắn"><MessageCircle size={22} /></Link>
    </div>
  </div>;
}
