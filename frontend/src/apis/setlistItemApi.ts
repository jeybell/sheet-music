import http from './http'
import type { SetlistItem, SetlistItemCreateRequest } from '../types/setlist'

export const addSetlistItem = async (setlistId: number, request: SetlistItemCreateRequest) => {
  const { data } = await http.post<SetlistItem>(`/api/setlists/${setlistId}/items`, request)
  return data
}

export const deleteSetlistItem = async (itemId: number) => {
  await http.delete(`/api/setlist-items/${itemId}`)
}
