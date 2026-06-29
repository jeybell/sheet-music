import http from './http'
import type { FeatureRequest, FeatureRequestCreateRequest, FeatureRequestStatus } from '../types/featureRequest'

export const getFeatureRequests = async () => {
  const { data } = await http.get<FeatureRequest[]>('/api/feature-requests')
  return data
}

export const createFeatureRequest = async (request: FeatureRequestCreateRequest) => {
  const { data } = await http.post<FeatureRequest>('/api/feature-requests', request)
  return data
}

export const updateFeatureRequestStatus = async (id: number, status: FeatureRequestStatus) => {
  const { data } = await http.patch<FeatureRequest>(`/api/feature-requests/${id}/status`, { status })
  return data
}

export const deleteFeatureRequest = async (id: number) => {
  await http.delete(`/api/feature-requests/${id}`)
}
