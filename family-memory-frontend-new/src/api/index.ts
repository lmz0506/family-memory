import http from './http'
import type {UniverseOverview,UniversePage,UniverseBatch,UniverseSourceType} from '@/types'
import type {Capsule, Comment as MemoryComment, Family, FamilyMember, FamilyMilestone, FamilyMilestonePayload, Memory, PageData, TimelineMemoryStar, TimelineYearSummary, User} from '@/types'

export const authApi = {
    login: (data: { username: string; password: string }) => http.post<any, {
        token: string;
        user: User
    }>('/auth/login', data),
    register: (data: { username: string; password: string; nickname: string }) => http.post<any, {
        token: string;
        user: User
    }>('/auth/register', data),
    me: () => http.get<any, User>('/auth/me')
}
export const familyApi = {
    list: () => http.get<any, Family[]>('/families'),
    detail: (id: number) => http.get<any, Family>(`/families/${id}`),
    members: (id: number) => http.get<any, FamilyMember[]>(`/families/${id}/members`),
    create: (data: Partial<Family>) => http.post<any, Family>('/families', data),
    join: (data: {
        inviteCode: string;
        displayName?: string;
        role?: string
    }) => http.post<any, Family>('/families/join', data),
    invite: (id: number) => http.post<any, {
        code: string;
        expireAt: string;
        maxUses: number
    }>(`/families/${id}/invite-codes`, {expireHours: 48, maxUses: 5})
}
export const memoryApi = {
    list: (familyId: number, params?: Record<string, unknown>) => http.get<any, PageData<Memory>>(`/families/${familyId}/memories`, {params}),
    today: (familyId: number) => http.get<any, Memory[]>(`/families/${familyId}/memories/today-in-history`),
    years: (familyId: number) => http.get<any, number[]>(`/families/${familyId}/memories/years`),
    timeline: (familyId: number) => http.get<any, TimelineYearSummary[]>(`/families/${familyId}/memories/timeline`),
    timelineStars: (familyId: number) => http.get<any, TimelineMemoryStar[]>(`/families/${familyId}/memories/timeline/stars`),
    timelineMonth: (familyId: number, year: number, month: number) => http.get<any, Memory[]>(`/families/${familyId}/memories/timeline/${year}/${month}`),
    detail: (id: number) => http.get<any, Memory>(`/memories/${id}`),
    create: (familyId: number, data: Record<string, unknown>) => http.post<any, Memory>(`/families/${familyId}/memories`, data),
    like: (id: number) => http.post<any, { liked: boolean; likesCount: number }>(`/memories/${id}/like`),
    comment: (id: number, content: string) => http.post<any, MemoryComment>(`/memories/${id}/comments`, {content}),
    remove: (id: number) => http.delete(`/memories/${id}`)
}
export const capsuleApi = {
    list: (familyId: number) => http.get<any, Capsule[]>(`/families/${familyId}/capsules`),
    create: (familyId: number, data: Record<string, unknown>) => http.post<any, Capsule>(`/families/${familyId}/capsules`, data),
    detail: (id: number) => http.get<any, Capsule>(`/capsules/${id}`)
}
export const milestoneApi = {
    list: (familyId: number) => http.get<any, FamilyMilestone[]>(`/families/${familyId}/milestones`),
    create: (familyId: number, data: FamilyMilestonePayload) => http.post<any, FamilyMilestone>(`/families/${familyId}/milestones`, data),
    update: (id: number, data: FamilyMilestonePayload) => http.put<any, FamilyMilestone>(`/milestones/${id}`, data),
    remove: (id: number) => http.delete(`/milestones/${id}`)
}
export const uploadApi = {
    file: async (file: File, category = 'memory', progress?: (percent: number) => void) => {
        const form = new FormData();
        form.append('file', file);
        form.append('category', category);
        return http.post<any, { url: string }>('/uploads', form, {
            onUploadProgress: event => { if (event.total) progress?.(Math.min(99, Math.round(event.loaded / event.total * 100))) }
        })
    }
}

/** 星海专用轻量接口：概要只取计数，日期列表和搜索均有界分页。 */
export const universeApi = {
    year: (familyId:number,year:number,afterId=0,size=120,signal?:AbortSignal) => http.get<any,UniverseBatch>(`/families/${familyId}/universe/year`,{params:{year,afterId,size},signal}),
    overview: (familyId:number, signal?:AbortSignal) => http.get<any,UniverseOverview>(`/families/${familyId}/universe/overview`,{signal}),
    day: (familyId:number,date:string,page=1,size=24,signal?:AbortSignal) => http.get<any,UniversePage>(`/families/${familyId}/universe/day`,{params:{date,page,size},signal}),
    search: (familyId:number,keyword:string,sourceType?:UniverseSourceType,page=1,size=24,signal?:AbortSignal) => http.get<any,UniversePage>(`/families/${familyId}/universe/search`,{params:{keyword,sourceType,page,size},signal})
}
