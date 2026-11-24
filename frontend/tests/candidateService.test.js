import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../src/services/httpClient', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn() },
}))

import httpClient from '../src/services/httpClient'
import {
  closeJobOpening,
  createApplication,
  getApplications,
  transitionApplication,
} from '../src/services/candidateService'

describe('candidateService', () => {
  beforeEach(() => vi.clearAllMocks())

  it('loads the application board with filters', async () => {
    const page = { content: [{ id: 1, stage: 'NEW' }], totalElements: 1 }
    httpClient.get.mockResolvedValue({ data: { data: page } })

    await expect(getApplications({ stage: 'NEW' })).resolves.toEqual(page)
    expect(httpClient.get).toHaveBeenCalledWith('/applications', { params: { stage: 'NEW' } })
  })

  it('gets CSRF before creating, transitioning, and closing', async () => {
    httpClient.get.mockResolvedValue({ data: { data: 'token' } })
    httpClient.post.mockResolvedValue({ data: { data: { id: 8 } } })

    await createApplication({ candidateId: 2, jobOpeningId: 3 })
    await transitionApplication(8, { nextStage: 'SCREENING' })
    await closeJobOpening(3)

    expect(httpClient.get).toHaveBeenCalledTimes(3)
    expect(httpClient.post).toHaveBeenCalledWith('/applications', { candidateId: 2, jobOpeningId: 3 })
    expect(httpClient.post).toHaveBeenCalledWith('/applications/8/transition', { nextStage: 'SCREENING' })
    expect(httpClient.post).toHaveBeenCalledWith('/job-openings/3/close')
  })
})
