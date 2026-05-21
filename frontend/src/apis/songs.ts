import http from './http'
import type { Song } from '../types/song'

export const getSongs = async () => {
  const { data } = await http.get<Song[]>('/api/songs')
  return data
}

export const getSong = async (id: number) => {
  const { data } = await http.get<Song>(`/api/songs/${id}`)
  return data
}
