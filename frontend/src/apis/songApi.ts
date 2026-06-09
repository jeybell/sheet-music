import http from "./http";
import type { Song, SongCreateRequest, SongUpdateRequest } from "../types/song";

export const getSongs = async () => {
  const { data } = await http.get<Song[]>("/api/songs");
  return data;
};

export const getSong = async (songId: number) => {
  const { data } = await http.get<Song>(`/api/songs/${songId}`);
  return data;
};

export const createSong = async (request: SongCreateRequest) => {
  const { data } = await http.post<Song>("/api/songs", request);
  return data;
};

export const updateSong = async (songId: number, request: SongUpdateRequest) => {
  const { data } = await http.put<Song>(`/api/songs/${songId}`, request);
  return data;
};

export const deleteSong = async (songId: number) => {
  await http.delete(`/api/songs/${songId}`);
};
