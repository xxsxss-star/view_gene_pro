import type { Task } from "@/lib/api/tasks";

export function isTaskTerminal(task: Task): boolean {
  return task.status === "SUCCEEDED" || task.status === "FAILED";
}

export function shouldPollTasks(tasks?: Task[]): boolean {
  return Boolean(tasks?.some((task) => !isTaskTerminal(task)));
}

export function mergeTaskSnapshots(previous: Task[] | undefined, incoming: Task[]): Task[] {
  if (!previous) return sortNewestFirst(incoming);
  const previousById = new Map(previous.map((task) => [task.id, task]));
  const merged = incoming.map((next) => {
    const current = previousById.get(next.id);
    if (!current) return next;
    // 迟到的 PROCESSING 响应绝不能覆盖已经观察到的成功或失败终态。
    if (isTaskTerminal(current) && !isTaskTerminal(next)) return current;
    const currentTime = Date.parse(current.updatedAt);
    const nextTime = Date.parse(next.updatedAt);
    return Number.isFinite(currentTime) && Number.isFinite(nextTime) && nextTime < currentTime ? current : next;
  });
  return sortNewestFirst(merged);
}

function sortNewestFirst(tasks: Task[]): Task[] {
  return [...tasks].sort((left, right) => {
    const byTime = Date.parse(right.createdAt) - Date.parse(left.createdAt);
    return byTime || right.id - left.id;
  });
}
