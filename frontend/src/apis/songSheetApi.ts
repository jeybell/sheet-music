import http from "./http";
import type { SongSheetCreateRequest, SongSheetSummary } from "../types/song";

export const createSongSheet = async (
  songId: number,
  request: SongSheetCreateRequest,
) => {
  const { data } = await http.post<SongSheetSummary>(
    `/api/songs/${songId}/sheets`,
    request,
  );
  return data;
};

export const updateSongSheet = async (
  songSheetId: number,
  request: SongSheetCreateRequest,
) => {
  const { data } = await http.put<SongSheetSummary>(
    `/api/song-sheets/${songSheetId}`,
    request,
  );
  return data;
};

export const deleteSongSheet = async (songSheetId: number) => {
  await http.delete(`/api/song-sheets/${songSheetId}`);
};

/** 악보 버전 순서 변경(드래그). songSheetIds 순서대로 저장된다. */
export const reorderSongSheets = async (songId: number, songSheetIds: number[]) => {
  await http.patch(`/api/songs/${songId}/sheets/reorder`, { songSheetIds });
};
