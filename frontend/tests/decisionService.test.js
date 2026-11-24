import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../src/services/httpClient', () => ({
  default: { get: vi.fn(), post: vi.fn() },
}))

import httpClient from '../src/services/httpClient'
import { createDecision, getDecisionView } from '../src/services/decisionService'

describe('decisionService', () => {
  beforeEach(() => vi.clearAllMocks())

  it('loads a decision view', async () => {
    httpClient.get.mockResolvedValue({ data: { data: { applicationId: 7 } } })
    await expect(getDecisionView(7)).resolves.toEqual({ applicationId: 7 })
    expect(httpClient.get).toHaveBeenCalledWith('/applications/7/decision-view')
  })

  it('gets CSRF before creating a final decision', async () => {
    httpClient.get.mockResolvedValue({ data: { data: 'token' } })
    httpClient.post.mockResolvedValue({ data: { data: { stage: 'HIRED' } } })
    const payload = { result: 'HIRED', reason: '通過', applicationVersion: 2 }
    await expect(createDecision(7, payload)).resolves.toEqual({ stage: 'HIRED' })
    expect(httpClient.get).toHaveBeenCalledWith('/auth/csrf')
    expect(httpClient.post).toHaveBeenCalledWith('/applications/7/decision', payload)
  })
})
