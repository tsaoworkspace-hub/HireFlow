import httpClient from './httpClient'

export async function fetchPublicJobs() {
  const response = await httpClient.get('/public/jobs')
  return response.data.data
}
export async function fetchPublicJob(slug) {
  const response = await httpClient.get(`/public/jobs/${slug}`)
  return response.data.data
}
export async function submitApplication(slug, payload) {
  await httpClient.get('/auth/csrf')
  const response = await httpClient.post(`/public/jobs/${slug}/applications`, payload)
  return response.data.data
}
export async function fetchApplication(publicId, token) {
  const response = await httpClient.get(`/public/applications/${publicId}`, { params: { token } })
  return response.data.data
}
