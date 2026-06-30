import http from "./http";
import type { OcrResult } from "../types/song";

export const previewOcr = async (file: File): Promise<OcrResult> => {
  const formData = new FormData();
  formData.append("file", file);
  const { data } = await http.post<OcrResult>("/api/ocr/preview", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return data;
};

export const runOcrOnFile = async (songFileId: number): Promise<OcrResult | null> => {
  const { data, status } = await http.post<OcrResult>(`/api/song-files/${songFileId}/ocr`);
  if (status === 204) return null;
  return data;
};
