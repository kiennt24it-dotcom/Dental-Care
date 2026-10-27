import { useEffect, useMemo, useRef, useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Activity, ArrowUpRight, CalendarClock, CalendarDays, ChevronDown, CircleAlert, Clock3, HeartPulse, MessageCircle, Search, ShieldCheck, Sparkles, BadgeCheck } from "lucide-react";
import { Link, useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis } from "recharts";
import { Client } from "@stomp/stompjs";
import { api, apiError, mediaUrl } from "../api/client";
import { clinicApi } from "../api/clinic";
import { formatClinicTime, toClinicInstant } from "../utils/clinicTime";
import { useAuth } from "../features/auth/AuthContext";
import { useToast } from "../components/Toast";
import { AdminProfileReviewPage } from "./AdminProfileReviewPage";
import type { AdminUser, Appointment, AppointmentStatus, ChatContact, ChatMessage, DentalService, Doctor, FollowUp, MedicalRecord, PageResult } from "../types";

const money=(value:number)=>new Intl.NumberFormat("vi-VN",{style:"currency",currency:"VND",maximumFractionDigits:0}).format(value);
const dateLabel=(value:string)=>new Intl.DateTimeFormat("vi-VN",{weekday:"short",day:"2-digit",month:"short",hour:"2-digit",minute:"2-digit"}).format(new Date(value));
const clinicDate=(value:string)=>new Intl.DateTimeFormat("sv-SE",{timeZone:"Asia/Ho_Chi_Minh"}).format(new Date(value));
const followingDate=(value:string)=>{const day=new Date(`${value}T00:00:00Z`);day.setUTCDate(day.getUTCDate()+1);return day.toISOString().slice(0,10);};
const statusLabel:Record<AppointmentStatus,string>={PENDING:"Chờ xác nhận",CONFIRMED:"Đã xác nhận",CANCELLED:"Đã hủy",COMPLETED:"Hoàn thành",NO_SHOW:"Vắng mặt",REJECTED:"Từ chối"};
function PageHeading({eyebrow,title,subtitle,action}:{eyebrow:string;title:string;subtitle:string;action?:React.ReactNode}){
  return <div className="page-heading"><div><span className="eyebrow">{eyebrow}</span><h1>{title}</h1><p>{subtitle}</p></div>{action}</div>;
}
function useAppointments(date?:string,enabled=true) {
  return useQuery({queryKey:["appointments",date],queryFn:()=>clinicApi.appointments(date?{date}:undefined),enabled});
}
export function DashboardPage() {
  const {user}=useAuth();
  const today=new Date().toLocaleDateString("sv-SE");
  const appointments=useAppointments(user?.role==="PATIENT"||user?.role==="ADMIN"?undefined:today,user?.role!=="ADMIN");
  const statistics=useQuery({queryKey:["statistics"],queryFn:clinicApi.statistics,enabled:user?.role==="ADMIN"||user?.role==="RECEPTIONIST"});
  const pendingRequests=useQuery({queryKey:["pending-appointment-count"],queryFn:()=>clinicApi.appointmentsPage({page:0,size:1,status:"PENDING"}),enabled:user?.role==="RECEPTIONIST",refetchInterval:user?.role==="RECEPTIONIST"?10000:false});
  const recent=appointments.data??[];
  const pending=recent.filter((a)=>a.status==="PENDING").length;
  const confirmed=recent.filter((a)=>a.status==="CONFIRMED").length;
  const complete=recent.filter((a)=>a.status==="COMPLETED").length;
  const role=user?.role??"PATIENT";
  const heading=role==="ADMIN"?"Điều hành phòng khám":role==="RECEPTIONIST"?"Vận hành lễ tân":role==="DOCTOR"?"Không gian bác sĩ":"Tổng quan chăm sóc";
  const subtitle=role==="ADMIN"?"Theo dõi hoạt động tổng thể và quản trị nhân sự, hồ sơ công khai.":role==="RECEPTIONIST"?"Tiếp nhận yêu cầu, điều phối lịch khám và hỗ trợ bệnh nhân.":role==="DOCTOR"?"Theo dõi lịch khám, hồ sơ điều trị và hồ sơ chuyên môn của bạn.":"Quản lý lịch khám và thông tin chăm sóc của bạn.";
  const cards=role==="ADMIN"?[
    {title:"Bệnh nhân",value:statistics.data?String(statistics.data.patients):"—",note:"Tài khoản đang hoạt động",icon:HeartPulse},
    {title:"Bác sĩ",value:statistics.data?String(statistics.data.doctors):"—",note:"Đội ngũ chuyên môn",icon:ShieldCheck},
    {title:"Lịch khám hôm nay",value:statistics.data?String(statistics.data.appointmentsToday):"—",note:"Tổng lịch đã ghi nhận",icon:CalendarClock},
    {title:"Doanh thu hoàn tất",value:statistics.data?money(statistics.data.revenue):"—",note:"Từ dịch vụ hoàn tất",icon:Activity},
  ]:role==="RECEPTIONIST"?[
    {title:"Bệnh nhân",value:statistics.data?String(statistics.data.patients):"—",note:"Tài khoản hoạt động",icon:HeartPulse},
    {title:"Yêu cầu chờ duyệt",value:pendingRequests.data?String(pendingRequests.data.totalElements):pendingRequests.isLoading?"—":"0",note:"Cần xác nhận hoặc từ chối",icon:Clock3},
    {title:"Lịch khám hôm nay",value:appointments.isLoading?"—":String(recent.length),note:"Lịch trong ngày",icon:CalendarClock},
    {title:"Doanh thu hoàn tất",value:statistics.data?money(statistics.data.revenue):"—",note:"Chỉ số phòng khám",icon:Activity},
  ]:role==="DOCTOR"?[
    {title:"Lịch khám hôm nay",value:appointments.isLoading?"—":String(recent.length),note:"Lịch được phân công",icon:CalendarClock},
    {title:"Cần hoàn tất",value:appointments.isLoading?"—":String(confirmed),note:"Bệnh nhân đã xác nhận",icon:Clock3},
    {title:"Đã hoàn thành",value:appointments.isLoading?"—":String(complete),note:"Buổi khám hôm nay",icon:ShieldCheck},
  ]:[
    {title:"Lịch hẹn của tôi",value:appointments.isLoading?"—":String(recent.length),note:"Tổng các lịch khám",icon:CalendarClock},
    {title:"Chờ xác nhận",value:appointments.isLoading?"—":String(pending),note:"Yêu cầu đang được tiếp nhận",icon:Clock3},
    {title:"Đã xác nhận",value:appointments.isLoading?"—":String(confirmed),note:"Sẵn sàng đến khám",icon:ShieldCheck},
    {title:"Đã hoàn thành",value:appointments.isLoading?"—":String(complete),note:"Lịch khám đã kết thúc",icon:Activity},
  ];
  return <><PageHeading eyebrow={`${new Intl.DateTimeFormat("vi-VN",{weekday:"long"}).format(new Date()).toUpperCase()} · ${new Intl.DateTimeFormat("vi-VN",{day:"2-digit",month:"long",year:"numeric"}).format(new Date()).toUpperCase()}`} title={`${heading}, ${user?.fullName.split(" ").slice(-1)[0] ?? ""} 👋`} subtitle={subtitle}
    action={role==="PATIENT"?<Link to="/app/book" className="button button-primary"><CalendarDays size={17}/> Đặt lịch khám</Link>:role==="RECEPTIONIST"?<Link to="/app/book" className="button button-primary"><CalendarDays size={17}/> Tạo lịch cho bệnh nhân</Link>:role==="ADMIN"?<Link to="/app/admin" className="button button-primary"><ShieldCheck size={17}/> Quản trị hệ thống</Link>:<Link to="/app/doctor-profile" className="button button-primary"><BadgeCheck size={17}/> Hồ sơ chuyên môn</Link>} />
    <div className="dashboard-banner"><div className="banner-symbol"><Sparkles size={20}/></div><div><strong>{role==="PATIENT"?"Chăm sóc định kỳ giúp nụ cười luôn khỏe mạnh":role==="ADMIN"?"Quản trị an toàn, hồ sơ minh bạch":"Hôm nay tại DentalCare"}</strong><p>{role==="PATIENT"?"Đừng quên kiểm tra răng miệng định kỳ mỗi 6 tháng. Bác sĩ luôn sẵn sàng đồng hành cùng bạn.":role==="RECEPTIONIST"?`Bạn có ${pendingRequests.data?.totalElements??0} yêu cầu đặt lịch đang chờ xử lý.`:role==="ADMIN"?"Quản lý tài khoản, vận hành phòng khám và duyệt hồ sơ bác sĩ tại đây.":`Bạn có ${recent.length} lịch khám hôm nay, ${confirmed} lịch đã được xác nhận.`}</p></div>{role==="ADMIN"?<Link to="/app/admin">Mở quản trị <ArrowUpRight size={16}/></Link>:<Link to={role==="PATIENT"?"/app/book":role==="RECEPTIONIST"?"/app/appointments?status=PENDING":role==="DOCTOR"?"/app/appointments":"/app/admin"}>{role==="PATIENT"?"Đặt lịch":role==="RECEPTIONIST"?"Duyệt yêu cầu":"Xem lịch"} <ArrowUpRight size={16}/></Link>}</div>
    <div className="stat-grid">{cards.map(card=><StatCard key={card.title} {...card}/>)}</div>
    {(user?.role==="ADMIN"||user?.role==="RECEPTIONIST")&&statistics.data&&<section className="content-card chart-card"><div className="card-title-row"><div><h2>Hoạt động phòng khám</h2><p>Số lịch hẹn trong 7 ngày gần nhất · Tỷ lệ vắng mặt {(statistics.data.noShowRate*100).toFixed(1)}%</p></div><span className="chart-badge">DOANH THU {money(statistics.data.revenue)}</span></div><div className="chart-area"><ResponsiveContainer width="100%" height="100%"><BarChart data={statistics.data.appointmentsByDay} margin={{top:8,right:10,left:-24,bottom:0}}><CartesianGrid stroke="#edf1ef" vertical={false}/><XAxis dataKey="label" axisLine={false} tickLine={false} tick={{fontSize:10,fill:"#8b9998"}}/><Tooltip cursor={{fill:"#edf6f2"}} contentStyle={{borderRadius:8,border:"1px solid #e5eeea",fontSize:11}}/><Bar dataKey="appointments" name="Lịch hẹn" fill="#27877e" radius={[5,5,0,0]} maxBarSize={36}/></BarChart></ResponsiveContainer></div></section>}
    {appointments.isError&&<div className="error-state"><CircleAlert/>Không thể tải lịch hẹn. {apiError(appointments.error)}</div>}
    {role!=="ADMIN"&&<section className="content-card"><div className="card-title-row"><div><h2>{role==="PATIENT"?"Lịch hẹn gần đây":role==="RECEPTIONIST"?"Lịch khám trong ngày":"Lịch khám được phân công"}</h2><p>Cập nhật trực tiếp từ hệ thống DentalCare</p></div><Link to="/app/appointments" className="subtle-link">{role==="RECEPTIONIST"?"Duyệt & xem lịch":"Xem lịch"} <ArrowUpRight size={15}/></Link></div>
      {appointments.isLoading?<div className="loading-line">Đang tải dữ liệu lịch hẹn...</div>:recent.length===0?<div className="empty-state"><div><CalendarDays/></div><strong>Chưa có lịch hẹn</strong><span>Các lịch hẹn của bạn sẽ hiển thị tại đây.</span>{user?.role==="PATIENT"&&<Link to="/app/book" className="button button-primary">Đặt lịch đầu tiên</Link>}</div>:
        <AppointmentTable appointments={recent.slice(0,5)} role={user?.role??"PATIENT"} />}
    </section>}</>;
}
function StatCard({title,value,note,icon:Icon}:{title:string;value:string;note:string;icon:typeof CalendarDays}) {
  return <article className="stat-card"><div className="stat-card-top"><span>{title}</span><span className="stat-icon"><Icon size={18}/></span></div><strong>{value}</strong><small><span className="stat-trend"><ArrowUpRight size={13}/></span>{note}</small></article>;
}
function AppointmentTable({appointments,role}:{appointments:Appointment[];role:string}) {
  const toast=useToast(); const query=useQueryClient();
  const mutation=useMutation({mutationFn:({id,status}:{id:number;status:string})=>clinicApi.changeStatus(id,status),onSuccess:()=>{toast("Đã cập nhật trạng thái lịch hẹn");void query.invalidateQueries({queryKey:["appointments"]});},onError:(error)=>toast(apiError(error),"error")});
  const actions=(appointment:Appointment)=>role==="DOCTOR"?[
    ...(appointment.status==="CONFIRMED"?[{label:"Hoàn thành",status:"COMPLETED"},{label:"Vắng mặt",status:"NO_SHOW"}]:[]),
  ]:role==="RECEPTIONIST"?[
    ...(appointment.status==="PENDING"?[{label:"Xác nhận",status:"CONFIRMED"},{label:"Từ chối",status:"REJECTED"}]:[]),
    ...(appointment.status==="CONFIRMED"?[{label:"Hủy",status:"CANCELLED"}]:[]),
  ]:appointment.status==="PENDING"||appointment.status==="CONFIRMED"?[{label:"Hủy lịch",status:"CANCELLED"}]:[];
  return <div className="table-wrap"><table className="data-table"><thead><tr><th>BỆNH NHÂN</th><th>BÁC SĨ & DỊCH VỤ</th><th>THỜI GIAN</th><th>TRẠNG THÁI</th><th></th></tr></thead><tbody>{appointments.map((item)=><tr key={item.id}><td><span className="table-person"><span className="table-avatar">{item.patientName.slice(0,1)}</span><span><strong>{item.patientName}</strong><small>#{String(item.id).padStart(5,"0")}</small></span></span></td><td><strong>{item.doctorName}</strong><small className="table-secondary">{item.serviceName}</small></td><td>{dateLabel(item.startAt)}</td><td><span className={`status-pill ${item.status.toLowerCase()}`}><i/>{statusLabel[item.status]}</span></td><td><div className="table-actions">{actions(item).map((action)=><button key={action.status} className="table-action" disabled={mutation.isPending} onClick={()=>mutation.mutate({id:item.id,status:action.status})}>{action.label}</button>)}</div></td></tr>)}</tbody></table></div>;
}
export function AppointmentsPage() {
  const {user}=useAuth();const [searchParams]=useSearchParams();const location=useLocation();const [query,setQuery]=useState("");const routeStatus=searchParams.get("status")??"ALL";const [status,setStatus]=useState(routeStatus);
  const isCalendarUser=user?.role==="RECEPTIONIST"||user?.role==="DOCTOR";
  const [date,setDate]=useState("");
  const today=new Date().toLocaleDateString("sv-SE",{timeZone:"Asia/Ho_Chi_Minh"});
  const selectedDate=date||(isCalendarUser?today:"");
  const requestDate=user?.role==="RECEPTIONIST"&&status==="PENDING"&&!date?undefined:selectedDate;
  const [view,setView]=useState<"day"|"week">("day");
  const [page,setPage]=useState(0);
  useEffect(()=>{if(searchParams.has("status")){setStatus(routeStatus);if(routeStatus==="PENDING")setDate("");}},[location.key,routeStatus,searchParams]);
  useEffect(()=>setPage(0),[query,status,date,view]);
  const weekDates=useMemo(()=>{
    const selected=new Date(`${selectedDate||today}T00:00:00Z`);
    selected.setUTCDate(selected.getUTCDate()-((selected.getUTCDay()+6)%7));
    return Array.from({length:7},(_,index)=>{const day=new Date(selected);day.setUTCDate(selected.getUTCDate()+index);return day.toISOString().slice(0,10);});
  },[selectedDate,today]);
  const appointments=useQuery<Appointment[]|PageResult<Appointment>>({
    queryKey:["appointments",isCalendarUser?view:"list",requestDate,view==="week"?0:page,query,status],
    queryFn:()=>isCalendarUser&&view==="week"
      ?clinicApi.appointments({from:weekDates[0],to:followingDate(weekDates[6])})
      :clinicApi.appointmentsPage({
        page,size:20,...(requestDate?{date:requestDate}:{}),...(status!=="ALL"?{status}:{}),...(query.trim()?{search:query.trim()}:{}),
      }),
    refetchInterval:user?.role==="RECEPTIONIST"&&status==="PENDING"?10000:false,
  });
  const pageData=appointments.data&&!Array.isArray(appointments.data)?appointments.data:undefined;
  const appointmentRows=Array.isArray(appointments.data)?appointments.data:appointments.data?.content??[];
  const visible=useMemo(()=>appointmentRows.filter((item)=>
    (!selectedDate||isCalendarUser||clinicDate(item.startAt)===selectedDate)
    &&`${item.patientName} ${item.doctorName} ${item.serviceName}`.toLowerCase().includes(query.toLowerCase())
    &&(status==="ALL"||item.status===status)),[appointmentRows,query,status,selectedDate,isCalendarUser]);
  const groupedWeek=useMemo(()=>weekDates.map((day)=>({
    date:day,
    appointments:visible.filter((item)=>clinicDate(item.startAt)===day),
  })),[weekDates,visible]);
  return <><PageHeading eyebrow="LỊCH HẸN" title={user?.role==="RECEPTIONIST"?"Tiếp nhận & điều phối lịch":"Lịch khám của tôi"} subtitle={user?.role==="RECEPTIONIST"?"Duyệt yêu cầu đặt lịch và điều phối lịch khám của phòng khám.":user?.role==="DOCTOR"?"Theo dõi lịch được phân công; cập nhật kết quả sau buổi khám.":"Theo dõi lịch hẹn và trạng thái chăm sóc của bạn."} action={user?.role==="PATIENT"||user?.role==="RECEPTIONIST"?<Link to="/app/book" className="button button-primary"><CalendarDays size={17}/> {user?.role==="PATIENT"?"Đặt lịch khám":"Tạo lịch cho bệnh nhân"}</Link>:undefined}/>
    <section className="content-card"><div className="filter-row"><label className="search-field"><Search size={17}/><input value={query} onChange={(e)=>setQuery(e.target.value)} placeholder="Tìm bệnh nhân, bác sĩ, dịch vụ..." /></label><label className="filter-select"><CalendarDays size={16}/><input aria-label="Lọc theo ngày" type="date" value={user?.role==="RECEPTIONIST"&&status==="PENDING"&&!date?"":selectedDate} onChange={(e)=>setDate(e.target.value)}/></label><label className="filter-select"><ChevronDown size={16}/><select value={status} onChange={(e)=>setStatus(e.target.value)}><option value="ALL">Tất cả trạng thái</option>{Object.entries(statusLabel).map(([key,label])=><option value={key} key={key}>{label}</option>)}</select></label></div>
      {isCalendarUser&&<div className="calendar-toggle" role="group" aria-label="Chế độ xem lịch"><button type="button" className={`button ${view==="day"?"button-primary":"button-quiet"}`} onClick={()=>setView("day")}>Ngày</button><button type="button" className={`button ${view==="week"?"button-primary":"button-quiet"}`} onClick={()=>setView("week")}>Tuần</button></div>}
      {appointments.isLoading?<div className="loading-line">Đang tải lịch hẹn...</div>:appointments.isError?<div className="error-state">Không thể tải lịch hẹn. {apiError(appointments.error)}</div>:view==="week"&&isCalendarUser
        ?<>{groupedWeek.map((group)=><section className="calendar-day" key={group.date}><h3>{new Intl.DateTimeFormat("vi-VN",{weekday:"long",day:"2-digit",month:"long",timeZone:"UTC"}).format(new Date(`${group.date}T12:00:00Z`))}</h3>{group.appointments.length?<AppointmentTable appointments={group.appointments} role={user?.role??"PATIENT"}/>:<p className="calendar-empty">Không có lịch hẹn trong ngày này.</p>}</section>)}</>
        :visible.length?<><AppointmentTable appointments={visible} role={user?.role??"PATIENT"}/>{pageData&&pageData.totalPages>1&&<div className="pagination-row"><span>{pageData.totalElements} lịch hẹn · Trang {pageData.page+1}/{pageData.totalPages}</span><div><button type="button" className="button button-quiet" disabled={pageData.first} onClick={()=>setPage(current=>Math.max(0,current-1))}>Trước</button><button type="button" className="button button-quiet" disabled={pageData.last} onClick={()=>setPage(current=>current+1)}>Tiếp</button></div></div>}</>:<div className="empty-state"><div><CalendarDays/></div><strong>Không tìm thấy lịch hẹn</strong><span>Thử thay đổi bộ lọc hoặc tạo lịch hẹn mới.</span></div>}
    </section></>;
}
export function BookingPage() {
  const doctors=useQuery({queryKey:["public-doctors"],queryFn:clinicApi.doctors});
  const services=useQuery({queryKey:["public-services"],queryFn:clinicApi.services});
  const [draft,setDraft]=useState(()=>{try{return JSON.parse(sessionStorage.getItem("dentalcare.booking-draft")??"{}") as {doctorId?:string;serviceId?:string;date?:string;slot?:string;symptoms?:string};}catch{return {};}});
  const doctorId=draft.doctorId??"";const serviceId=draft.serviceId??"";const date=draft.date??"";const slot=draft.slot??"";const symptoms=draft.symptoms??"";
  const [patientSearch,setPatientSearch]=useState("");const[patientId,setPatientId]=useState<number|undefined>();
  const {user}=useAuth();const toast=useToast();const query=useQueryClient();
  const location=useLocation();const navigate=useNavigate();
  const setDoctorId=(value:string)=>setDraft(current=>({...current,doctorId:value,slot:""}));
  const setServiceId=(value:string)=>setDraft(current=>({...current,serviceId:value,slot:""}));
  const setDate=(value:string)=>setDraft(current=>({...current,date:value,slot:""}));
  const setSlot=(value:string)=>setDraft(current=>({...current,slot:value}));
  const setSymptoms=(value:string)=>setDraft(current=>({...current,symptoms:value}));
  useEffect(()=>{if(!user&&Object.values(draft).some(Boolean))sessionStorage.setItem("dentalcare.booking-draft",JSON.stringify(draft));},[draft,user]);
  const doctorOptions=doctors.data??[];const serviceOptions=services.data??[];
  const slots=useQuery({queryKey:["slots",doctorId,serviceId,date],queryFn:()=>clinicApi.slots(Number(doctorId),Number(serviceId),date),enabled:Boolean(doctorId&&serviceId&&date)});
  const patientOptions=useQuery({queryKey:["patients",patientSearch],queryFn:()=>clinicApi.patients(patientSearch),enabled:(user?.role==="RECEPTIONIST"||user?.role==="ADMIN")&&patientSearch.trim().length>=2});
  const booking=useMutation({mutationFn:()=>clinicApi.book({doctorId:Number(doctorId),serviceId:Number(serviceId),startAt:toClinicInstant(date,slot),symptoms,...(patientId?{patientId}:{})}),onSuccess:()=>{toast("Đã gửi yêu cầu đặt lịch. Lịch đang chờ phòng khám xác nhận.");void query.invalidateQueries({queryKey:["appointments"]});void query.invalidateQueries({queryKey:["notifications"]});sessionStorage.removeItem("dentalcare.booking-draft");setDraft({});setPatientId(undefined);setPatientSearch("");},onError:(error)=>toast(apiError(error),"error")});
  const minDate=new Intl.DateTimeFormat("sv-SE",{timeZone:"Asia/Ho_Chi_Minh"}).format(new Date());
  const chosenDoctor=doctorOptions.find((d)=>String(d.id)===doctorId);const chosenService=serviceOptions.find((s)=>String(s.id)===serviceId);
  const publicPage=location.pathname==="/book";
  return <div className={publicPage?"public-booking-page":undefined}>{publicPage&&<div className="public-booking-heading"><span className="eyebrow">ĐẶT LỊCH KHÁM TRỰC TUYẾN</span><h1>Chọn lịch khám phù hợp với bạn</h1><p>Chọn dịch vụ, bác sĩ và khung giờ còn trống. Phòng khám sẽ xác nhận yêu cầu của bạn.</p><div><span>01 · Chọn dịch vụ</span><span>02 · Chọn khung giờ</span><span>03 · Xác nhận lịch</span></div></div>}<><PageHeading eyebrow={publicPage?"THÔNG TIN CUỘC HẸN":"ĐẶT LỊCH TRỰC TUYẾN"} title="Lịch khám của bạn" subtitle="Chọn dịch vụ và khung giờ thuận tiện; lịch sẽ chờ phòng khám xác nhận."/><div className="booking-layout"><section className="content-card booking-form-card"><div className="booking-step"><span>01</span><div><strong>Chọn dịch vụ & bác sĩ</strong><small>Hãy cho chúng tôi biết bạn cần chăm sóc gì</small></div></div>
    {(user?.role==="RECEPTIONIST"||user?.role==="ADMIN")&&<label className="patient-picker">Bệnh nhân<input value={patientSearch} onChange={(e)=>{setPatientSearch(e.target.value);setPatientId(undefined);}} placeholder="Tìm theo tên bệnh nhân..." />{patientOptions.data?.length? <span className="patient-options">{patientOptions.data.map((patient)=><button type="button" key={patient.id} onClick={()=>{setPatientId(patient.id);setPatientSearch(patient.fullName);}}><strong>{patient.fullName}</strong><small>{patient.email} · {patient.phone??"Chưa có số điện thoại"}</small></button>)}</span>:patientSearch.length>=2&&!patientOptions.isLoading&&!patientOptions.isError?<small>Không tìm thấy bệnh nhân phù hợp.</small>:null}</label>}
    {doctors.isError||services.isError?<div className="error-state">Không thể tải dịch vụ hoặc bác sĩ. {apiError(doctors.error??services.error)}</div>:null}
    <div className="booking-fields"><label>Dịch vụ nha khoa<select value={serviceId} disabled={services.isLoading||services.isError} onChange={(e)=>{setServiceId(e.target.value);setSlot("");}}><option value="">Chọn dịch vụ</option>{serviceOptions.map((item)=><option key={item.id} value={item.id}>{item.name} · {money(item.price)}</option>)}</select></label>
      <label>Bác sĩ mong muốn<select value={doctorId} disabled={doctors.isLoading||doctors.isError} onChange={(e)=>{setDoctorId(e.target.value);setSlot("");}}><option value="">Chọn bác sĩ</option>{doctorOptions.map((item)=><option key={item.id} value={item.id}>{item.fullName}{item.specialty?` · ${item.specialty}`:""}</option>)}</select></label>
      <label>Ngày khám<input type="date" min={minDate} value={date} onChange={(e)=>{setDate(e.target.value);setSlot("");}} /></label></div>
    <div className="booking-step second"><span>02</span><div><strong>Chọn thời gian</strong><small>Các khung giờ còn trống trong ngày</small></div></div>
    {!doctorId||!serviceId||!date?<div className="slot-empty">Vui lòng chọn dịch vụ, bác sĩ và ngày khám để xem khung giờ.</div>:slots.isLoading?<div className="loading-line">Đang kiểm tra lịch trống...</div>:slots.isError?<div className="error-state">{apiError(slots.error)}</div>:slots.data?.slots.length?<div className="slot-grid">{slots.data.slots.map((item)=><button type="button" className={`slot-button ${slot===item.start?"selected":""}`} key={item.start} onClick={()=>setSlot(item.start)}><Clock3 size={15}/>{formatClinicTime(item.start)}</button>)}</div>:<div className="slot-empty">Ngày này chưa có khung giờ phù hợp. Vui lòng chọn ngày khác.</div>}
    <label className="symptom-label">Triệu chứng hoặc lưu ý<textarea value={symptoms} maxLength={2000} onChange={(e)=>setSymptoms(e.target.value)} placeholder="Mô tả ngắn tình trạng răng miệng để bác sĩ chuẩn bị tốt hơn..." rows={4}/><small>{symptoms.length}/2000 ký tự</small></label>
    {user?<button className="button button-primary button-full booking-submit" disabled={!slot||((user.role==="RECEPTIONIST"||user.role==="ADMIN")&&!patientId)||booking.isPending} onClick={()=>booking.mutate()}>{booking.isPending?"Đang gửi yêu cầu...":"Gửi yêu cầu đặt lịch"} <ArrowUpRight size={17}/></button>
      :<div className="booking-login-prompt"><p>Đăng nhập hoặc tạo tài khoản miễn phí để gửi yêu cầu đặt lịch. Thông tin bạn đã chọn sẽ được giữ lại.</p><button className="button button-primary button-full" disabled={!slot} onClick={()=>navigate("/login",{state:{returnTo:publicPage?"/book":"/app/book"}})}>Đăng nhập để xác nhận lịch <ArrowUpRight size={17}/></button><Link to="/register" state={{returnTo:publicPage?"/book":"/app/book"}}>Chưa có tài khoản? Đăng ký</Link></div>}
    <p className="booking-notice"><ShieldCheck size={15}/> Yêu cầu sẽ ở trạng thái chờ xác nhận; chúng tôi sẽ thông báo khi phòng khám tiếp nhận.</p>
  </section><aside className="booking-summary"><span className="eyebrow">THÔNG TIN ĐẶT LỊCH</span><h3>Tóm tắt lịch khám</h3><div className="summary-line"><span>Dịch vụ</span><strong>{chosenService?.name??"Chưa chọn"}</strong></div><div className="summary-line"><span>Bác sĩ</span><strong>{chosenDoctor?.fullName??"Chưa chọn"}</strong></div><div className="summary-line"><span>Ngày khám</span><strong>{date?new Intl.DateTimeFormat("vi-VN",{dateStyle:"long",timeZone:"UTC"}).format(new Date(`${date}T12:00:00Z`)):"Chưa chọn"}</strong></div>  <div className="summary-line"><span>Thời gian</span><strong>{slot??"Chưa chọn"}</strong></div><div className="summary-total"><span>Chi phí dự kiến</span><strong>{chosenService?money(chosenService.price):"—"}</strong></div><div className="summary-support"><MessageCircle size={18}/><span><strong>Cần hỗ trợ đặt lịch?</strong><small>Liên hệ lễ tân phòng khám để được hỗ trợ.</small></span></div></aside></div></></div>;
}
export function RecordsPage() {
  const {user}=useAuth();const toast=useToast();const cache=useQueryClient();
  const query=useQuery({queryKey:["medical-records"],queryFn:clinicApi.medicalRecords});
  const today=new Date().toLocaleDateString("sv-SE");
  const [appointmentDate,setAppointmentDate]=useState(today);
  const appointments=useQuery({queryKey:["appointments",appointmentDate],queryFn:()=>clinicApi.appointments({date:appointmentDate}),enabled:user?.role==="DOCTOR"});
  const [record,setRecord]=useState({appointmentId:"",diagnosis:"",treatment:"",prescription:"",notes:"",followUpDate:""});
  const save=useMutation({mutationFn:()=>clinicApi.createMedicalRecord({appointmentId:Number(record.appointmentId),diagnosis:record.diagnosis,treatment:record.treatment,prescription:record.prescription,notes:record.notes,...(record.followUpDate?{followUpDate:record.followUpDate}:{})}),onSuccess:()=>{toast("Đã lưu hồ sơ điều trị");setRecord({appointmentId:"",diagnosis:"",treatment:"",prescription:"",notes:"",followUpDate:""});void cache.invalidateQueries({queryKey:["medical-records"]});},onError:(error)=>toast(apiError(error),"error")});
  return <><PageHeading eyebrow="HỒ SƠ SỨC KHỎE" title="Hồ sơ điều trị" subtitle={user?.role==="DOCTOR"?"Ghi nhận kết quả khám và kế hoạch chăm sóc cho bệnh nhân.":"Lịch sử thăm khám và kế hoạch điều trị của bạn."}/>
    {user?.role==="DOCTOR"&&<section className="content-card"><div className="card-title-row"><div><h2>Tạo hồ sơ sau buổi khám</h2><p>Chỉ lịch hẹn đã hoàn tất của bác sĩ đang đăng nhập mới được chọn.</p></div></div><form className="record-entry-form" onSubmit={(e)=>{e.preventDefault();save.mutate();}}><label>Ngày khám<input type="date" max={today} value={appointmentDate} onChange={(e)=>{setAppointmentDate(e.target.value);setRecord({...record,appointmentId:""});}}/></label><label>Lịch hẹn đã hoàn tất<select required value={record.appointmentId} onChange={(e)=>setRecord({...record,appointmentId:e.target.value})}><option value="">Chọn bệnh nhân / lịch hẹn</option>{(appointments.data??[]).filter((item)=>item.status==="COMPLETED").map((item)=><option value={item.id} key={item.id}>{item.patientName} · {dateLabel(item.startAt)} · #{item.id}</option>)}</select></label><label>Chẩn đoán<textarea required rows={2} maxLength={2000} value={record.diagnosis} onChange={(e)=>setRecord({...record,diagnosis:e.target.value})}/></label><label>Điều trị<textarea rows={2} maxLength={4000} value={record.treatment} onChange={(e)=>setRecord({...record,treatment:e.target.value})}/></label><div className="two-fields"><label>Đơn thuốc / hướng dẫn<input maxLength={3000} value={record.prescription} onChange={(e)=>setRecord({...record,prescription:e.target.value})}/></label><label>Ngày tái khám<input type="date" min={new Date(Date.now()+86400000).toLocaleDateString("sv-SE")} value={record.followUpDate} onChange={(e)=>setRecord({...record,followUpDate:e.target.value})}/></label></div><label>Ghi chú<textarea rows={2} maxLength={3000} value={record.notes} onChange={(e)=>setRecord({...record,notes:e.target.value})}/></label><button className="button button-primary" disabled={save.isPending||appointments.isLoading}>{save.isPending?"Đang lưu...":"Lưu hồ sơ điều trị"}</button>{appointments.isError&&<span className="field-error">{apiError(appointments.error)}</span>}</form></section>}
    <section className="content-card">{query.isLoading?<div className="loading-line">Đang tải hồ sơ...</div>:query.isError?<div className="error-state">{apiError(query.error)}</div>:!query.data?.length?<div className="empty-state"><div><HeartPulse/></div><strong>Chưa có hồ sơ điều trị</strong><span>Hồ sơ được cập nhật sau khi bác sĩ hoàn tất buổi khám.</span></div>:<div className="record-list">{query.data.map((item:MedicalRecord)=><article className="record-card" key={item.id}><div className="record-date"><strong>{new Date(`${item.examinationDate}T12:00:00`).getDate()}</strong><span>TH.{new Date(`${item.examinationDate}T12:00:00`).getMonth()+1}</span></div><div className="record-main"><div className="record-top"><div><span className="eyebrow">MÃ HỒ SƠ #{String(item.id).padStart(5,"0")}</span><h3>{item.diagnosis??"Khám nha khoa"}</h3></div><span className="status-pill completed"><i/>Hoàn tất</span></div><p><strong>Bác sĩ:</strong> {item.doctorName} <span>·</span> <strong>Triệu chứng:</strong> {item.symptoms??"—"}</p><div className="record-details">{item.treatment&&<div><strong>Điều trị</strong><span>{item.treatment}</span></div>}{item.prescription&&<div><strong>Đơn thuốc</strong><span>{item.prescription}</span></div>}{item.notes&&<div><strong>Ghi chú</strong><span>{item.notes}</span></div>}</div>{item.followUpDate&&<div className="followup-tag"><CalendarDays size={15}/> Tái khám dự kiến: {new Date(`${item.followUpDate}T12:00:00`).toLocaleDateString("vi-VN")}</div>}</div></article>)}</div>}</section></>;
}
export function FollowUpsPage() {
  const query=useQuery({queryKey:["follow-ups"],queryFn:clinicApi.followUps});
  return <><PageHeading eyebrow="KẾ HOẠCH CHĂM SÓC TIẾP THEO" title="Lịch tái khám" subtitle="Theo dõi lời dặn và các lịch hẹn chăm sóc tiếp theo."/><section className="content-card">
    {query.isLoading?<div className="loading-line">Đang tải lịch tái khám...</div>:query.isError?<div className="error-state">{apiError(query.error)}</div>:!query.data?.length?<div className="empty-state"><div><CalendarDays/></div><strong>Chưa có lịch tái khám</strong><span>Bác sĩ sẽ gửi đề xuất tái khám sau buổi điều trị khi cần thiết.</span></div>:<div className="record-list">{query.data.map((item:FollowUp)=><article className="record-card" key={item.id}><div className="record-date"><CalendarDays size={18}/></div><div className="record-main"><div className="record-top"><div><span className="eyebrow">LỊCH TÁI KHÁM · #{String(item.id).padStart(5,"0")}</span><h3>{item.reason}</h3></div><span className={`status-pill ${item.status==="PLANNED"?"pending":"confirmed"}`}><i/>{item.status==="PLANNED"?"Đã lên kế hoạch":item.status}</span></div><p><strong>Thời gian:</strong> {dateLabel(item.scheduledAt)} <span>·</span> <strong>Bác sĩ:</strong> {item.doctorName}</p>{item.notes&&<div className="record-details"><div><strong>Ghi chú</strong><span>{item.notes}</span></div></div>}<span className="followup-tag">Liên kết hồ sơ khám #{item.previousAppointmentId}</span></div></article>)}</div>}
  </section></>;
}
export function ProfilePage() {
  const {user,updateUser}=useAuth();const toast=useToast();
  const profile=useQuery({queryKey:["my-profile"],queryFn:clinicApi.myProfile});
  const [form,setForm]=useState({fullName:user?.fullName??"",phone:user?.phone??"",avatarUrl:user?.avatarUrl??""});
  const mutation=useMutation({mutationFn:()=>clinicApi.updateProfile(form),onSuccess:(updated)=>{updateUser(updated);toast("Đã cập nhật thông tin cá nhân");},onError:(error)=>toast(apiError(error),"error")});
  const query=useQueryClient();
  const upload=useMutation({mutationFn:clinicApi.uploadAvatar,onSuccess:(updated)=>{updateUser(updated);setForm(current=>({...current,avatarUrl:updated.avatarUrl??""}));query.setQueryData(["my-profile"],updated);toast("Đã tải ảnh đại diện lên");},onError:(error)=>toast(apiError(error),"error")});
  return <><PageHeading eyebrow="TÀI KHOẢN" title="Thông tin cá nhân" subtitle="Cập nhật thông tin liên hệ trong hồ sơ DentalCare."/><section className="content-card profile-card">{profile.isError&&<div className="error-state">{apiError(profile.error)}</div>}<div className="profile-avatar">{form.avatarUrl?<img src={mediaUrl(form.avatarUrl)} alt="Ảnh đại diện"/>:<span>{form.fullName.slice(0,1).toUpperCase()}</span>}</div><form className="profile-form" onSubmit={(e)=>{e.preventDefault();mutation.mutate();}}><label>Họ và tên<input required maxLength={120} value={form.fullName} onChange={(e)=>setForm({...form,fullName:e.target.value})}/></label><label>Email tài khoản<input value={profile.data?.email??user?.email??""} disabled/></label><label>Số điện thoại<input maxLength={30} value={form.phone} onChange={(e)=>setForm({...form,phone:e.target.value})} placeholder="090 123 4567"/></label><label>Tải ảnh đại diện (PNG/JPEG, tối đa 5 MB)<input type="file" accept="image/png,image/jpeg" disabled={upload.isPending} onChange={(e)=>{const file=e.currentTarget.files?.[0];if(file)upload.mutate(file);e.currentTarget.value="";}}/></label>{upload.isPending&&<p>Đang tải ảnh lên...</p>}<label>Hoặc dùng đường dẫn ảnh<input maxLength={500} type="url" value={form.avatarUrl} onChange={(e)=>setForm({...form,avatarUrl:e.target.value})} placeholder="https://..."/></label><p>Ảnh tải lên được xác minh định dạng và kích thước trước khi lưu.</p><button className="button button-primary" disabled={mutation.isPending||upload.isPending}>{mutation.isPending?"Đang lưu...":"Lưu thay đổi"}</button></form></section></>;
}
export function AssistantPage() {
  const [symptoms,setSymptoms]=useState("");const [externalConsent,setExternalConsent]=useState(false);const[result,setResult]=useState<{possible_concern:string;suggested_service:string;explanation:string;note:string}|null>(null);const toast=useToast();
  const mutation=useMutation({mutationFn:(value:string)=>clinicApi.analyzeSymptoms(value,externalConsent),onSuccess:setResult,onError:(error)=>toast(apiError(error),"error")});
  return <><PageHeading eyebrow="TRỢ LÝ SỨC KHỎE AI" title="Cùng bạn hiểu rõ hơn" subtitle="Chia sẻ triệu chứng để nhận thông tin tham khảo trước khi gặp nha sĩ."/><div className="assistant-grid"><section className="content-card assistant-form"><div className="assistant-intro"><span className="assistant-icon"><Sparkles size={22}/></span><div><strong>Trợ lý nha khoa DentalCare</strong><p>Thông tin tham khảo bằng AI — luôn được rà soát theo nguyên tắc an toàn.</p></div></div><label>Mô tả tình trạng của bạn<textarea rows={7} maxLength={4000} value={symptoms} onChange={(e)=>setSymptoms(e.target.value)} placeholder="Ví dụ: Răng hàm bên trái bị ê buốt khi uống đồ lạnh khoảng một tuần nay..." /></label><label className="ai-data-consent"><input type="checkbox" checked={externalConsent} onChange={(e)=>setExternalConsent(e.target.checked)} /><span>Tôi đồng ý gửi nội dung triệu chứng đến Google Gemini (dịch vụ AI bên ngoài DentalCare) để phân tích. Với gói miễn phí, Google có thể sử dụng nội dung gửi lên để cải thiện sản phẩm. Tôi sẽ không nhập tên, số điện thoại hoặc thông tin nhận dạng.</span></label><div className="assistant-form-bottom"><span>{symptoms.length}/4000</span><button className="button button-primary" disabled={symptoms.trim().length<5||!externalConsent||mutation.isPending} onClick={()=>mutation.mutate(symptoms)}><Sparkles size={17}/>{mutation.isPending?"Đang phân tích...":"Phân tích triệu chứng"}</button></div><div className="ai-disclaimer"><ShieldCheck size={17}/><p>AI không thể chẩn đoán bệnh. Nếu đau dữ dội, sưng mặt/cổ, khó thở/nuốt hoặc chảy máu không cầm, hãy tìm cơ sở cấp cứu ngay. Khi có dấu hiệu khẩn cấp, hệ thống sẽ bỏ qua AI bên ngoài.</p></div></section>
    <aside className={`assistant-result ${result?"has-result":""}`}><div className="assistant-result-header"><span><Sparkles size={17}/></span><div><strong>Gợi ý tham khảo</strong><small>Thông tin hỗ trợ sức khỏe</small></div></div>{result?<div className="ai-answer"><div><span className="eyebrow">VẤN ĐỀ CÓ THỂ LIÊN QUAN</span><h3>{result.possible_concern}</h3><p>{result.explanation}</p></div><div className="suggested-service"><span>DỊCH VỤ ĐƯỢC GỢI Ý</span><strong>{result.suggested_service}</strong><Link to="/app/book">Đặt lịch khám <ArrowUpRight size={15}/></Link></div><div className="ai-disclaimer"><ShieldCheck size={17}/><p>{result.note}</p></div></div>:<div className="assistant-placeholder"><span className="placeholder-spark"><Sparkles size={25}/></span><strong>Bắt đầu từ những gì bạn cảm nhận</strong><p>Mô tả triệu chứng ở khung bên cạnh. Trợ lý sẽ đưa ra gợi ý tham khảo và dịch vụ phù hợp.</p><div className="example-chip">“Răng ê buốt khi uống nước lạnh”</div></div>}</aside></div></>;
}
const roleNames={PATIENT:"Bệnh nhân",RECEPTIONIST:"Lễ tân",DOCTOR:"Bác sĩ",ADMIN:"Quản trị viên"} as const;
export function ChatPage() {
  const {user}=useAuth();
  const [searchParams,setSearchParams]=useSearchParams();
  const toast=useToast();
  const cache=useQueryClient();
  const [search,setSearch]=useState("");
  const requestedContactId=Number(searchParams.get("contactId"));
  const routeContactId=Number.isSafeInteger(requestedContactId)&&requestedContactId>0?requestedContactId:null;
  const routeContactName=searchParams.get("contactName")?.trim()||null;
  const [selectedId,setSelectedId]=useState<number|null>(routeContactId);
  const [socketConnected,setSocketConnected]=useState(false);
  const [content,setContent]=useState("");
  const messageEnd=useRef<HTMLDivElement>(null);
  const selectedIdRef=useRef(selectedId);
  selectedIdRef.current=selectedId;
  const contacts=useQuery({queryKey:["chat-contacts",search],queryFn:()=>clinicApi.chatContacts(search),refetchInterval:5000});
  const selected=contacts.data?.find((contact)=>contact.id===selectedId)??null;
  const messages=useQuery({queryKey:["chat-messages",selectedId],queryFn:()=>clinicApi.chatMessages(selectedId!),enabled:selectedId!==null,refetchInterval:socketConnected?false:5000});
  const markRead=useMutation({mutationFn:clinicApi.markChatRead,onSuccess:()=>void cache.invalidateQueries({queryKey:["chat-contacts"]}),onError:(error)=>toast(apiError(error),"error")});
  const send=useMutation({mutationFn:()=>clinicApi.sendChatMessage(selectedId!,content),onSuccess:()=>{setContent("");void cache.invalidateQueries({queryKey:["chat-messages",selectedId]});void cache.invalidateQueries({queryKey:["chat-contacts"]});void cache.invalidateQueries({queryKey:["notifications"]});},onError:(error)=>toast(apiError(error),"error")});
  useEffect(()=>{
    const token=localStorage.getItem("dentalcare.access");
    if(!token||!user)return;
    const socketUrl=new URL(api.defaults.baseURL??"/api",window.location.origin);
    socketUrl.protocol=socketUrl.protocol==="https:"?"wss:":"ws:";
    socketUrl.pathname="/ws/chat";
    socketUrl.search="";
    const client=new Client({
      brokerURL:socketUrl.toString(),
      connectHeaders:{Authorization:`Bearer ${token}`},
      beforeConnect:()=>{
        const currentToken=localStorage.getItem("dentalcare.access");
        client.connectHeaders=currentToken?{Authorization:`Bearer ${currentToken}`}:{};
      },
      reconnectDelay:3000,
      heartbeatIncoming:10000,
      heartbeatOutgoing:10000,
      onConnect:()=>{
        setSocketConnected(true);
        client.subscribe("/user/queue/messages",(frame)=>{
          let incoming:ChatMessage;
          try{incoming=JSON.parse(frame.body) as ChatMessage;}
          catch{toast("Không thể đọc tin nhắn realtime từ máy chủ.","error");return;}
          const contactId=incoming.senderId===user.id?incoming.recipientId:incoming.senderId;
          cache.setQueryData<ChatMessage[]>(["chat-messages",contactId],(current)=>{
            if(!current||current.some((message)=>message.id===incoming.id))return current;
            return [...current,incoming].sort((a,b)=>a.createdAt.localeCompare(b.createdAt));
          });
          void cache.invalidateQueries({queryKey:["chat-messages",contactId]});
          void cache.invalidateQueries({queryKey:["chat-contacts"]});
          void cache.invalidateQueries({queryKey:["notifications"]});
          if(incoming.recipientId===user.id&&selectedIdRef.current===contactId){
            void clinicApi.markChatRead(contactId)
              .then(()=>cache.invalidateQueries({queryKey:["chat-contacts"]}))
              .catch((error)=>toast(apiError(error),"error"));
          }
        });
      },
      onWebSocketClose:()=>setSocketConnected(false),
      onWebSocketError:()=>setSocketConnected(false),
      onStompError:()=>{setSocketConnected(false);toast("Kết nối tin nhắn realtime bị từ chối. Vui lòng đăng nhập lại.","error");},
    });
    client.activate();
    return ()=>{setSocketConnected(false);void client.deactivate();};
  },[cache,toast,user]);
  useEffect(()=>{
    if(routeContactId!==null&&selectedId!==routeContactId)setSelectedId(routeContactId);
    else if(routeContactId===null&&routeContactName&&contacts.data){
      const sender=contacts.data.find(contact=>contact.fullName.trim().toLocaleLowerCase("vi-VN")===routeContactName.toLocaleLowerCase("vi-VN"));
      if(sender){
        setSelectedId(sender.id);
        setSearchParams({contactId:String(sender.id)},{replace:true});
      }else setSelectedId(null);
    }else if(routeContactId===null&&!routeContactName&&selectedId===null&&contacts.data?.length){
      setSelectedId(contacts.data[0].id);
      setSearchParams({contactId:String(contacts.data[0].id)},{replace:true});
    }
  },[contacts.data,routeContactId,routeContactName,selectedId,setSearchParams]);
  useEffect(()=>{if(selectedId!==null&&selected?.unreadCount)markRead.mutate(selectedId);},[selectedId,selected?.unreadCount]);
  useEffect(()=>{messageEnd.current?.scrollIntoView({behavior:"smooth"});},[messages.data]);
  const submit=(event:FormEvent)=>{event.preventDefault();if(selectedId!==null&&content.trim())send.mutate();};
  return <><PageHeading eyebrow="LIÊN LẠC NỘI BỘ" title="Tin nhắn" subtitle="Trao đổi trực tiếp, an toàn với bệnh nhân và đội ngũ phòng khám."/><section className="chat-workspace">
    <aside className="chat-contacts"><div className="chat-contacts-header"><strong>Hội thoại</strong><span>{contacts.data?.length??0}</span></div><label className="chat-search"><Search size={16}/><input value={search} onChange={(event)=>setSearch(event.target.value)} placeholder="Tìm người liên hệ"/></label>
      <div className="chat-contact-list">{contacts.isPending?<p className="chat-empty">Đang tải liên hệ...</p>:contacts.isError?<p className="chat-empty error-state">Không thể tải danh sách liên hệ. {apiError(contacts.error)}</p>:contacts.data?.length?contacts.data.map((contact:ChatContact)=><button type="button" key={contact.id} className={`chat-contact ${selectedId===contact.id?"active":""}`} onClick={()=>{setSelectedId(contact.id);setSearchParams({contactId:String(contact.id)},{replace:true});}}><span className="chat-avatar">{contact.avatarUrl?<img src={mediaUrl(contact.avatarUrl)} alt=""/>:contact.fullName.slice(0,1).toUpperCase()}</span><span className="chat-contact-copy"><strong>{contact.fullName}</strong><small>{roleNames[contact.role]}</small><span>{contact.lastMessage??"Bắt đầu cuộc trò chuyện"}</span></span>{contact.unreadCount>0&&<i className="chat-unread">{contact.unreadCount}</i>}</button>):<p className="chat-empty">Không tìm thấy người liên hệ phù hợp.</p>}</div>
    </aside>
    <div className="chat-thread">{selected?<><header className="chat-thread-header"><span className="chat-avatar">{selected.avatarUrl?<img src={mediaUrl(selected.avatarUrl)} alt=""/>:selected.fullName.slice(0,1).toUpperCase()}</span><div><strong>{selected.fullName}</strong><small>{roleNames[selected.role]}</small></div><span className="chat-secure"><ShieldCheck size={15}/> {socketConnected?"Đang kết nối realtime":"Kết nối dự phòng"}</span></header>
      <div className="chat-messages">{messages.isPending?<p className="chat-empty">Đang tải tin nhắn...</p>:messages.isError?<p className="chat-empty error-state">Không thể tải cuộc trò chuyện. {apiError(messages.error)}</p>:messages.data?.length?messages.data.map((message:ChatMessage)=><article key={message.id} className={`chat-bubble ${message.senderId===user?.id?"mine":"theirs"}`}><p>{message.content}</p><time dateTime={message.createdAt}>{new Intl.DateTimeFormat("vi-VN",{hour:"2-digit",minute:"2-digit"}).format(new Date(message.createdAt))}</time></article>):<div className="chat-first-message"><MessageCircle size={24}/><strong>Chưa có tin nhắn</strong><span>Gửi lời chào để bắt đầu trao đổi với {selected.fullName}.</span></div>}<div ref={messageEnd}/></div>
      <form className="chat-compose" onSubmit={submit}><input aria-label="Nội dung tin nhắn" maxLength={4000} value={content} onChange={(event)=>setContent(event.target.value)} placeholder={`Nhắn tin cho ${selected.fullName}...`}/><button className="button button-primary" disabled={!content.trim()||send.isPending} aria-label="Gửi tin nhắn"><MessageCircle size={16}/>{send.isPending?"Đang gửi":"Gửi"}</button></form>
    </>:<div className="chat-no-selection"><MessageCircle size={30}/><strong>Chọn một người để trò chuyện</strong><span>Tin nhắn được lưu an toàn trong hệ thống DentalCare.</span></div>}</div>
  </section></>;
}
const adminTabs=[["services","Dịch vụ"],["doctors","Bác sĩ & lịch"],["profiles","Duyệt hồ sơ bác sĩ"],["users","Người dùng"]] as const;
export function AdminPage() {
  const [tab,setTab]=useState<(typeof adminTabs)[number][0]>("services");
  const [search,setSearch]=useState("");const [editId,setEditId]=useState<number|null>(null);
  const [service,setService]=useState({name:"",category:"",price:"",durationMinutes:"30",description:""});
  const [specialty,setSpecialty]=useState({name:"",description:""});
  const [schedule,setSchedule]=useState({doctorId:"",dayOfWeek:"MONDAY",startTime:"08:30",endTime:"17:00",breakStart:"12:00",breakEnd:"13:00"});
  const [block,setBlock]=useState({doctorId:"",startAt:"",endAt:"",reason:""});
  const toast=useToast();const query=useQueryClient();
  const services=useQuery({queryKey:["admin-services"],queryFn:clinicApi.adminServices,enabled:tab==="services"});
  const doctors=useQuery({queryKey:["public-doctors"],queryFn:clinicApi.doctors,enabled:tab==="doctors"});
  const blocks=useQuery({queryKey:["doctor-unavailabilities",block.doctorId],queryFn:()=>clinicApi.doctorUnavailabilities(Number(block.doctorId)),enabled:tab==="doctors"&&Boolean(block.doctorId)});
  const users=useQuery({queryKey:["admin-users",search],queryFn:()=>clinicApi.adminUsers(search),enabled:tab==="users"});
  const refresh=()=>{void query.invalidateQueries({queryKey:["admin-services"]});void query.invalidateQueries({queryKey:["public-services"]});void query.invalidateQueries({queryKey:["public-doctors"]});void query.invalidateQueries({queryKey:["public-specialties"]});void query.invalidateQueries({queryKey:["admin-users"]});};
  const reportError=(error:unknown)=>toast(apiError(error),"error");
  const saveService=useMutation({mutationFn:()=>{const payload={...service,price:Number(service.price),durationMinutes:Number(service.durationMinutes)};return editId?clinicApi.updateService(editId,payload):clinicApi.createService(payload);},onSuccess:()=>{toast(editId?"Đã cập nhật dịch vụ":"Đã thêm dịch vụ");setEditId(null);setService({name:"",category:"",price:"",durationMinutes:"30",description:""});refresh();},onError:reportError});
  const toggleService=useMutation({mutationFn:({id,active}:{id:number;active:boolean})=>clinicApi.setServiceActive(id,active),onSuccess:refresh,onError:reportError});
  const addSpecialty=useMutation({mutationFn:()=>clinicApi.createSpecialty(specialty),onSuccess:()=>{toast("Đã thêm chuyên khoa");setSpecialty({name:"",description:""});refresh();},onError:reportError});
  const saveSchedule=useMutation({mutationFn:()=>clinicApi.setSchedule({...schedule,doctorId:Number(schedule.doctorId),active:true}),onSuccess:()=>toast("Đã lưu lịch làm việc"),onError:reportError});
  const saveBlock=useMutation({mutationFn:()=>clinicApi.blockDoctorTime({...block,doctorId:Number(block.doctorId),startAt:new Date(block.startAt).toISOString(),endAt:new Date(block.endAt).toISOString()}),onSuccess:()=>{toast("Đã ghi nhận thời gian bác sĩ không nhận lịch");void query.invalidateQueries({queryKey:["doctor-unavailabilities",block.doctorId]});},onError:reportError});
  const role=useMutation({mutationFn:({id,value}:{id:number;value:string})=>clinicApi.setUserRole(id,value),onSuccess:()=>{toast("Đã cập nhật vai trò người dùng");refresh();},onError:reportError});
  const active=useMutation({mutationFn:({id,value}:{id:number;value:boolean})=>clinicApi.setUserActive(id,value),onSuccess:()=>{toast("Đã cập nhật trạng thái tài khoản");refresh();},onError:reportError});
  const submit=(event:FormEvent,mutation:{mutate:()=>void})=>{event.preventDefault();mutation.mutate();};
  return <><PageHeading eyebrow="QUẢN TRỊ DENTALCARE" title="Trung tâm quản trị" subtitle="Quản lý tài khoản và phân quyền, cấu hình vận hành phòng khám, duyệt hồ sơ bác sĩ trước khi công khai."/>
    <div className="admin-tabs">{adminTabs.map(([id,label])=><button className={tab===id?"active":""} key={id} onClick={()=>setTab(id)}>{label}</button>)}</div>
    {tab==="services"&&<div className="management-grid"><section className="content-card management-form"><div className="card-title-row"><div><h2>{editId?"Cập nhật dịch vụ":"Thêm dịch vụ mới"}</h2><p>Giá dịch vụ được ghi nhận tại thời điểm khách đặt lịch.</p></div></div><form onSubmit={(e)=>submit(e,saveService)} className="admin-form"><label>Tên dịch vụ<input required maxLength={120} value={service.name} onChange={(e)=>setService({...service,name:e.target.value})}/></label><label>Danh mục<input maxLength={80} value={service.category} onChange={(e)=>setService({...service,category:e.target.value})}/></label><div className="two-fields"><label>Giá (VND)<input type="number" min="1" required value={service.price} onChange={(e)=>setService({...service,price:e.target.value})}/></label><label>Thời lượng (phút)<input type="number" min="15" max="240" step="15" required value={service.durationMinutes} onChange={(e)=>setService({...service,durationMinutes:e.target.value})}/></label></div><label>Mô tả<textarea rows={3} maxLength={2000} value={service.description} onChange={(e)=>setService({...service,description:e.target.value})}/></label><button className="button button-primary" disabled={saveService.isPending}>{editId?"Lưu thay đổi":"Thêm dịch vụ"}</button>{editId&&<button type="button" className="button button-quiet" onClick={()=>{setEditId(null);setService({name:"",category:"",price:"",durationMinutes:"30",description:""});}}>Hủy sửa</button>}</form></section>
      <section className="content-card management-list"><div className="card-title-row"><div><h2>Danh sách dịch vụ</h2><p>{services.data?.length??0} dịch vụ đã khai báo</p></div></div>{services.isError?<div className="error-state">{apiError(services.error)}</div>:services.isLoading?<div className="loading-line">Đang tải dịch vụ...</div>:<div className="admin-item-list">{(services.data??[]).map((item:DentalService)=><article key={item.id}><span className="admin-item-icon"><HeartPulse size={17}/></span><div><strong>{item.name}</strong><small>{money(item.price)} · {item.durationMinutes} phút · {item.category}</small></div><span className={`status-pill ${item.active?"confirmed":"cancelled"}`}><i/>{item.active?"Đang mở":"Đã ẩn"}</span><button className="table-action" onClick={()=>{setEditId(item.id);setService({name:item.name,category:item.category??"",price:String(item.price),durationMinutes:String(item.durationMinutes),description:item.description??""});}}>Sửa</button><button className="table-action" onClick={()=>toggleService.mutate({id:item.id,active:!item.active})}>{item.active?"Ẩn":"Mở"}</button></article>)}</div>}</section></div>}
    {tab==="doctors"&&<div className="management-grid"><section className="content-card management-form"><div className="card-title-row"><div><h2>Danh mục chuyên khoa</h2><p>Quản lý chuyên môn phòng khám; bác sĩ tự gửi hồ sơ và quản trị viên duyệt trước khi công khai.</p></div></div><form className="admin-form" onSubmit={(e)=>submit(e,addSpecialty)}><label>Tên chuyên khoa<input required maxLength={100} value={specialty.name} onChange={(e)=>setSpecialty({...specialty,name:e.target.value})}/></label><label>Mô tả<input maxLength={500} value={specialty.description} onChange={(e)=>setSpecialty({...specialty,description:e.target.value})}/></label><button className="button button-outline" disabled={addSpecialty.isPending}>Thêm chuyên khoa</button></form></section>
      <section className="content-card management-form"><div className="card-title-row"><div><h2>Lịch làm việc tuần</h2><p>Khung giờ lặp theo thứ trong tuần, theo múi giờ phòng khám.</p></div></div><form className="admin-form" onSubmit={(e)=>submit(e,saveSchedule)}><label>Bác sĩ<select required value={schedule.doctorId} onChange={(e)=>setSchedule({...schedule,doctorId:e.target.value})}><option value="">Chọn bác sĩ</option>{(doctors.data??[]).map((doctor:Doctor)=><option key={doctor.id} value={doctor.id}>{doctor.fullName}</option>)}</select></label><label>Ngày làm việc<select value={schedule.dayOfWeek} onChange={(e)=>setSchedule({...schedule,dayOfWeek:e.target.value})}>{["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"].map((day,index)=><option key={day} value={day}>{["Thứ Hai","Thứ Ba","Thứ Tư","Thứ Năm","Thứ Sáu","Thứ Bảy","Chủ nhật"][index]}</option>)}</select></label><div className="two-fields"><label>Bắt đầu ca<input required type="time" value={schedule.startTime} onChange={(e)=>setSchedule({...schedule,startTime:e.target.value})}/></label><label>Kết thúc ca<input required type="time" value={schedule.endTime} onChange={(e)=>setSchedule({...schedule,endTime:e.target.value})}/></label><label>Bắt đầu nghỉ<input type="time" value={schedule.breakStart} onChange={(e)=>setSchedule({...schedule,breakStart:e.target.value})}/></label><label>Kết thúc nghỉ<input type="time" value={schedule.breakEnd} onChange={(e)=>setSchedule({...schedule,breakEnd:e.target.value})}/></label></div><button className="button button-primary" disabled={saveSchedule.isPending}>Lưu lịch làm việc</button></form>
        <div className="doctor-directory"><h3>Bác sĩ đang hoạt động</h3>{(doctors.data??[]).map((doctor:Doctor)=><div key={doctor.id}><span className="table-avatar">{doctor.fullName.slice(0,1)}</span><span><strong>{doctor.fullName}</strong><small>{doctor.specialty??"Chưa gán chuyên khoa"} · {doctor.experienceYears} năm kinh nghiệm</small></span></div>)}</div>
        <div className="doctor-directory"><h3>Thời gian không nhận lịch</h3><p className="management-hint">Khung giờ đã chặn sẽ bị loại khỏi danh sách lịch còn trống.</p><form className="admin-form" onSubmit={(e)=>submit(e,saveBlock)}><label>Bác sĩ<select required value={block.doctorId} onChange={(e)=>setBlock({...block,doctorId:e.target.value})}><option value="">Chọn bác sĩ</option>{(doctors.data??[]).map((doctor:Doctor)=><option key={doctor.id} value={doctor.id}>{doctor.fullName}</option>)}</select></label><div className="two-fields"><label>Bắt đầu<input required type="datetime-local" value={block.startAt} onChange={(e)=>setBlock({...block,startAt:e.target.value})}/></label><label>Kết thúc<input required type="datetime-local" value={block.endAt} onChange={(e)=>setBlock({...block,endAt:e.target.value})}/></label></div><label>Lý do<input maxLength={300} value={block.reason} onChange={(e)=>setBlock({...block,reason:e.target.value})}/></label><button className="button button-outline" disabled={saveBlock.isPending}>Chặn khung giờ</button></form>{blocks.isError?<div className="error-state">{apiError(blocks.error)}</div>:blocks.data?.map((item)=><div className="unavailability-row" key={item.id}><strong>{item.doctorName}</strong><span>{dateLabel(item.startAt)} – {new Date(item.endAt).toLocaleTimeString("vi-VN",{hour:"2-digit",minute:"2-digit"})}</span><small>{item.reason??"Không nhận lịch"}</small></div>)}</div></section></div>}
    {tab==="profiles"&&<AdminProfileReviewPage/>}
    {tab==="users"&&<section className="content-card"><div className="filter-row"><label className="search-field"><Search size={17}/><input value={search} onChange={(e)=>setSearch(e.target.value)} placeholder="Tìm theo tên hoặc email"/></label></div>{users.isLoading?<div className="loading-line">Đang tải người dùng...</div>:users.isError?<div className="error-state">{apiError(users.error)}</div>:<div className="table-wrap"><table className="data-table"><thead><tr><th>TÀI KHOẢN</th><th>EMAIL</th><th>SỐ ĐIỆN THOẠI</th><th>VAI TRÒ</th><th>TRẠNG THÁI</th></tr></thead><tbody>{(users.data??[]).map((item:AdminUser)=><tr key={item.id}><td><strong>{item.fullName}</strong></td><td>{item.email}</td><td>{item.phone??"—"}</td><td><select className="role-select" value={item.role} disabled={role.isPending} onChange={(e)=>role.mutate({id:item.id,value:e.target.value})}>{["PATIENT","RECEPTIONIST","DOCTOR","ADMIN"].map((entry)=><option key={entry}>{entry}</option>)}</select></td><td><button className={`status-pill ${item.active?"confirmed":"cancelled"}`} disabled={active.isPending} onClick={()=>active.mutate({id:item.id,value:!item.active})}><i/>{item.active?"Hoạt động · khóa":"Đã khóa · mở"}</button></td></tr>)}</tbody></table></div>}</section>}
  </>;
}
