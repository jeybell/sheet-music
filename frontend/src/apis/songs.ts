import http from './http'

export interface Song {
  id: number
  title: string
  artist: string | null
  memo?: string | null
}

export const getSongs = async () => {
  const { data } = await http.get<Song[]>('/api/songs')
  return data
}

export const getSong = async (id: number) => {
  const { data } = await http.get<Song>(`/api/songs/${id}`)
  return data
}
