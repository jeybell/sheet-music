import http from './http'
import type { AdminUser, DeletedSetlist, DeletedSong, UserRole } from '../types/admin'

// 사용자 관리
export const getAdminUsers = async () => {
  const { data } = await http.get<AdminUser[]>('/api/admin/users')
  return data
}

export const changeUserRole = async (userId: number, role: UserRole) => {
  const { data } = await http.patch<AdminUser>(`/api/admin/users/${userId}/role`, { role })
  return data
}

export const deleteUser = async (userId: number) => {
  await http.delete(`/api/admin/users/${userId}`)
}

// 콘텐츠(휴지통) 관리
export const getDeletedSongs = async () => {
  const { data } = await http.get<DeletedSong[]>('/api/admin/songs/deleted')
  return data
}

export const restoreSong = async (songId: number) => {
  await http.post(`/api/admin/songs/${songId}/restore`)
}

export const getDeletedSetlists = async () => {
  const { data } = await http.get<DeletedSetlist[]>('/api/admin/setlists/deleted')
  return data
}

export const restoreSetlist = async (setlistId: number) => {
  await http.post(`/api/admin/setlists/${setlistId}/restore`)
}
