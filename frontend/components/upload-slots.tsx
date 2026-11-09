"use client";

import Image from "next/image";
import { ChangeEvent, useEffect, useRef, useState } from "react";
import { uploadAsset, UploadedAsset } from "@/lib/api/assets";
import { ApiClientError } from "@/lib/api/client";
import { createTask } from "@/lib/api/tasks";

type UploadStatus = "EMPTY" | "UPLOADING" | "SUCCESS" | "FAILED";

interface UploadSlot {
  position: number;
  status: UploadStatus;
  file?: File;
  previewUrl?: string;
  asset?: UploadedAsset;
  error?: string;
}

interface UploadSlotsProps {
  templateId: string;
  requiredImageCount: number;
  acceptedMimeTypes: string[];
  maxFileSizeBytes: number;
  onTaskCreated?: (taskId: number) => void;
}

export function UploadSlots({ templateId, requiredImageCount, acceptedMimeTypes, maxFileSizeBytes, onTaskCreated }: UploadSlotsProps) {
  const [slots, setSlots] = useState<UploadSlot[]>(() =>
    Array.from({ length: requiredImageCount }, (_, position) => ({ position, status: "EMPTY" })),
  );
  const slotsRef = useRef(slots);
  const requestVersions = useRef(Array.from({ length: requiredImageCount }, () => 0));
  const [isCreatingTask, setCreatingTask] = useState(false);
  const [isTaskCreated, setTaskCreated] = useState(false);
  const [taskError, setTaskError] = useState("");

  useEffect(() => { slotsRef.current = slots; }, [slots]);

  useEffect(() => () => {
    // blob URL 只属于当前页面，离开详情页时必须释放浏览器内存。
    slotsRef.current.forEach((slot) => { if (slot.previewUrl) URL.revokeObjectURL(slot.previewUrl); });
  }, []);

  function replaceSlot(position: number, update: UploadSlot) {
    setSlots((current) => current.map((slot) => slot.position === position ? update : slot));
  }

  function chooseFile(position: number, event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;
    setTaskCreated(false);
    const previous = slotsRef.current[position];
    if (previous.previewUrl) URL.revokeObjectURL(previous.previewUrl);
    const previewUrl = URL.createObjectURL(file);
    if (!acceptedMimeTypes.includes(file.type)) {
      replaceSlot(position, { position, status: "FAILED", file, previewUrl, error: "请选择模板支持的图片格式" });
      return;
    }
    if (file.size > maxFileSizeBytes) {
      replaceSlot(position, { position, status: "FAILED", file, previewUrl, error: "图片超过模板允许的大小" });
      return;
    }
    void startUpload(position, file, previewUrl);
  }

  async function startUpload(position: number, file: File, previewUrl: string) {
    const version = ++requestVersions.current[position];
    replaceSlot(position, { position, status: "UPLOADING", file, previewUrl });
    try {
      const asset = await uploadAsset(templateId, file);
      // 替换文件后旧请求可能晚返回；版本号可阻止旧结果覆盖新槽位。
      if (requestVersions.current[position] === version) {
        replaceSlot(position, { position, status: "SUCCESS", file, previewUrl, asset });
      }
    } catch (error) {
      if (requestVersions.current[position] === version) {
        const message = error instanceof ApiClientError ? error.message : "上传失败，请重试";
        replaceSlot(position, { position, status: "FAILED", file, previewUrl, error: message });
      }
    }
  }

  function remove(position: number) {
    setTaskCreated(false);
    requestVersions.current[position]++;
    const slot = slotsRef.current[position];
    if (slot.previewUrl) URL.revokeObjectURL(slot.previewUrl);
    replaceSlot(position, { position, status: "EMPTY" });
  }

  const allSucceeded = slots.every((slot) => slot.status === "SUCCESS");

  async function submitTask() {
    if (!allSucceeded || isCreatingTask || isTaskCreated) return;
    setCreatingTask(true);
    setTaskError("");
    try {
      // slots 始终按 position 排列，因此 assetIds 会保持用户看到的上传位顺序。
      const task = await createTask(templateId, slots.map((slot) => slot.asset!.id));
      setTaskCreated(true);
      setCreatingTask(false);
      onTaskCreated?.(task.id);
    } catch (error) {
      setTaskError(error instanceof ApiClientError ? error.message : "创建任务失败，请重试");
      setCreatingTask(false);
    }
  }

  return (
    <section className="upload-section" aria-labelledby="upload-title">
      <h2 id="upload-title">上传图片</h2>
      <p>每个位置独立上传，可随时替换或移除。</p>
      <div className="upload-grid">
        {slots.map((slot) => (
          <article className={`upload-slot status-${slot.status.toLowerCase()}`} key={slot.position}>
            <input
              id={`upload-slot-${templateId}-${slot.position}`}
              className="slot-file-input"
              type="file"
              hidden
              accept={acceptedMimeTypes.join(",")}
              disabled={slot.status === "UPLOADING"}
              aria-label={`选择第 ${slot.position + 1} 张图片`}
              onChange={(event) => chooseFile(slot.position, event)}
            />
            <label
              className="slot-preview"
              htmlFor={`upload-slot-${templateId}-${slot.position}`}
              aria-label={`点击上传第 ${slot.position + 1} 张图片`}
            >
              {slot.previewUrl
                ? <Image src={slot.previewUrl} alt={`第 ${slot.position + 1} 张图片预览`} width={480} height={320} unoptimized />
                : <span>图片 {slot.position + 1}</span>}
              {slot.status === "UPLOADING" && <span className="uploading-label" role="status">上传中…</span>}
            </label>
            {slot.asset && <p className="success-text">上传成功：{slot.asset.originalName}</p>}
            {slot.error && <p className="slot-error" role="alert">{slot.error}</p>}
            {slot.status !== "EMPTY" && (
            <div className="slot-actions">
              {slot.status === "FAILED" && slot.file && slot.previewUrl && (
                <button type="button" onClick={() => void startUpload(slot.position, slot.file!, slot.previewUrl!)}>重试</button>
              )}
              <button type="button" className="text-button" onClick={() => remove(slot.position)}>移除</button>
            </div>
            )}
          </article>
        ))}
      </div>
      {taskError && <p className="slot-error" role="alert">{taskError}</p>}
      <button type="button" className="ready-button" disabled={!allSucceeded || isCreatingTask || isTaskCreated}
              onClick={() => void submitTask()}>
        {isCreatingTask ? "正在创建任务…" : isTaskCreated ? "任务已创建，正在生成" : allSucceeded ? "创建制作任务" : `请先完成 ${requiredImageCount} 张图片上传`}
      </button>
    </section>
  );
}
