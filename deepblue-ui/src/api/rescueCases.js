import { request } from './client'

export const getCasesByStatus = (status) =>
  request(`/api/rescue-cases?status=${status}`)

export const changeCaseStatus = (caseCode, status) =>
  request(`/api/rescue-cases/${caseCode}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  })