import http from './http'
import type { Setlist, SetlistCreateRequest } from '../types/setlist'

export const getSetlists = async () => {
  const { data } = await http.get<Setlist[]>('/api/setlists')
  return data
}

export const getSetlist = async (setlistId: number) => {
  const { data } = await http.get<Setlist>(`/api/setlists/${setlistId}`)
  return data
}

export const createSetlist = async (request: SetlistCreateRequest) => {
  const { data } = await http.post<Setlist>('/api/setlists', request)
  return data
}

export const updateSetlist = async (setlistId: number, request: SetlistCreateRequest) => {
  const { data } = await http.put<Setlist>(`/api/setlists/${setlistId}`, request)
  return data
}

export const deleteSetlist = async (setlistId: number) => {
  await http.delete(`/api/setlists/${setlistId}`)
}
