import httpClient from './httpClient'

export async function getAuditLogs(params = {}) {
  const response = await httpClient.get('/admin/audit-logs', { params })
  return response.data.data
}
