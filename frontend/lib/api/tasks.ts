import { apiRequest } from "./client";

interface Envelope<T> { data: T; requestId: string; timestamp: string }

export type TaskStatus = "QUEUED" | "PROCESSING" | "SUCCEEDED" | "FAILED";
export type TaskStage = "MATERIAL_CHECK" | "GENERATING" | "FINALIZING";

export interface CreatedTask {
  id: number;
  status: TaskStatus;
  stage: TaskStage | null;
  createdAt: string;
}

export interface Task {
  id: number;
  template: { id: string; title: string; coverUrl: string };
  status: TaskStatus;
  stage: TaskStage | null;
  resultUrl: string | null;
  failure: { code: string; message: string; stage: TaskStage | null } | null;
  createdAt: string;
  startedAt: string | null;
  completedAt: string | null;
  updatedAt: string;
}

export async function createTask(templateId: string, assetIds: number[]): Promise<CreatedTask> {
  return (await apiRequest<Envelope<CreatedTask>>("/tasks", {
    method: "POST",
    body: JSON.stringify({ templateId, assetIds }),
  })).data;
}

export async function getTasks(status?: TaskStatus): Promise<Task[]> {
  const query = status ? `?status=${status}` : "";
  return (await apiRequest<Envelope<Task[]>>(`/tasks${query}`)).data;
}

export async function getTask(taskId: number): Promise<Task> {
  return (await apiRequest<Envelope<Task>>(`/tasks/${taskId}`)).data;
}
