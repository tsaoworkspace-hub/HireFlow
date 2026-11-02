import { describe, expect, it, vi } from 'vitest'

vi.mock('../src/services/httpClient', () => ({
  default: {
    get: vi.fn().mockResolvedValue({
      data: { success: true, data: { status: 'UP' } },
    }),
  },
}))

import { getHealth } from '../src/services/healthService'

describe('healthService', () => {
  it('returns the API response body', async () => {
    await expect(getHealth()).resolves.toEqual({
      success: true,
      data: { status: 'UP' },
    })
  })
})

