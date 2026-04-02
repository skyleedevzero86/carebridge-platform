"use client";

import { startTransition, useEffect, useState } from "react";

import type {
  CreateWorkItemInput,
  WorkItemBoard,
  WorkItemPriority,
  WorkItemSort,
  WorkItemStatus,
} from "@/features/work-items/model/work-item";
import { workItemRepository } from "@/features/work-items/repository/http-work-item-repository";

const emptyBoard: WorkItemBoard = {
  sortBy: "RECENT",
  totalCount: 0,
  items: [],
  cached: false,
};

export function useWorkItemBoard(token: string) {
  const [board, setBoard] = useState<WorkItemBoard>(emptyBoard);
  const [sortBy, setSortBy] = useState<WorkItemSort>("RECENT");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<WorkItemPriority>("HIGH");

  async function refresh(nextSort: WorkItemSort) {
    setLoading(true);
    setError(null);

    try {
      const nextBoard = await workItemRepository.list(nextSort, token);
      setBoard(nextBoard);
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "알 수 없는 오류");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (!token) return;
    void refresh(sortBy);
    
  }, [sortBy, token]);

  function changeSort(nextSort: WorkItemSort) {
    startTransition(() => {
      setSortBy(nextSort);
    });
  }

  async function createWorkItem() {
    if (!title.trim()) return;
    const input: CreateWorkItemInput = { title: title.trim(), description, priority };
    try {
      await workItemRepository.create(input, token);
      setTitle("");
      setDescription("");
      await refresh(sortBy);
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "작업 생성 실패");
    }
  }

  async function updateStatus(workItemId: string, status: WorkItemStatus) {
    try {
      await workItemRepository.updateStatus(workItemId, status, token);
      await refresh(sortBy);
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "상태 변경 실패");
    }
  }

  return {
    board,
    loading,
    error,
    sortBy,
    title,
    description,
    priority,
    setTitle,
    setDescription,
    setPriority,
    changeSort,
    createWorkItem,
    updateStatus,
  };
}