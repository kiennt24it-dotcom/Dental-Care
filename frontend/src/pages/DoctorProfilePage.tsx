import { useEffect, useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BadgeCheck, CircleAlert, Clock3 } from "lucide-react";
import { apiError } from "../api/client";
import { clinicApi } from "../api/clinic";
import { useToast } from "../components/Toast";
import type { DoctorProfileDraft } from "../types";
import { PageHeading } from "./RoleWorkflowComponents";

type ProfileForm = { specialtyId:string; experienceYears:string; qualification:string; description:string; achievements:string };
const emptyForm:ProfileForm={specialtyId:"",experienceYears:"0",qualification:"",description:"",achievements:""};

function profileForm(profile:DoctorProfileDraft):ProfileForm {
  return {
    specialtyId:profile.specialtyId?String(profile.specialtyId):"",
    experienceYears:String(profile.experienceYears),
    qualification:profile.qualification??"",
    description:profile.description??"",
    achievements:profile.achievements??"",
  };
}

export function DoctorProfilePage() {
  const toast=useToast();
  const cache=useQueryClient();
  const [form,setForm]=useState<ProfileForm>(emptyForm);
  const profile=useQuery({queryKey:["doctor-profile-me"],queryFn:clinicApi.myDoctorProfile});
  const specialties=useQuery({queryKey:["public-specialties"],queryFn:clinicApi.specialties});
  useEffect(()=>{if(profile.data)setForm(profileForm(profile.data));},[profile.data]);
  const save=useMutation({
    mutationFn:()=>clinicApi.submitDoctorProfile({
      specialtyId:Number(form.specialtyId),
      experienceYears:Number(form.experienceYears),
      qualification:form.qualification.trim(),
      description:form.description.trim(),
      achievements:form.achievements.trim(),
    }),
    onSuccess:async result=>{
      toast("Hồ sơ đã gửi quản trị viên duyệt");
      await cache.invalidateQueries({queryKey:["doctor-profile-me"]});
      await cache.invalidateQueries({queryKey:["public-doctors"]});
      setForm(profileForm(result));
    },
    onError:error=>toast(apiError(error),"error"),
  });
  const submit=(event:FormEvent)=>{event.preventDefault();save.mutate();};
  const status=profile.data?.status;
  return <>
    <PageHeading eyebrow="HỒ SƠ CHUYÊN MÔN" title="Hồ sơ bác sĩ của tôi" subtitle="Giới thiệu chuyên môn, kinh nghiệm và thành tích để bệnh nhân hiểu rõ người sẽ chăm sóc mình."/>
    {profile.isLoading?<div className="loading-line">Đang tải hồ sơ...</div>:profile.isError?<div className="error-state"><CircleAlert/>Không thể tải hồ sơ. {apiError(profile.error)}</div>:<>
      {status==="PENDING"&&<div className="profile-review-state pending"><Clock3/><span><strong>Hồ sơ đang chờ duyệt</strong><small>Bản đang công khai vẫn được giữ nguyên trong lúc quản trị viên xem xét.</small></span></div>}
      {status==="REJECTED"&&<div className="profile-review-state rejected"><CircleAlert/><span><strong>Hồ sơ cần chỉnh sửa</strong><small>{profile.data?.reviewNote}</small></span></div>}
      {status==="APPROVED"&&<div className="profile-review-state approved"><BadgeCheck/><span><strong>Hồ sơ đã được duyệt</strong><small>Thông tin hiện tại đang hiển thị trong danh sách bác sĩ công khai.</small></span></div>}
      <section className="content-card doctor-profile-form-card">
        <div className="card-title-row"><div><h2>Thông tin sẽ gửi duyệt</h2><p>Quản trị viên kiểm tra tính chính xác trước khi cập nhật hồ sơ công khai.</p></div></div>
        <form className="admin-form" onSubmit={submit}>
          <label>Chuyên khoa<select required value={form.specialtyId} onChange={event=>setForm({...form,specialtyId:event.target.value})}><option value="">Chọn chuyên khoa</option>{(specialties.data??[]).map(specialty=><option key={specialty.id} value={specialty.id}>{specialty.name}</option>)}</select></label>
          <label>Số năm kinh nghiệm<input type="number" min="0" max="70" required value={form.experienceYears} onChange={event=>setForm({...form,experienceYears:event.target.value})}/></label>
          <label>Bằng cấp, chứng chỉ<input maxLength={200} value={form.qualification} onChange={event=>setForm({...form,qualification:event.target.value})} placeholder="Ví dụ: Bác sĩ chuyên khoa..." /></label>
          <label>Giới thiệu chuyên môn<textarea rows={4} maxLength={2000} value={form.description} onChange={event=>setForm({...form,description:event.target.value})} placeholder="Kinh nghiệm, hướng điều trị và điều bệnh nhân nên biết..." /></label>
          <label>Thành tích, giải thưởng<textarea rows={5} maxLength={3000} value={form.achievements} onChange={event=>setForm({...form,achievements:event.target.value})} placeholder={"Mỗi thành tích một dòng; chỉ ghi thông tin có thể xác minh."} /></label>
          {specialties.isError&&<div className="error-state">{apiError(specialties.error)}</div>}
          <p className="management-hint">Thành tích được công khai đúng theo nội dung sau khi quản trị viên duyệt. Không nhập thông tin bệnh nhân.</p>
          <button className="button button-primary" disabled={save.isPending||specialties.isPending}>{save.isPending?"Đang gửi...":status==="PENDING"?"Cập nhật bản chờ duyệt":"Gửi hồ sơ để duyệt"}</button>
        </form>
      </section>
    </>}
  </>;
}
