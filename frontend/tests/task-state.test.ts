import type { Task } from "@/lib/api/tasks";
import { mergeTaskSnapshots, shouldPollTasks } from "@/lib/tasks/task-state";

function task(id: number, status: Task["status"], updatedAt: string): Task {
  return {
    id,
    template: { id: "template", title: `模板 ${id}`, coverUrl: "/cover.svg" },
    status,
    stage: status === "PROCESSING" ? "GENERATING" : null,
    resultUrl: status === "SUCCEEDED" ? "/result.svg" : null,
    failure: status === "FAILED" ? { code: "FAILED", message: "失败", stage: "GENERATING" } : null,
    createdAt: `2026-11-08T00:00:0${id}Z`,
    startedAt: null,
    completedAt: null,
    updatedAt,
  };
}

describe("task snapshot management", () => {
  it("keeps tasks independent and never lets a stale processing snapshot overwrite a terminal state", () => {
    const previous = [
      task(1, "SUCCEEDED", "2026-11-08T00:00:09Z"),
      task(2, "PROCESSING", "2026-11-08T00:00:05Z"),
    ];
    const incoming = [
      task(1, "PROCESSING", "2026-11-08T00:00:04Z"),
      task(2, "FAILED", "2026-11-08T00:00:10Z"),
    ];

    const merged = mergeTaskSnapshots(previous, incoming);
    expect(merged.find((item) => item.id === 1)?.status).toBe("SUCCEEDED");
    expect(merged.find((item) => item.id === 2)?.status).toBe("FAILED");
  });

  it("polls while any task is active and stops only after all tasks terminate", () => {
    expect(shouldPollTasks([task(1, "SUCCEEDED", "2026-11-08T00:00:09Z"),
      task(2, "PROCESSING", "2026-11-08T00:00:05Z")])).toBe(true);
    expect(shouldPollTasks([task(1, "SUCCEEDED", "2026-11-08T00:00:09Z"),
      task(2, "FAILED", "2026-11-08T00:00:10Z")])).toBe(false);
    expect(shouldPollTasks([])).toBe(false);
  });
});
