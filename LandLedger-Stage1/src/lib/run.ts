import { toast } from "sonner";

export async function run(fn: () => unknown | Promise<unknown>, ok?: string): Promise<boolean> {
  try {
    await fn();
    if (ok) toast.success(ok);
    return true;
  } catch (error) {
    toast.error(error instanceof Error ? error.message : "The request could not be completed");
    return false;
  }
}
