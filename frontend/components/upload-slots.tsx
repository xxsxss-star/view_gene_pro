"use client";

import Image from "next/image";
import { ChangeEvent, useEffect, useRef, useState } from "react";
import { uploadAsset, UploadedAsset } from "@/lib/api/assets";
import { ApiClientError } from "@/lib/api/client";

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
}

export function UploadSlots({ templateId, requiredImageCount, acceptedMimeTypes, maxFileSizeBytes }: UploadSlotsProps) {
  const [slots, setSlots] = useState<UploadSlot[]>(() =>
    Array.from({ length: requiredImageCount }, (_, position) => ({ position, status: "EMPTY" })),
  );
  const slotsRef = useRef(slots);
  const requestVersions = useRef(Array.from({ length: requiredImageCount }, () => 0));

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
    requestVersions.current[position]++;
    const slot = slotsRef.current[position];
    if (slot.previewUrl) URL.revokeObjectURL(slot.previewUrl);
    replaceSlot(position, { position, status: "EMPTY" });
  }

  const allSucceeded = slots.every((slot) => slot.status === "SUCCESS");

  return (
    <section className="upload-section" aria-labelledby="upload-title">
      <h2 id="upload-title">上传图片</h2>
      <p>每个位置独立上传，可随时替换或移除。</p>
      <div className="upload-grid">
        {slots.map((slot) => (
          <article className={`upload-slot status-${slot.status.toLowerCase()}`} key={slot.position}>
            <div className="slot-preview">
              {slot.previewUrl
                ? <Image src={slot.previewUrl} alt={`第 ${slot.position + 1} 张图片预览`} width={480} height={320} unoptimized />
                : <span>图片 {slot.position + 1}</span>}
              {slot.status === "UPLOADING" && <span className="uploading-label" role="status">上传中…</span>}
            </div>
            {slot.asset && <p className="success-text">上传成功：{slot.asset.originalName}</p>}
            {slot.error && <p className="slot-error" role="alert">{slot.error}</p>}
            <div className="slot-actions">
              <label className="file-button">
                {slot.status === "EMPTY" ? "选择图片" : "替换图片"}
                <input type="file" accept={acceptedMimeTypes.join(",")} disabled={slot.status === "UPLOADING"}
                       aria-label={`选择第 ${slot.position + 1} 张图片`} onChange={(event) => chooseFile(slot.position, event)} />
              </label>
              {slot.status === "FAILED" && slot.file && slot.previewUrl && (
                <button type="button" onClick={() => void startUpload(slot.position, slot.file!, slot.previewUrl!)}>重试</button>
              )}
              {slot.status !== "EMPTY" && (
                <button type="button" className="text-button" onClick={() => remove(slot.position)}>移除</button>
              )}
            </div>
          </article>
        ))}
      </div>
      <button type="button" className="ready-button" disabled={!allSucceeded}>
        {allSucceeded ? "素材已就绪（下一阶段创建任务）" : `请先完成 ${requiredImageCount} 张图片上传`}
      </button>
    </section>
  );
}
