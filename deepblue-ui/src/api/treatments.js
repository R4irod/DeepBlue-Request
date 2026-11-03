import { request } from './client'

export const registerTreatment = (payload) =>
  request('/api/treatments', {
    method: 'POST',
    body: JSON.stringify(payload),
  })