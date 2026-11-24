import httpClient from './httpClient'

async function ensureCsrfToken() {
  await httpClient.get('/auth/csrf')
}

async function getData(url, config = {}) {
  const response = await httpClient.get(url, config)
  return response.data.data
}

async function mutate(method, url, payload) {
  await ensureCsrfToken()
  const response = await httpClient[method](url, payload)
  return response.data.data
}

export const getOffers = (params = {}) => getData('/offers', { params })
export const getOffer = (id) => getData(`/offers/${id}`)
export const getOfferOptions = () => getData('/offers/options')
export const createOffer = (payload) => mutate('post', '/offers', payload)
export const updateOffer = (id, payload) => mutate('put', `/offers/${id}`, payload)
export const submitOffer = (id) => mutate('post', `/offers/${id}/submit`)
export const reviewOffer = (id, payload) => mutate('post', `/offers/${id}/review`, payload)
export const sendOffer = (id) => mutate('post', `/offers/${id}/send`)
export const cancelOffer = (id, reason) => mutate('post', `/offers/${id}/cancel`, { reason })
export const getAttachments = (applicationId) => getData(`/applications/${applicationId}/attachments`)

export async function downloadAttachment(attachment) {
  const response = await httpClient.get(`/attachments/${attachment.id}/download`, { responseType: 'blob' })
  const url = URL.createObjectURL(response.data)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = attachment.originalFilename
  anchor.click()
  URL.revokeObjectURL(url)
}

export const getPublicOffer = (publicId, token) =>
  getData(`/public/applications/${publicId}/offer`, { params: { token } })

export async function acceptPublicOffer(publicId, token) {
  return mutate('post', `/public/applications/${publicId}/offer/accept?token=${encodeURIComponent(token)}`)
}

export async function declinePublicOffer(publicId, token, reason) {
  return mutate('post', `/public/applications/${publicId}/offer/decline?token=${encodeURIComponent(token)}`, { reason })
}

export const getPublicResume = (publicId, token) =>
  getData(`/public/applications/${publicId}/resume`, { params: { token } })

export async function uploadPublicResume(publicId, token, file) {
  await ensureCsrfToken()
  const form = new FormData()
  form.append('file', file)
  const response = await httpClient.post(
    `/public/applications/${publicId}/resume?token=${encodeURIComponent(token)}`,
    form,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
  return response.data.data
}
