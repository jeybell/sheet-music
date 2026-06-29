export interface OcrResult {
  title: string | null;
  key: string | null;
  chords: string[];
  artist: string | null;
  lyrics: string | null;
  rawText: string | null;
}

export interface SongFile {
  songFileId: number;
  originalFileName?: string | null;
  storedFileName?: string | null;
  filePath?: string | null;
  contentType?: string | null;
  fileSize?: number | null;
  ocrDone?: boolean;
  ocrResult?: OcrResult | null;
}

export interface SongSheetSummary {
  songSheetId: number;
  sheetKey: string | null;
  versionName: string | null;
  memo: string | null;
  files?: SongFile[];
}

export interface SongLink {
  linkId: number;
  title: string;
  url: string;
}

export interface Song {
  songId: number;
  title: string;
  artist: string | null;
  memo?: string | null;
  lyrics?: string | null;
  youtubeUrl?: string | null;
  tags?: string[];
  links?: SongLink[];
  sheets?: SongSheetSummary[];
  songSheets?: SongSheetSummary[];
  files?: SongFile[];
}

export interface SongCreateRequest {
  title: string;
  artist?: string | null;
  memo?: string | null;
}

export interface SongUpdateRequest {
  title: string;
  artist?: string | null;
  memo?: string | null;
  youtubeUrl?: string | null;
  tags?: string[];
}

export interface SongSheetCreateRequest {
  sheetKey?: string | null;
  versionName?: string | null;
  memo?: string | null;
}
