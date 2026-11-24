import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../src/services/httpClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

import httpClient from '../src/services/httpClient'
import { getCurrentUser, login, logout } from '../src/services/authService'

describe('authService', () => {
  beforeEach(() => vi.clearAllMocks())

  it('returns the logged-in user from the response envelope', async () => {
    const user = { email: 'hr@hireflow.local', roles: ['HR'] }
    httpClient.get.mockResolvedValue({ data: { data: 'csrf-token' } })
    httpClient.post.mockResolvedValue({ data: { data: user } })

    await expect(login({ email: user.email, password: 'secret' })).resolves.toEqual(user)
  })

  it('restores the current user', async () => {
    const user = { email: 'manager@hireflow.local', roles: ['HIRING_MANAGER'] }
    httpClient.get.mockResolvedValue({ data: { data: user } })

    await expect(getCurrentUser()).resolves.toEqual(user)
  })

  it('calls the logout endpoint', async () => {
    httpClient.post.mockResolvedValue({})
    httpClient.get.mockResolvedValue({ data: { data: 'csrf-token' } })

    await logout()

    expect(httpClient.post).toHaveBeenCalledWith('/auth/logout')
  })
})
