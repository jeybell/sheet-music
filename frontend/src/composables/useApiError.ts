import { isAxiosError } from 'axios'

interface ApiErrorResponse {
  message?: string
  validationErrors?: Record<string, string>
}

export function extractApiError(error: unknown, fallback: string): string {
  if (!isAxiosError<ApiErrorResponse>(error)) return fallback

  const status = error.response?.status
  const data = error.response?.data

  if (status === 409) {
    const msg = data?.message ?? fallback
    return `${msg}\n동명이곡은 제목에 '(구분어)'를 붙여 구분하세요. 예) 제목 (통기타)`
  }

  if (status === 400 && data?.validationErrors) {
    return Object.values(data.validationErrors).join('\n')
  }

  return data?.message ?? fallback
}
