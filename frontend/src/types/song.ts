export interface SongSheetSummary {
  songSheetId: number
  sheetKey: string
  versionName: string
  memo: string | null
}

export interface Song {
  id: number
  title: string
  artist: string | null
  memo?: string | null
  sheets?: SongSheetSummary[]
  songSheets?: SongSheetSummary[]
}
