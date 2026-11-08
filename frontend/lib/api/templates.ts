import { apiRequest } from "./client";

interface Envelope<T> { data: T; requestId: string; timestamp: string }

export interface TemplateCategory { code: string; name: string }
export interface TemplateCard {
  id: string;
  title: string;
  categoryCode: string;
  categoryName: string;
  coverUrl: string;
  description: string;
  requiredImageCount: number;
}
export interface TemplateList {
  categories: TemplateCategory[];
  items: TemplateCard[];
  total: number;
}
export interface TemplateDetail extends TemplateCard {
  acceptedMimeTypes: string[];
  maxFileSizeBytes: number;
}

export async function getTemplates(categoryCode?: string): Promise<TemplateList> {
  const query = categoryCode ? `?categoryCode=${encodeURIComponent(categoryCode)}` : "";
  return (await apiRequest<Envelope<TemplateList>>(`/templates${query}`)).data;
}

export async function getTemplate(templateId: string): Promise<TemplateDetail> {
  return (await apiRequest<Envelope<TemplateDetail>>(`/templates/${encodeURIComponent(templateId)}`)).data;
}
