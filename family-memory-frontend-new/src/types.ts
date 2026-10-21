export interface User {
    id: number;
    username: string;
    nickname: string;
    avatar?: string;
    families?: Family[];
    currentFamily?: Family | null
}

export interface Family {
    id: number;
    name: string;
    description?: string;
    coverUrl?: string;
    role?: string;
    admin?: boolean;
    memberCount?: number;
    memoryCount?: number;
    photoCount?: number;
    capsuleCount?: number;
    recordDays?: number
}

export interface FamilyMember {
    id: number;
    userId: number;
    displayName: string;
    role: string;
    color: string;
    admin: boolean;
    avatar?: string;
    memoryCount: number
}

export interface Memory {
    id: number;
    familyId: number;
    type: string;
    title: string;
    content?: string;
    memoryDate: string;
    memoryYear: number;
    location?: string;
    audioUrl?: string;
    audioDuration?: number;
    mediaUrls: string[];
    coverUrl?: string;
    tags: string[];
    likesCount: number;
    commentsCount: number;
    liked: boolean;
    authorId: number;
    authorName: string;
    authorAvatar?: string;
    createdAt: string;
    yearsAgo?: number;
    comments?: Comment[]
}

export interface TimelineMonthSummary {
    month: number;
    count: number
}

export interface TimelineYearSummary {
    year: number;
    count: number;
    months: TimelineMonthSummary[]
}

export interface TimelineMemoryStar {
    id: number;
    year: number;
    month: number;
    day: number;
    weight: number
}

export interface Comment {
    id: number;
    content: string;
    userId: number;
    userName: string;
    avatar?: string;
    createdAt: string
}

export interface Capsule {
    id: number;
    title: string;
    recipient?: string;
    coverUrl?: string;
    openDate: string;
    opened: boolean;
    daysRemaining: number;
    creatorName: string;
    createdAt: string;
    content?: string
}

export type MilestoneVisibility = 'PRIVATE' | 'FAMILY' | 'SPECIFIC'

export interface FamilyMilestone {
    id: number;
    familyId: number;
    title: string;
    description?: string;
    eventDate: string;
    nextEventDate: string;
    recurringYearly: boolean;
    visibilityScope: MilestoneVisibility;
    visibleTo: Array<{userId: number; displayName: string}>;
    daysRemaining: number;
    expired: boolean;
    creatorId: number;
    creatorName: string;
    canManage: boolean;
    createdAt: string
}

export interface FamilyMilestonePayload {
    title: string;
    description?: string;
    eventDate: string;
    recurringYearly: boolean;
    visibilityScope: MilestoneVisibility;
    viewerUserIds: number[]
}

export interface PageData<T> {
    total: number;
    page: number;
    size: number;
    list: T[]
}

export interface ApiResult<T> {
    code: number;
    message: string;
    data: T
}


/** 星海索引来源；不对应媒体类型。 */
export type UniverseSourceType = 'MEMORY' | 'CAPSULE'
export interface UniverseDaySummary { date:string; day:number; memoryCount:number; capsuleCount:number }
export interface UniverseMonthSummary { month:number; memoryCount:number; capsuleCount:number; days:UniverseDaySummary[] }
export interface UniverseYearSummary { year:number; memoryCount:number; capsuleCount:number; months:UniverseMonthSummary[] }
export interface UniverseOverview { memoryCount:number; capsuleCount:number; years:UniverseYearSummary[] }
/** 点击日期或搜索后才加载；不含正文、图片或珍藏开启状态。 */
export interface UniverseItem { indexId:number; sourceType:UniverseSourceType; sourceId:number; nodeDate:string; title:string }
export interface UniversePage { total:number; page:number; size:number; hasMore:boolean; items:UniverseItem[] }
export interface UniverseBatch { year:number; items:UniverseItem[]; nextAfterId:number; hasMore:boolean }
