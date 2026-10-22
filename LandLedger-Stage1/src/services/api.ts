import { request, setAccessToken } from "@/services/backend";

export type Role = "CITIZEN" | "OFFICER" | "ADMIN";
export type AppStatus = "PENDING" | "UNDER_REVIEW" | "APPROVED" | "REJECTED";
export type TransferStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface PublicUser {
  id: string;
  name: string;
  email: string;
  role: Role;
  phone: string;
  active: boolean;
}
export type User = PublicUser;

export interface LandInput {
  parcelId: string;
  surveyNumber: string;
  village: string;
  district: string;
  state: string;
  area: number;
  landType: string;
  latitude?: number | undefined;
  longitude?: number | undefined;
}

export interface BoundaryPoint {
  latitude: number;
  longitude: number;
}

export interface ParcelCreateInput {
  land: LandInput;
  ownerEmail: string;
  boundary: BoundaryPoint[];
}

export interface Application extends LandInput {
  id: string;
  citizenId: string;
  applicantName: string;
  status: AppStatus;
  rejectionReason?: string | undefined;
  reviewedBy?: string | undefined;
  createdAt: string;
  updatedAt: string;
}

export interface HistoryEntry {
  ownerName: string;
  event: string;
  date: string;
  txRef: string | null;
  blockchainStatus: "PENDING" | "CONFIRMED" | "FAILED" | null;
}

export interface Parcel extends LandInput {
  ownerId: string | null;
  ownerName: string | null;
  boundary: BoundaryPoint[];
  history: HistoryEntry[];
}

export interface Transfer {
  id: string;
  parcelId: string;
  fromId: string;
  fromName: string;
  toEmail: string;
  toName: string;
  status: TransferStatus;
  reason?: string | undefined;
  createdAt: string;
  decidedAt?: string | undefined;
}

export interface Notice {
  id: string;
  userId: string;
  text: string;
  at: string;
}

export interface Audit {
  id: string;
  actor: string;
  action: string;
  at: string;
}

export interface DashboardStats {
  users: number;
  parcels: number;
  pending: number;
  transfers: number;
}

interface BackendUser extends Omit<PublicUser, "role"> {
  role: "CITIZEN" | "LAND_OFFICER" | "ADMIN";
}

interface AuthResponse {
  token: string;
  user: BackendUser;
}

function mapUser(user: BackendUser): PublicUser {
  return { ...user, role: user.role === "LAND_OFFICER" ? "OFFICER" : user.role };
}

async function authenticate(path: string, body: unknown): Promise<PublicUser> {
  const result = await request<AuthResponse>(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  setAccessToken(result.token);
  return mapUser(result.user);
}

export const login = (email: string, password: string) =>
  authenticate("/auth/login", { email, password });

export const register = (name: string, email: string, password: string, phone: string) =>
  authenticate("/auth/register", { name, email, password, phone });

export async function getCurrentUser(): Promise<PublicUser> {
  return mapUser(await request<BackendUser>("/auth/me"));
}

export function signOut() {
  setAccessToken(null);
}

export function hasSession() {
  return (
    typeof window !== "undefined" &&
    Boolean(window.sessionStorage.getItem("landledger-access-token"))
  );
}

export function updateProfile(name: string, phone: string): Promise<PublicUser> {
  return request<BackendUser>("/users/me", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name, phone }),
  }).then(mapUser);
}

export const listApplications = () => request<Application[]>("/registration-requests");

export function submitApplication(input: LandInput): Promise<Application> {
  return request("/registration-requests", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
}

export function resubmitApplication(id: string, input: LandInput): Promise<Application> {
  return request(`/registration-requests/${encodeURIComponent(id)}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
}

export function setApplicationStatus(
  id: string,
  status: AppStatus,
  reason?: string,
  boundary?: BoundaryPoint[],
): Promise<Application> {
  return request(`/registration-requests/${encodeURIComponent(id)}/review`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ status, remarks: reason, boundary }),
  });
}

export function listParcels(q = "", mineOnly = false): Promise<Parcel[]> {
  const params = new URLSearchParams();
  if (q.trim()) params.set("q", q.trim());
  if (mineOnly) params.set("mine", "true");
  return request(`/lands${params.size ? `?${params.toString()}` : ""}`);
}

export function createParcel(input: ParcelCreateInput): Promise<Parcel> {
  return request("/lands", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
}

export const listTransfers = () => request<Transfer[]>("/transfers");

export function requestTransfer(parcelId: string, toEmail: string): Promise<Transfer> {
  return request("/transfers", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ parcelId, toEmail }),
  });
}

export function decideTransfer(
  id: string,
  status: TransferStatus,
  reason?: string,
): Promise<Transfer> {
  return request(`/transfers/${encodeURIComponent(id)}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ status, reason }),
  });
}

export const listNotices = () => request<Notice[]>("/notifications");
export const listUsers = async () => (await request<BackendUser[]>("/admin/users")).map(mapUser);

export function updateUser(id: string, patch: { role?: Role; active?: boolean }) {
  return request<BackendUser>(`/admin/users/${encodeURIComponent(id)}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      ...patch,
      role: patch.role === "OFFICER" ? "LAND_OFFICER" : patch.role,
    }),
  }).then(mapUser);
}

export const listAudit = () => request<Audit[]>("/audit-logs");
export const stats = () => request<DashboardStats>("/dashboard/stats");
