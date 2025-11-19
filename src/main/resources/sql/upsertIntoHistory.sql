INSERT INTO task_history(task_id, last_viewed)
VALUES (?, now())
    ON CONFLICT (task_id) DO UPDATE SET last_viewed = EXCLUDED.last_viewed;