export type UserRole = 'USER' | 'ADMIN'

export interface AdminUser {
  userId: number
  username: string
  role: UserRole
  createdAt: string
}

export interface DeletedSong {
  songId: number
  title: string
  artist: string | null
  deletedAt: string
}

export interface DeletedSetlist {
  setlistId: number
  title: string | null
  serviceDate: string
  deletedAt: string
}

export const ROLE_LABEL: Record<UserRole, string> = {
  USER: '일반',
  ADMIN: '관리자',
}
