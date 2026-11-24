import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../src/services/httpClient', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn() },
}))

import httpClient from '../src/services/httpClient'
import {
  acceptPublicOffer,
  createOffer,
  declinePublicOffer,
  getOffers,
  reviewOffer,
  submitOffer,
} from '../src/services/offerService'

describe('offerService', () => {
  beforeEach(() => vi.clearAllMocks())

  it('loads the offer page with query parameters', async () => {
    const page = { content: [{ id: 1, status: 'DRAFT' }], totalElements: 1 }
    httpClient.get.mockResolvedValue({ data: { data: page } })

    await expect(getOffers({ size: 100 })).resolves.toEqual(page)
    expect(httpClient.get).toHaveBeenCalledWith('/offers', { params: { size: 100 } })
  })

  it('gets CSRF before offer workflow mutations', async () => {
    httpClient.get.mockResolvedValue({ data: { data: 'token' } })
    httpClient.post.mockResolvedValue({ data: { data: { id: 9 } } })
    const payload = { applicationId: 7, annualSalary: 900000 }

    await createOffer(payload)
    await submitOffer(9)
    await reviewOffer(9, { decision: 'APPROVE', reason: null })

    expect(httpClient.get).toHaveBeenCalledTimes(3)
    expect(httpClient.post).toHaveBeenCalledWith('/offers', payload)
    expect(httpClient.post).toHaveBeenCalledWith('/offers/9/submit', undefined)
    expect(httpClient.post).toHaveBeenCalledWith(
      '/offers/9/review', { decision: 'APPROVE', reason: null },
    )
  })

  it('sends candidate offer responses through the magic-link endpoints', async () => {
    httpClient.get.mockResolvedValue({ data: { data: 'token' } })
    httpClient.post.mockResolvedValue({ data: { data: { status: 'ACCEPTED' } } })

    await acceptPublicOffer('public-id', 'a+b')
    await declinePublicOffer('public-id', 'a+b', '其他規劃')

    expect(httpClient.get).toHaveBeenCalledTimes(2)
    expect(httpClient.post).toHaveBeenCalledWith(
      '/public/applications/public-id/offer/accept?token=a%2Bb', undefined,
    )
    expect(httpClient.post).toHaveBeenCalledWith(
      '/public/applications/public-id/offer/decline?token=a%2Bb', { reason: '其他規劃' },
    )
  })
})
