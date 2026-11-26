import httpClient from "./httpClient";
async function csrf() {
  await httpClient.get("/auth/csrf");
}
export async function getGoogleStatus() {
  const r = await httpClient.get("/integrations/google-calendar/status");
  return r.data.data;
}
export async function getGoogleAuthorization() {
  const r = await httpClient.get("/integrations/google-calendar/authorize");
  return r.data.data;
}
export async function disconnectGoogle() {
  await csrf();
  await httpClient.delete("/integrations/google-calendar");
}
export async function getEmailDeliveries() {
  const r = await httpClient.get("/integrations/email-deliveries", {
    params: { size: 100 },
  });
  return r.data.data;
}
export async function retryEmail(id) {
  await csrf();
  const r = await httpClient.post(`/integrations/email-deliveries/${id}/retry`);
  return r.data.data;
}
export async function getCalendarSyncJobs() {
  const r = await httpClient.get("/integrations/google-calendar/sync-jobs", {
    params: { size: 100 },
  });
  return r.data.data;
}
export async function retryCalendarSync(id) {
  await csrf();
  const r = await httpClient.post(
    `/integrations/google-calendar/sync-jobs/${id}/retry`,
  );
  return r.data.data;
}
