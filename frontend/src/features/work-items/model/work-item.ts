export type WorkItemStatus = "BACKLOG" | "IN_PROGRESS" | "DONE";
export type WorkItemPriority = "HIGH" | "MEDIUM" | "LOW";
export type WorkItemSort = "RECENT" | "PRIORITY";

export interface WorkItem {
  id: string;
  title: string;
  description: string;
  status: WorkItemStatus;
  priority: WorkItemPriority;
  createdAt: string;
  updatedAt: string;
}

export interface WorkItemBoard {
  sortBy: WorkItemSort;
  totalCount: number;
  items: WorkItem[];
  cached: boolean;
}

export interface CreateWorkItemInput {
  title: string;
  description: string;
  priority: WorkItemPriority;
}