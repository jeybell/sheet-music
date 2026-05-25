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
