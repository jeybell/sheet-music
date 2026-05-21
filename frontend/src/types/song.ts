export interface SongFile {
  id: number
  originalFileName?: string | null
  storedFileName?: string | null
  filePath?: string | null
  contentType?: string | null
  fileSize?: number | null
}

export interface SongSheetSummary {
  id?: number
  songSheetId?: number
  sheetKey: string | null
  versionName: string | null
  memo: string | null
  files?: SongFile[]
}

export interface Song {
  id: number
  title: string
  artist: string | null
  composer?: string | null
  memo?: string | null
  sheets?: SongSheetSummary[]
  songSheets?: SongSheetSummary[]
  files?: SongFile[]
}

export interface SongCreateRequest {
  title: string
  artist?: string | null
  composer?: string | null
  memo?: string | null
}
