export interface SetlistItem {
  setlistItemId: number
  orderNo: number
  songId: number
  songTitle: string
  songArtist: string | null
  songSheetId: number | null
  sheetKey: string | null
  versionName: string | null
  memo: string | null
}

export interface Setlist {
  setlistId: number
  serviceDate: string
  serviceType: string | null
  title: string | null
  memo: string | null
  items: SetlistItem[]
  createdAt: string
}

export interface SetlistCreateRequest {
  serviceDate: string
  serviceType?: string | null
  title?: string | null
  memo?: string | null
}

export interface SetlistItemCreateRequest {
  songId: number
  songSheetId?: number | null
  orderNo: number
  memo?: string | null
}
