import httpClient from './httpClient'

async function ensureCsrfToken() {
  await httpClient.get('/auth/csrf')
}

async function getData(url, params) {
  const response = await httpClient.get(url, { params })
  return response.data.data
}

export const getCandidates = (params = {}) => getData('/candidates', params)
export const getCandidateResume = (id) => getData(`/candidates/${id}/resume`)
export const getApplications = (params = {}) => getData('/applications', params)
export const getApplication = (id) => getData(`/applications/${id}`)
export const getJobOpenings = (params = {}) => getData('/job-openings', params)

export async function createCandidate(payload) {
  await ensureCsrfToken()
  const response = await httpClient.post('/candidates', payload)
  return response.data.data
}

export async function updateCandidate(id, payload) {
  await ensureCsrfToken()
  const response = await httpClient.put(`/candidates/${id}`, payload)
  return response.data.data
}

export async function createApplication(payload) {
  await ensureCsrfToken()
  const response = await httpClient.post('/applications', payload)
  return response.data.data
}

export async function transitionApplication(id, payload) {
  await ensureCsrfToken()
  const response = await httpClient.post(`/applications/${id}/transition`, payload)
  return response.data.data
}

export async function closeJobOpening(id) {
  await ensureCsrfToken()
  const response = await httpClient.post(`/job-openings/${id}/close`)
  return response.data.data
}
