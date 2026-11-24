import httpClient from './httpClient'

async function ensureCsrfToken() {
  await httpClient.get('/auth/csrf')
}

export async function login(credentials) {
  await ensureCsrfToken()
  const response = await httpClient.post('/auth/login', credentials)
  return response.data.data
}

export async function logout() {
  await ensureCsrfToken()
  await httpClient.post('/auth/logout')
}

export async function getCurrentUser() {
  const response = await httpClient.get('/auth/me')
  return response.data.data
}
