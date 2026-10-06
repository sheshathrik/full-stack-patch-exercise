import { useState, useEffect } from 'react';
import { fetchTasks } from '../api';

export function useTasks(query, status, page, pageSize) {
  const [tasks, setTasks] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [debouncedQuery, setDebouncedQuery] = useState(query);

  // Debounce search input to avoid overwhelming backend on rapid keystrokes
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(query);
    }, 300);

    return () => clearTimeout(timer);
  }, [query]);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);

    fetchTasks({
      query: debouncedQuery,
      status,
      page,
      pageSize,
      signal: controller.signal,
    })
      .then((data) => {
        setTasks(data.items || []);
        setTotal(data.total || 0);
        setLoading(false);
      })
      .catch((err) => {
        if (err.name === 'AbortError') {
          return;
        }
        setError(err.message);
        setLoading(false);
      });

    return () => {
      controller.abort();
    };
  }, [debouncedQuery, status, page, pageSize]);

  return { tasks, total, loading, error };
}
