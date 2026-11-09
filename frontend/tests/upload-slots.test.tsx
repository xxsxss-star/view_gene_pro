import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { UploadSlots } from "@/components/upload-slots";
import { ApiClientError } from "@/lib/api/client";
import * as assetApi from "@/lib/api/assets";
import * as taskApi from "@/lib/api/tasks";

vi.mock("@/lib/api/assets", () => ({ uploadAsset: vi.fn() }));
vi.mock("@/lib/api/tasks", () => ({ createTask: vi.fn() }));

const createObjectURL = vi.fn((file: File) => `blob:${file.name}`);
const revokeObjectURL = vi.fn();

function asset(id: number, name: string) {
  return {
    id, originalName: name, mimeType: "image/png", fileSizeBytes: 8,
    contentUrl: `/api/v1/assets/${id}/content`, createdAt: "2026-11-08T00:00:00Z",
  };
}

function renderSlots(count = 2, onTaskCreated?: (taskId: number) => void) {
  return render(<UploadSlots templateId="travel-collage" requiredImageCount={count}
    acceptedMimeTypes={["image/png"]} maxFileSizeBytes={1024} onTaskCreated={onTaskCreated} />);
}

function png(name: string) {
  return new File([new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10])], name, { type: "image/png" });
}

beforeEach(() => {
  vi.mocked(assetApi.uploadAsset).mockReset();
  vi.mocked(taskApi.createTask).mockReset();
  createObjectURL.mockClear();
  revokeObjectURL.mockClear();
  Object.defineProperty(URL, "createObjectURL", { configurable: true, value: createObjectURL });
  Object.defineProperty(URL, "revokeObjectURL", { configurable: true, value: revokeObjectURL });
});

describe("UploadSlots", () => {
  it("keeps multiple upload slots independent and disables completion while incomplete", async () => {
    vi.mocked(assetApi.uploadAsset).mockResolvedValue(asset(1, "first.png"));
    renderSlots();
    fireEvent.change(screen.getByLabelText("选择第 1 张图片"), { target: { files: [png("first.png")] } });
    expect(await screen.findByText("上传成功：first.png")).toBeInTheDocument();
    expect(screen.getByText("图片 2")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "请先完成 2 张图片上传" })).toBeDisabled();
  });

  it("retries a failed upload", async () => {
    vi.mocked(assetApi.uploadAsset)
      .mockRejectedValueOnce(new ApiClientError(500, "FILE_STORAGE_ERROR", "图片保存失败"))
      .mockResolvedValueOnce(asset(2, "retry.png"));
    renderSlots(1);
    fireEvent.change(screen.getByLabelText("选择第 1 张图片"), { target: { files: [png("retry.png")] } });
    expect(await screen.findByRole("alert")).toHaveTextContent("图片保存失败");
    fireEvent.click(screen.getByRole("button", { name: "重试" }));
    expect(await screen.findByText("上传成功：retry.png")).toBeInTheDocument();
    expect(assetApi.uploadAsset).toHaveBeenCalledTimes(2);
  });

  it("supports replacement and removal while releasing old previews", async () => {
    vi.mocked(assetApi.uploadAsset)
      .mockResolvedValueOnce(asset(1, "first.png"))
      .mockResolvedValueOnce(asset(2, "second.png"));
    renderSlots(1);
    const input = screen.getByLabelText("选择第 1 张图片");
    fireEvent.change(input, { target: { files: [png("first.png")] } });
    expect(await screen.findByText("上传成功：first.png")).toBeInTheDocument();
    fireEvent.change(input, { target: { files: [png("second.png")] } });
    expect(await screen.findByText("上传成功：second.png")).toBeInTheDocument();
    expect(revokeObjectURL).toHaveBeenCalledWith("blob:first.png");
    fireEvent.click(screen.getByRole("button", { name: "移除" }));
    await waitFor(() => expect(screen.getByText("图片 1")).toBeInTheDocument());
    expect(revokeObjectURL).toHaveBeenCalledWith("blob:second.png");
  });

  it("enables completion only after every slot succeeds", async () => {
    vi.mocked(assetApi.uploadAsset)
      .mockResolvedValueOnce(asset(1, "one.png"))
      .mockResolvedValueOnce(asset(2, "two.png"));
    renderSlots();
    fireEvent.change(screen.getByLabelText("选择第 1 张图片"), { target: { files: [png("one.png")] } });
    fireEvent.change(screen.getByLabelText("选择第 2 张图片"), { target: { files: [png("two.png")] } });
    expect(await screen.findByRole("button", { name: "创建制作任务" })).toBeEnabled();
  });

  it("submits ordered asset ids once and reports the created task without leaving the page", async () => {
    const onTaskCreated = vi.fn();
    vi.mocked(assetApi.uploadAsset)
      .mockResolvedValueOnce(asset(11, "one.png"))
      .mockResolvedValueOnce(asset(22, "two.png"));
    vi.mocked(taskApi.createTask).mockResolvedValue({
      id: 9001, status: "QUEUED", stage: null, createdAt: "2026-11-08T00:00:00Z",
    });
    renderSlots(2, onTaskCreated);
    fireEvent.change(screen.getByLabelText("选择第 1 张图片"), { target: { files: [png("one.png")] } });
    fireEvent.change(screen.getByLabelText("选择第 2 张图片"), { target: { files: [png("two.png")] } });
    fireEvent.click(await screen.findByRole("button", { name: "创建制作任务" }));
    await waitFor(() => expect(taskApi.createTask).toHaveBeenCalledWith("travel-collage", [11, 22]));
    expect(onTaskCreated).toHaveBeenCalledWith(9001);
    expect(screen.getByRole("button", { name: "任务已创建，正在生成" })).toBeDisabled();
  });

  it("keeps uploaded assets available when task creation fails", async () => {
    vi.mocked(assetApi.uploadAsset).mockResolvedValue(asset(33, "keep.png"));
    vi.mocked(taskApi.createTask).mockRejectedValue(
      new ApiClientError(409, "ASSET_NOT_READY", "素材暂不可用"),
    );
    renderSlots(1);
    fireEvent.change(screen.getByLabelText("选择第 1 张图片"), { target: { files: [png("keep.png")] } });
    fireEvent.click(await screen.findByRole("button", { name: "创建制作任务" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("素材暂不可用");
    expect(screen.getByText("上传成功：keep.png")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "创建制作任务" })).toBeEnabled();
  });
});
