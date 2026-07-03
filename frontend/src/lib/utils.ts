import { type ClassValue, clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/**
 * 사용자가 입력한 URL 이 안전한 http/https 인지 검사한다.
 * javascript:, data:, vbscript: 같은 스킴을 걸러 저장형 XSS 를 막는다.
 * 백엔드에서도 검증하지만, 이미 저장된 악성 데이터를 렌더 시점에 무력화하기 위한 방어선.
 */
export function isSafeHttpUrl(url: string | null | undefined): boolean {
  if (!url) return false
  try {
    const parsed = new URL(url, window.location.origin)
    return parsed.protocol === 'http:' || parsed.protocol === 'https:'
  } catch {
    return false
  }
}

/** 안전한 URL 이면 그대로, 아니면 링크로 쓸 수 없도록 undefined 를 반환한다(href 바인딩용). */
export function safeHref(url: string | null | undefined): string | undefined {
  return isSafeHttpUrl(url) ? (url as string) : undefined
}
