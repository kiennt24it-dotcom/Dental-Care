import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BadgeCheck, CircleAlert, Clock3, UserRoundCheck } from "lucide-react";
import { apiError, mediaUrl } from "../api/client";
import { clinicApi } from "../api/clinic";
import { useToast } from "../components/Toast";
import type { DoctorProfileDraft } from "../types";

export function AdminProfileReviewPage() {
  const toast=useToast();
  const cache=useQueryClient();
  const [notes,setNotes]=useState<Record<number,string>>({});
  const submissions=useQuery({queryKey:["pending-doctor-profiles"],queryFn:clinicApi.pendingDoctorProfiles});
  const review=useMutation({
    mutationFn:({doctorId,decision}:{doctorId:number;decision:"APPROVE"|"REJECT"})=>
      clinicApi.reviewDoctorProfile(doctorId,decision,notes[doctorId]?.trim()??""),
    onSuccess:async result=>{
      toast(result.status==="APPROVED"?"Hồ sơ bác sĩ đã được duyệt và công khai":"Đã trả hồ sơ cho bác sĩ chỉnh sửa");
      await cache.invalidateQueries({queryKey:["pending-doctor-profiles"]});
      await cache.invalidateQueries({queryKey:["public-doctors"]});
    },
    onError:error=>toast(apiError(error),"error"),
  });
  return <section className="profile-review-queue">
    <header className="card-title-row"><div><h2>Hồ sơ bác sĩ chờ duyệt</h2><p>Kiểm tra bằng cấp và thành tích trước khi công khai với bệnh nhân.</p></div><span className="review-queue-count"><Clock3 size={15}/>{submissions.data?.length??0} chờ duyệt</span></header>
    {submissions.isPending?<div className="loading-line">Đang tải hồ sơ chờ duyệt...</div>
      :submissions.isError?<div className="error-state"><CircleAlert/>Không thể tải hồ sơ. {apiError(submissions.error)}</div>
      :submissions.data?.length?<div className="review-cards">{submissions.data.map((profile:DoctorProfileDraft)=><article className="content-card review-card" key={profile.doctorId}>
        <header><span className="review-doctor-avatar">{profile.avatarUrl?<img src={mediaUrl(profile.avatarUrl)} alt=""/>:<UserRoundCheck/>}</span><div><h3>{profile.doctorName}</h3><p>{profile.specialty??"Chưa chọn chuyên khoa"} · {profile.experienceYears} năm kinh nghiệm</p></div><span className="status-pill pending"><i/>Chờ duyệt</span></header>
        <dl><div><dt>Bằng cấp / chứng chỉ</dt><dd>{profile.qualification||"Chưa khai báo"}</dd></div><div><dt>Giới thiệu</dt><dd>{profile.description||"Chưa khai báo"}</dd></div><div><dt>Thành tích</dt><dd className="achievement-lines">{profile.achievements||"Chưa khai báo"}</dd></div></dl>
        <label className="review-note-field">Ghi chú cho bác sĩ khi trả hồ sơ<textarea rows={2} maxLength={1000} value={notes[profile.doctorId]??""} onChange={event=>setNotes({...notes,[profile.doctorId]:event.target.value})} placeholder="Bắt buộc nếu từ chối; giúp bác sĩ biết cần chỉnh sửa gì."/></label>
        <div className="review-actions"><button className="button button-outline" disabled={review.isPending||!(notes[profile.doctorId]??"").trim()} onClick={()=>review.mutate({doctorId:profile.doctorId,decision:"REJECT"})}>Từ chối & yêu cầu sửa</button><button className="button button-primary" disabled={review.isPending} onClick={()=>review.mutate({doctorId:profile.doctorId,decision:"APPROVE"})}><BadgeCheck size={16}/> Duyệt & công khai</button></div>
      </article>)}</div>:<div className="empty-state"><div><BadgeCheck/></div><strong>Không có hồ sơ chờ duyệt</strong><span>Các hồ sơ mới của bác sĩ sẽ xuất hiện tại đây.</span></div>}
  </section>;
}
