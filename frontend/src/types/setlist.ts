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
  performanceKey: string | null
  youtubeUrl: string | null
}

export interface Setlist {
  setlistId: number
  serviceDate: string
  title: string | null
  memo: string | null
  youtubeUrl: string | null
  shareToken: string | null
  items: SetlistItem[]
  createdAt: string
}

export interface SharedFile {
  songFileId: number
  contentType: string | null
  originalFileName: string | null
}

export interface SharedSetlistItem {
  setlistItemId: number
  orderNo: number
  songId: number
  songTitle: string
  songArtist: string | null
  songSheetId: number | null
  sheetKey: string | null
  versionName: string | null
  memo: string | null
  files: SharedFile[]
}

export interface SharedSetlist {
  setlistId: number
  serviceDate: string
  title: string | null
  memo: string | null
  items: SharedSetlistItem[]
}

export interface SetlistCreateRequest {
  serviceDate: string
  title?: string | null
  memo?: string | null
  youtubeUrl?: string | null
}

export interface SetlistItemCreateRequest {
  songId: number
  songSheetId?: number | null
  orderNo: number
  memo?: string | null
  performanceKey?: string | null
  youtubeUrl?: string | null
}
