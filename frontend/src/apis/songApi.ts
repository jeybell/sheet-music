import http from "./http";
import type { Song, SongCreateRequest, SongUpdateRequest, SongLink } from "../types/song";

export interface SongSearchParams {
  query?: string | null;
  songKey?: string | null;
  tag?: string | null;
}

export const getSongs = async (params?: SongSearchParams) => {
  const { data } = await http.get<Song[]>("/api/songs", {
    params: {
      query: params?.query?.trim() || undefined,
      songKey: params?.songKey?.trim() || undefined,
      tag: params?.tag?.trim() || undefined,
    },
  });
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

export const updateLyrics = async (songId: number, lyrics: string | null) => {
  const { data } = await http.patch<Song>(`/api/songs/${songId}/lyrics`, { lyrics });
  return data;
};

export const deleteSong = async (songId: number) => {
  await http.delete(`/api/songs/${songId}`);
};

export const getAllTags = async (): Promise<string[]> => {
  const { data } = await http.get<string[]>("/api/songs/tags");
  return data;
};

export const addSongLink = async (songId: number, payload: { title: string; url: string }): Promise<SongLink> => {
  const { data } = await http.post<SongLink>(`/api/songs/${songId}/links`, payload);
  return data;
};

export const updateSongLink = async (linkId: number, payload: { title: string; url: string }): Promise<SongLink> => {
  const { data } = await http.put<SongLink>(`/api/song-links/${linkId}`, payload);
  return data;
};

export const deleteSongLink = async (linkId: number): Promise<void> => {
  await http.delete(`/api/song-links/${linkId}`);
};
