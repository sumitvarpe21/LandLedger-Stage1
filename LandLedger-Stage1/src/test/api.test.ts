import { afterEach, describe, expect, it, vi } from "vitest";
import * as api from "@/services/api";
import { setAccessToken } from "@/services/backend";

describe("database API client", () => {
  afterEach(() => {
    setAccessToken(null);
    vi.unstubAllGlobals();
  });

  it("uses backend authentication and sends the session token with registry requests", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            token: "signed-session-token",
            user: {
              id: "citizen-id",
              name: "Registered Citizen",
              email: "citizen@example.test",
              role: "CITIZEN",
              phone: "",
              active: true,
            },
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        ),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify([]), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      );
    vi.stubGlobal("fetch", fetchMock);

    const user = await api.login("citizen@example.test", "correct-password");
    await api.listApplications();

    expect(user.role).toBe("CITIZEN");
    expect(fetchMock.mock.calls[0]?.[0]).toBe("http://localhost:8081/api/auth/login");
    expect(fetchMock.mock.calls[1]?.[0]).toBe("http://localhost:8081/api/registration-requests");
    expect(new Headers(fetchMock.mock.calls[1]?.[1]?.headers).get("Authorization")).toBe(
      "Bearer signed-session-token",
    );
    expect(window.sessionStorage.getItem("landledger-access-token")).toBe("signed-session-token");
  });

  it("maps the protected backend officer role to the existing frontend role", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            token: "signed-officer-token",
            user: {
              id: "officer-id",
              name: "Land Officer",
              email: "officer@example.test",
              role: "LAND_OFFICER",
              phone: "",
              active: true,
            },
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        ),
      ),
    );

    await expect(api.login("officer@example.test", "correct-password")).resolves.toMatchObject({
      role: "OFFICER",
    });
  });

  it("creates a parcel with its drawn boundary and registered owner", async () => {
    const boundary = [
      { latitude: 18.52, longitude: 73.85 },
      { latitude: 18.521, longitude: 73.85 },
      { latitude: 18.521, longitude: 73.851 },
    ];
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          id: "parcel-id",
          parcelId: "DEMO-01",
          boundary,
          history: [],
        }),
        { status: 201, headers: { "Content-Type": "application/json" } },
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      api.createParcel({
        land: {
          parcelId: "DEMO-01",
          surveyNumber: "12/A",
          village: "Sample Village",
          district: "Demo District",
          state: "Demo State",
          area: 1.25,
          landType: "Agricultural",
        },
        ownerEmail: "citizen@example.test",
        boundary,
      }),
    ).resolves.toMatchObject({ parcelId: "DEMO-01", boundary });

    expect(fetchMock.mock.calls[0]?.[0]).toBe("http://localhost:8081/api/lands");
    expect(JSON.parse(String(fetchMock.mock.calls[0]?.[1]?.body))).toMatchObject({
      ownerEmail: "citizen@example.test",
      boundary,
    });
  });
});
