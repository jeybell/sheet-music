export type FeatureRequestStatus = 'PENDING' | 'REVIEWING' | 'DONE' | 'HOLD'

export interface FeatureRequest {
  featureRequestId: number
  title: string
  content: string
  author: string | null
  status: FeatureRequestStatus
  createdAt: string
}

export interface FeatureRequestCreateRequest {
  title: string
  content: string
  author: string | null
}

export const STATUS_LABEL: Record<FeatureRequestStatus, string> = {
  PENDING: '요청중',
  REVIEWING: '검토중',
  DONE: '완료',
  HOLD: '보류',
}

export const STATUS_VARIANT: Record<FeatureRequestStatus, 'default' | 'secondary' | 'violet' | 'blue' | 'destructive'> = {
  PENDING: 'default',
  REVIEWING: 'blue',
  DONE: 'violet',
  HOLD: 'secondary',
}
