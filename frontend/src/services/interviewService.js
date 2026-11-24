import httpClient from './httpClient'

async function ensureCsrfToken() {
  await httpClient.get('/auth/csrf')
}

async function getData(url, params) {
  const response = await httpClient.get(url, { params })
  return response.data.data
}

export const getInterviews = (params = {}) => getData('/interviews', params)
export const getInterview = (id) => getData(`/interviews/${id}`)
export const getInterviewOptions = () => getData('/interviews/options')

export async function scheduleInterview(payload) {
  await ensureCsrfToken()
  const response = await httpClient.post('/interviews', payload)
  return response.data.data
}

export async function cancelInterview(id, reason) {
  await ensureCsrfToken()
  const response = await httpClient.post(`/interviews/${id}/cancel`, { reason })
  return response.data.data
}

export async function completeInterview(id) {
  await ensureCsrfToken()
  const response = await httpClient.post(`/interviews/${id}/complete`)
  return response.data.data
}

export async function saveScorecard(id, payload) {
  await ensureCsrfToken()
  const response = await httpClient.post(`/interviews/${id}/scorecards`, payload)
  return response.data.data
}

export async function unlockScorecard(interviewId, scorecardId, reason) {
  await ensureCsrfToken()
  const response = await httpClient.post(`/interviews/${interviewId}/scorecards/${scorecardId}/unlock`, { reason })
  return response.data.data
}
