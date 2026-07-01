import http from './http'
import type { SetlistItem, SetlistItemCreateRequest } from '../types/setlist'

export const addSetlistItem = async (setlistId: number, request: SetlistItemCreateRequest) => {
  const { data } = await http.post<SetlistItem>(`/api/setlists/${setlistId}/items`, request)
  return data
}

export interface SetlistItemUpdateRequest {
  songSheetId?: number | null
  orderNo: number
  memo?: string | null
  performanceKey?: string | null
}

export const updateSetlistItem = async (itemId: number, request: SetlistItemUpdateRequest) => {
  const { data } = await http.put<SetlistItem>(`/api/setlist-items/${itemId}`, request)
  return data
}

export const deleteSetlistItem = async (itemId: number) => {
  await http.delete(`/api/setlist-items/${itemId}`)
}
