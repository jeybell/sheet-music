import http from "./http";
import type { SongFile } from "../types/song";

export const uploadSongSheetFile = async (songSheetId: number, file: File) => {
  const formData = new FormData();
  formData.append("file", file);

  const { data } = await http.post<SongFile>(
    `/api/song-sheets/${songSheetId}/files`,
    formData,
    {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    },
  );
  return data;
};
