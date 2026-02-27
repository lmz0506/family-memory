/** 记忆类型 */
export type MemoryType = 'photo' | 'video' | 'note'

/** 记忆项 */
export interface Memory {
    id: number
    type: MemoryType
    date: string
    year: string
    author: string
    authorId?: number
    memoryTime?: string
    avatar: string
    title: string
    description: string
    location?: string
    images: string[]
    likes: number
    isLiked?: boolean
    tags: string[]
}

/** 家庭成员 */
export interface FamilyMember {
    name: string
    role: string
    avatar: string
    color: string
}

/** 时光胶囊 */
export interface TimeCapsule {
    id: number
    title: string
    content: string
    createdAt: string
    openDate: string
    isOpened: boolean
}

/** Tab类型 */
export type TabType = 'home' | 'timeline' | 'vault' | 'family'
