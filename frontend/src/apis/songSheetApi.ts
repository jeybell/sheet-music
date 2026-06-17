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
