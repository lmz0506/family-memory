import request from './request'
import type {ApiResult, PageResult} from './request'

// ==================== 认证模块 ====================

export interface LoginParams {
    username: string
    password: string
}

export interface RegisterParams {
    username: string
    password: string
    nickname: string
    phone?: string
    avatar?: string
}

export interface LoginResult {
    token: string
    user: UserInfo
}

export interface UserInfo {
    id: number
    username: string
    nickname: string
    avatar: string
    phone?: string
    currentFamily?: {
        id: number
        name: string
    }
}

export function login(data: LoginParams): Promise<ApiResult<LoginResult>> {
    return request.post('/auth/login', data)
}

export function register(data: RegisterParams): Promise<ApiResult<UserInfo>> {
    return request.post('/auth/register', data)
}

export function getCurrentUser(): Promise<ApiResult<UserInfo>> {
    return request.get('/auth/me')
}

export interface UpdateProfileParams {
    nickname?: string
    avatar?: string
    phone?: string
}

export function updateProfile(data: UpdateProfileParams): Promise<ApiResult<UserInfo>> {
    return request.put('/auth/profile', data)
}

// ==================== 家庭模块 ====================

export interface FamilyMemberResp {
    id: number
    userId: number
    nickname: string
    role: string
    avatar: string
    color: string
    isAdmin: boolean
}

export interface FamilyStatistics {
    recordDays: number
    photoCount: number
    memoryCount: number
    memberCount: number
    capsuleCount: number
}

export interface InviteCodeResp {
    code: string
    expireAt: string
    inviteLink: string
}

export function createFamily(data: { name: string; description?: string }): Promise<ApiResult> {
    return request.post('/families', data)
}

export function getFamily(familyId: number): Promise<ApiResult> {
    return request.get(`/families/${familyId}`)
}

export function getFamilyMembers(familyId: number): Promise<ApiResult<FamilyMemberResp[]>> {
    return request.get(`/families/${familyId}/members`)
}

export function createInviteCode(familyId: number, data?: {
    expireHours?: number;
    maxUses?: number
}): Promise<ApiResult<InviteCodeResp>> {
    return request.post(`/families/${familyId}/invite-codes`, data || {})
}

export function joinFamily(data: { inviteCode: string; nickname: string; role?: string }): Promise<ApiResult> {
    return request.post('/families/join', data)
}

export function getFamilyStatistics(familyId: number): Promise<ApiResult<FamilyStatistics>> {
    return request.get(`/families/${familyId}/statistics`)
}

// ==================== 记忆模块 ====================

export interface MemoryResp {
    id: number
    type: string
    title: string
    description: string
    date: string
    year: string
    author: string
    authorId: number
    avatar: string
    location?: string
    images: string[]
    audioUrl?: string
    audioDuration?: number
    likes: number
    tags: string[]
    isLiked: boolean
    createdAt: string
    yearsAgo?: number
    memoryTime: string
}

export interface MemoryQueryParams {
    page?: number
    size?: number
    year?: string
    type?: string
    keyword?: string
    /** 发布人ID */
    authorId?: number
    /** 地点关键字 */
    location?: string
    /** 标签 */
    tag?: string
}

export interface CreateMemoryParams {
    title: string
    description?: string
    type: string
    memoryDate: string
    location?: string
    tags?: string[]
    mediaUrls?: string[]
    audioUrl?: string
    audioDuration?: number
}

export function getMemories(familyId: number, params?: MemoryQueryParams): Promise<ApiResult<PageResult<MemoryResp>>> {
    return request.get(`/families/${familyId}/memories`, {params})
}

export function getMemoryDetail(memoryId: number): Promise<ApiResult<MemoryResp>> {
    return request.get(`/memories/${memoryId}`)
}

export function createMemory(familyId: number, data: CreateMemoryParams): Promise<ApiResult<MemoryResp>> {
    return request.post(`/families/${familyId}/memories`, data)
}

export function likeMemory(memoryId: number): Promise<ApiResult<MemoryResp>> {
    return request.post(`/memories/${memoryId}/like`)
}

export function unlikeMemory(memoryId: number): Promise<ApiResult<MemoryResp>> {
    return request.delete(`/memories/${memoryId}/like`)
}

export function getTodayInHistory(familyId: number): Promise<ApiResult<MemoryResp[]>> {
    return request.get(`/families/${familyId}/memories/today-in-history`)
}

export function getMemoryYears(familyId: number): Promise<ApiResult<string[]>> {
    return request.get(`/families/${familyId}/memories/years`)
}

export function deleteMemory(memoryId: number): Promise<ApiResult<void>> {
    return request.delete(`/memories/${memoryId}`)
}

// ==================== 时光胶囊模块 ====================

export interface CapsuleResp {
    id: number
    title: string
    content?: string
    openDate: string
    isOpened: boolean
    openedAt?: string
    creatorName: string
    creatorAvatar: string
    createdAt: string
}

export interface CapsuleListResp {
    pendingCount: number
    list: CapsuleResp[]
}

export function getCapsules(familyId: number, status?: string): Promise<ApiResult<CapsuleListResp>> {
    return request.get(`/families/${familyId}/capsules`, {params: {status}})
}

export function createCapsule(familyId: number, data: {
    title: string;
    content: string;
    openDate: string
}): Promise<ApiResult<CapsuleResp>> {
    return request.post(`/families/${familyId}/capsules`, data)
}

export function getCapsuleDetail(capsuleId: number): Promise<ApiResult<CapsuleResp>> {
    return request.get(`/capsules/${capsuleId}`)
}

// ==================== 文件上传模块 ====================

export interface FileUploadResp {
    id?: number
    /** 相对路径，用于存储到数据库 */
    url: string
    /** 完整URL，用于前端直接访问 */
    fullUrl: string
    thumbnailUrl?: string
    fileName: string
    fileSize: number
    fileType: string
}

export function uploadFile(file: File, category = 'memory'): Promise<ApiResult<FileUploadResp>> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('category', category)
    return request.post('/files/upload', formData, {
        headers: {'Content-Type': 'multipart/form-data'}
    })
}

export function uploadFiles(files: File[], category = 'memory'): Promise<ApiResult<FileUploadResp[]>> {
    const formData = new FormData()
    files.forEach(file => formData.append('files', file))
    formData.append('category', category)
    return request.post('/files/upload/batch', formData, {
        headers: {'Content-Type': 'multipart/form-data'}
    })
}
