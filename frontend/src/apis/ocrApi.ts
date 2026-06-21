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
