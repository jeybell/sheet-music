const PRIMARY_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
const FALLBACK_BASE_URL = import.meta.env.VITE_API_FALLBACK_BASE_URL || ''

let activeBaseUrl = PRIMARY_BASE_URL
let usingFallback = false

export const getActiveBaseUrl = () => activeBaseUrl

export const isPrimaryBaseUrl = (url: string) => url === PRIMARY_BASE_URL

export const getFallbackBaseUrl = () => FALLBACK_BASE_URL

/** 1차 백엔드가 응답 없을 때 폴백으로 전환. 폴백 URL이 없거나 이미 전환된 상태면 아무 것도 하지 않는다. */
export const switchToFallbackBaseUrl = () => {
  if (usingFallback || !FALLBACK_BASE_URL) return false
  activeBaseUrl = FALLBACK_BASE_URL
  usingFallback = true
  return true
}
