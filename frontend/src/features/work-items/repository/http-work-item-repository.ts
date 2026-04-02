import type {
  CreateWorkItemInput,
  WorkItem,
  WorkItemBoard,
  WorkItemSort,
  WorkItemStatus,
} from "@/features/work-items/model/work-item";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

function authHeaders(token: string): HeadersInit {
  return {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
  };
}

export const workItemRepository = {
  async list(sortBy: WorkItemSort, token: string): Promise<WorkItemBoard> {
    const response = await fetch(
      `${apiBaseUrl}/api/work-items?sortBy=${sortBy}`,
      {
        cache: "no-store",
        headers: authHeaders(token),
      },
    );

    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      throw new Error((body as { message?: string }).message ?? "작업 목록을 불러올 수 없습니다.");
    }

    return response.json();
  },

  async create(input: CreateWorkItemInput, token: string): Promise<WorkItem> {
    const response = await fetch(`${apiBaseUrl}/api/work-items`, {
      method: "POST",
      headers: authHeaders(token),
      body: JSON.stringify(input),
    });

    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      throw new Error((body as { message?: string }).message ?? "작업 생성에 실패했습니다.");
    }

    return response.json();
  },

  async updateStatus(id: string, status: WorkItemStatus, token: string): Promise<WorkItem> {
    const response = await fetch(`${apiBaseUrl}/api/work-items/${id}/status`, {
      method: "PATCH",
      headers: authHeaders(token),
      body: JSON.stringify({ status }),
    });

    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      throw new Error((body as { message?: string }).message ?? "상태 변경에 실패했습니다.");
    }

    return response.json();
  },
};