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

export const deleteSongFile = async (songFileId: number) => {
  await http.delete(`/api/song-files/${songFileId}`);
};

export interface AnnotationStrokePoint {
  x: number;
  y: number;
  pressure: number;
}

export interface AnnotationStroke {
  points: AnnotationStrokePoint[];
  color: string;
  width: number;
  eraser: boolean;
}

export interface SongFileAnnotation {
  songFileId: number;
  strokes: AnnotationStroke[];
  updatedAt: string | null;
}

export const getSongFileAnnotation = async (songFileId: number) => {
  const { data } = await http.get<SongFileAnnotation>(`/api/song-files/${songFileId}/annotation`);
  return data;
};

export const saveSongFileAnnotation = async (songFileId: number, strokes: AnnotationStroke[]) => {
  const { data } = await http.put<SongFileAnnotation>(`/api/song-files/${songFileId}/annotation`, { strokes });
  return data;
};
