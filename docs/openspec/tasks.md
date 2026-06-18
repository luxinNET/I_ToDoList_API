# Tasks OpenSpec

## Todo Lifecycle

- New todos start as `ACTIVE`.
- Completing a todo sets `status=COMPLETED` and `completed_at`.
- Uncompleting a todo restores `status=ACTIVE` and clears `completed_at`.
- Deletion is soft deletion.

## Views

- My Day: todos where `my_day=true`.
- Important: todos where `importance=IMPORTANT`.
- Planned: todos with `due_date` or `remind_at`.

## Authorization

Every todo query must include the authenticated `owner_id` or a verified shared-list permission.
