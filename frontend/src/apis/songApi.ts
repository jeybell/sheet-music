import http from "./http";
import type { Song, SongCreateRequest, SongUpdateRequest, SongLink } from "../types/song";
import type { PageResponse } from "../types/page";

export interface SongSearchParams {
  query?: string | null;
  songKey?: string | null;
  tags?: string[] | null;
}

// 곡 목록 화면 무한 스크롤용 페이지 단위 조회
export const getSongsPage = async (
  params: SongSearchParams | undefined,
  page: number,
  size: number,
) => {
  const { data } = await http.get<PageResponse<Song>>("/api/songs", {
    params: {
      query: params?.query?.trim() || undefined,
      songKey: params?.songKey?.trim() || undefined,
      // Spring 이 콤마 구분 문자열을 List<String> 으로 바인딩하므로 join 하여 전달
      tags: params?.tags?.length ? params.tags.join(',') : undefined,
      page,
      size,
    },
  });
  return data;
};

// 곡 전체 조회 (콘티의 곡 선택 모달 등에서 사용). 큰 size 로 한 번에 받아온다.
export const getSongs = async (params?: SongSearchParams) => {
  const data = await getSongsPage(params, 0, 2000);
  return data.content;
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
