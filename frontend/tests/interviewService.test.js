import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("../src/services/httpClient", () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn() },
}));

import httpClient from "../src/services/httpClient";
import {
  cancelInterview,
  getInterviews,
  saveScorecard,
  scheduleInterview,
  rescheduleInterview,
  unlockScorecard,
} from "../src/services/interviewService";

describe("interviewService", () => {
  beforeEach(() => vi.clearAllMocks());

  it("reschedules with CSRF and optimistic version", async () => {
    httpClient.get.mockResolvedValue({ data: { data: "token" } });
    httpClient.put.mockResolvedValue({ data: { data: { id: 7, version: 2 } } });
    const payload = {
      startsAt: "2026-12-01T02:00:00Z",
      endsAt: "2026-12-01T03:00:00Z",
      version: 1,
      reason: "改期",
    };
    await expect(rescheduleInterview(7, payload)).resolves.toEqual({
      id: 7,
      version: 2,
    });
    expect(httpClient.get).toHaveBeenCalledWith("/auth/csrf");
    expect(httpClient.put).toHaveBeenCalledWith(
      "/interviews/7/schedule",
      payload,
    );
  });

  it("loads interviews with role-aware filters", async () => {
    const page = { content: [{ id: 1 }], totalElements: 1 };
    httpClient.get.mockResolvedValue({ data: { data: page } });

    await expect(getInterviews({ status: "SCHEDULED" })).resolves.toEqual(page);
    expect(httpClient.get).toHaveBeenCalledWith("/interviews", {
      params: { status: "SCHEDULED" },
    });
  });

  it("gets CSRF before interview workflow writes", async () => {
    httpClient.get.mockResolvedValue({ data: { data: "token" } });
    httpClient.post.mockResolvedValue({ data: { data: { id: 7 } } });

    await scheduleInterview({ applicationId: 2 });
    await cancelInterview(7, "應徵者改期");
    await saveScorecard(7, { technicalScore: 4, submit: true });
    await unlockScorecard(7, 9, "更正內容");

    expect(httpClient.get).toHaveBeenCalledTimes(4);
    expect(httpClient.post).toHaveBeenCalledWith("/interviews", {
      applicationId: 2,
    });
    expect(httpClient.post).toHaveBeenCalledWith("/interviews/7/cancel", {
      reason: "應徵者改期",
    });
    expect(httpClient.post).toHaveBeenCalledWith("/interviews/7/scorecards", {
      technicalScore: 4,
      submit: true,
    });
    expect(httpClient.post).toHaveBeenCalledWith(
      "/interviews/7/scorecards/9/unlock",
      { reason: "更正內容" },
    );
  });
});
