import { useCallback, useEffect, useState } from "react";

export function useRemoteData<T>(load: () => Promise<T>) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await load();
      setData(result);
      return result;
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : "The request could not be completed";
      setError(message);
      throw cause;
    } finally {
      setLoading(false);
    }
  }, [load]);

  useEffect(() => {
    void reload().catch(() => {});
  }, [reload]);

  return { data, loading, error, reload };
}
