import httpClient from './httpClient'
async function csrf(){ await httpClient.get('/auth/csrf') }
export async function getCalendarEvents(from,to){ const r=await httpClient.get('/calendar',{params:{from,to}});return r.data.data }
export async function getInterviewSlots(applicationId){const r=await httpClient.get('/interview-slots',{params:{applicationId}});return r.data.data}
export async function createInterviewSlot(payload){await csrf();const r=await httpClient.post('/interview-slots',payload);return r.data.data}
export async function withdrawInterviewSlot(id){await csrf();await httpClient.delete(`/interview-slots/${id}`)}
export async function getPublicInterviewSlots(publicId,token){const r=await httpClient.get(`/public/applications/${publicId}/interview-slots`,{params:{token}});return r.data.data}
export async function selectPublicInterviewSlot(publicId,slotId,token){await csrf();const r=await httpClient.post(`/public/applications/${publicId}/interview-slots/${slotId}/select`,null,{params:{token}});return r.data.data}
