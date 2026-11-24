import httpClient from './httpClient'

async function ensureCsrfToken() {
  await httpClient.get('/auth/csrf')
}

export async function getDecisionView(applicationId) {
  const response = await httpClient.get(`/applications/${applicationId}/decision-view`)
  return response.data.data
}

export async function createDecision(applicationId, payload) {
  await ensureCsrfToken()
  const response = await httpClient.post(`/applications/${applicationId}/decision`, payload)
  return response.data.data
}
