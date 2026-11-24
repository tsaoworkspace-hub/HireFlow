import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../src/services/httpClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}))

import httpClient from '../src/services/httpClient'
import {
  createRequisition,
  getRequisition,
  getRequisitions,
  reviewRequisition,
  submitRequisition,
  updateRequisition,
} from '../src/services/requisitionService'

describe('requisitionService', () => {
  beforeEach(() => vi.clearAllMocks())

  it('unwraps the paged requisition response', async () => {
    const page = { content: [{ id: 1 }], totalElements: 1 }
    httpClient.get.mockResolvedValue({ data: { data: page } })

    await expect(getRequisitions({ status: 'DRAFT' })).resolves.toEqual(page)
    expect(httpClient.get).toHaveBeenCalledWith('/requisitions', { params: { status: 'DRAFT' } })
  })

  it('loads a requisition detail', async () => {
    const requisition = { id: 9, status: 'SUBMITTED' }
    httpClient.get.mockResolvedValue({ data: { data: requisition } })

    await expect(getRequisition(9)).resolves.toEqual(requisition)
    expect(httpClient.get).toHaveBeenCalledWith('/requisitions/9')
  })

  it('gets a CSRF token before creating and updating', async () => {
    httpClient.get.mockResolvedValue({ data: { data: 'token' } })
    httpClient.post.mockResolvedValue({ data: { data: { id: 3 } } })
    httpClient.put.mockResolvedValue({ data: { data: { id: 3, version: 1 } } })

    await createRequisition({ departmentId: 2 })
    await updateRequisition(3, { departmentId: 2, version: 0 })

    expect(httpClient.get).toHaveBeenCalledTimes(2)
    expect(httpClient.post).toHaveBeenCalledWith('/requisitions', { departmentId: 2 })
    expect(httpClient.put).toHaveBeenCalledWith('/requisitions/3', { departmentId: 2, version: 0 })
  })

  it('calls submit and review workflow endpoints', async () => {
    httpClient.get.mockResolvedValue({ data: { data: 'token' } })
    httpClient.post
      .mockResolvedValueOnce({ data: { data: { status: 'SUBMITTED' } } })
      .mockResolvedValueOnce({ data: { data: { status: 'APPROVED' } } })

    await expect(submitRequisition(7)).resolves.toEqual({ status: 'SUBMITTED' })
    await expect(reviewRequisition(7, { decision: 'APPROVE' })).resolves.toEqual({ status: 'APPROVED' })

    expect(httpClient.post).toHaveBeenCalledWith('/requisitions/7/submit')
    expect(httpClient.post).toHaveBeenCalledWith('/requisitions/7/review', { decision: 'APPROVE' })
  })
})
