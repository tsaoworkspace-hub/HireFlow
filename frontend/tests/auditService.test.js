import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../src/services/httpClient', () => ({ default: { get: vi.fn() } }))

import httpClient from '../src/services/httpClient'
import { getAuditLogs } from '../src/services/auditService'

describe('auditService', () => {
  beforeEach(() => vi.clearAllMocks())

  it('loads paged audit logs with filters', async () => {
    const page = { content: [], totalElements: 0 }
    httpClient.get.mockResolvedValue({ data: { data: page } })
    const params = { action: 'HIRING_DECISION_CREATED', page: 0 }
    await expect(getAuditLogs(params)).resolves.toEqual(page)
    expect(httpClient.get).toHaveBeenCalledWith('/admin/audit-logs', { params })
  })
})
