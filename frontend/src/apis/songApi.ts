import http from './http'
import type { Song, SongCreateRequest } from '../types/song'

export const getSongs = async () => {
  const { data } = await http.get<Song[]>('/api/songs')
  return data
}

export const getSong = async (songId: number) => {
  const { data } = await http.get<Song>(`/api/songs/${songId}`)
  return data
}

export const createSong = async (request: SongCreateRequest) => {
  const { data } = await http.post<Song>('/api/songs', request)
  return data
}
