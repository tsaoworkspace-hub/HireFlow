import httpClient from './httpClient'

async function ensureCsrfToken() {
  await httpClient.get('/auth/csrf')
}

export async function getRequisitions(params = {}) {
  const response = await httpClient.get('/requisitions', { params })
  return response.data.data
}

export async function getRequisitionOptions() {
  const response = await httpClient.get('/requisitions/options')
  return response.data.data
}

export async function getRequisition(id) {
  const response = await httpClient.get(`/requisitions/${id}`)
  return response.data.data
}

export async function createRequisition(payload) {
  await ensureCsrfToken()
  const response = await httpClient.post('/requisitions', payload)
  return response.data.data
}

export async function updateRequisition(id, payload) {
  await ensureCsrfToken()
  const response = await httpClient.put(`/requisitions/${id}`, payload)
  return response.data.data
}

export async function submitRequisition(id) {
  await ensureCsrfToken()
  const response = await httpClient.post(`/requisitions/${id}/submit`)
  return response.data.data
}

export async function reviewRequisition(id, payload) {
  await ensureCsrfToken()
  const response = await httpClient.post(`/requisitions/${id}/review`, payload)
  return response.data.data
}
