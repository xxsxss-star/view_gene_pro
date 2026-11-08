import { apiRequest } from "./client";

interface Envelope<T> { data: T; requestId: string; timestamp: string }

export interface UploadedAsset {
  id: number;
  originalName: string;
  mimeType: string;
  fileSizeBytes: number;
  contentUrl: string;
  createdAt: string;
}

export async function uploadAsset(templateId: string, file: File): Promise<UploadedAsset> {
  const body = new FormData();
  body.append("templateId", templateId);
  body.append("file", file);
  const response = await apiRequest<Envelope<UploadedAsset>>("/assets", { method: "POST", body });
  return response.data;
}
