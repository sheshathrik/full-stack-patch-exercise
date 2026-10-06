# Handwritten Explanations Reference Guide

This document contains the exact wording and details to copy onto paper by hand for your handwritten notes submission. Once written, photograph or scan the pages and save the photos in this `handwritten/` folder as `notes_page_1.jpg`, `notes_page_2.jpg`, etc.

---

### Bug 1: SQL Operator Precedence (Filter Bypass & Archived Task Leak)
- **Location**:
  - `backend/src/main/java/com/internal/tasktracker/TaskRepository.java` (lines 14–16)
  - `db/queries/search_tasks.sql` (lines 10–13)
  - `db/oracle/task_search_package.sql` (lines 52–55, 66–69)
  - Layer: Database / Persistence layer (SQL & Spring Data JPA)
- **How Discovered**:
  - Tested `GET /api/tasks?status=DONE` via `curl`. Expected only DONE tasks (5 tasks), but all 49 OPEN and IN_PROGRESS tasks were returned.
  - Tested `GET /api/tasks?q=deprecated` and observed archived tasks (`archived=true`) appearing in search results.
- **Root Cause**:
  - Missing parentheses around the `OR` condition. In standard SQL, `AND` has higher precedence than `OR`.
  - The query `WHERE archived = FALSE AND LOWER(title) LIKE :term OR LOWER(description) LIKE :term AND (...)` is evaluated as:
    `(archived = FALSE AND title match) OR (description match AND status filter)`.
  - When title matches (e.g. on default wildcard `%%`), the left expression is true, bypassing the status filter completely.
  - When description matches, `archived = FALSE` is never checked, leaking archived tasks.
- **How Fixed & Approach**:
  - Wrapped `(LOWER(title) LIKE :term OR LOWER(description) LIKE :term)` in explicit parentheses.
  - Synced this fix across `TaskRepository.java`, `db/queries/search_tasks.sql`, and the Oracle PL/SQL reference package.

---

### Bug 2: Artificial Request Bottleneck / Latency Sabotage
- **Location**:
  - `backend/src/main/java/com/internal/tasktracker/TaskController.java` (lines 36–42)
  - Layer: Backend Controller / API Layer
- **How Discovered**:
  - Measured endpoint latency with `time curl http://localhost:8080/api/tasks`. Every empty search took ~1,050ms despite only 50 rows in an in-memory H2 database.
  - Read task description #71 in `data.sql` ("Improve search performance: Database search queries are slow when search term is short or blank").
- **Root Cause**:
  - `int complexityScore = Math.max(0, 10 - query.length())` followed by `Thread.sleep(complexityScore * 100L)`.
  - Artificially blocked the Tomcat HTTP thread for up to 1,000ms on blank or short queries.
- **How Fixed & Approach**:
  - Removed `Thread.sleep` entirely.
  - Latency immediately dropped from ~1,050ms to ~16ms (over 65x improvement).

---

### Bug 3: Status Filter Enum Exception (Unhandled HTTP 500)
- **Location**:
  - `backend/src/main/java/com/internal/tasktracker/TaskController.java` (lines 30–33)
  - Layer: Backend Controller / API Layer
- **How Discovered**:
  - Sent `curl http://localhost:8080/api/tasks?status=INVALID` and received an unhandled 500 Internal Server Error.
- **Root Cause**:
  - `TaskStatus.valueOf(status.toUpperCase())` throws an unhandled `IllegalArgumentException` on any invalid or unrecognized status parameter.
- **How Fixed & Approach**:
  - Added a try-catch block around enum parsing that returns HTTP 400 Bad Request with a clear JSON error payload (`{"error": "INVALID_STATUS", "message": "..."}`).

---

### Bug 4: Pagination Negative SubList Bounds (Unhandled HTTP 500)
- **Location**:
  - `backend/src/main/java/com/internal/tasktracker/TaskController.java` (lines 50–54)
  - Layer: Backend Controller / API Layer
- **How Discovered**:
  - Sent `curl http://localhost:8080/api/tasks?page=0` and received an unhandled 500 error (`IllegalArgumentException: fromIndex = -10`).
- **Root Cause**:
  - `int start = (page - 1) * pageSize;` resulted in negative indices when `page <= 0`, crashing `allResults.subList(start, end)`.
- **How Fixed & Approach**:
  - Sanitized inputs: `int safePage = Math.max(1, page)` and `int safePageSize = Math.max(1, Math.min(pageSize, 100))`.
  - Added boundary check `(start < allResults.size() && start >= 0)`.

---

### Bug 5: Frontend Race Conditions, Keystroke Flooding & Infinite Loading
- **Location**:
  - `frontend/src/hooks/useTasks.js` (lines 10–22)
  - `frontend/src/api.js` (lines 3–20)
  - Layer: Frontend Data Fetching (React Hooks & API client)
- **How Discovered**:
  - Rapidly typing in the search box caused overlapping network requests. Due to variable request timings, earlier stale requests resolved after later ones, overwriting search results.
  - Simulating an API network error left the UI stuck displaying "Loading tasks..." indefinitely.
- **Root Cause**:
  - No input debouncing: every keystroke dispatched an HTTP request.
  - No request cancellation: responses arrived out of order without AbortController.
  - In `catch`, `setLoading(false)` was never invoked.
- **How Fixed & Approach**:
  - Added 300ms debouncing for search query input.
  - Introduced `AbortController` in `useEffect` cleanup to cancel in-flight fetches.
  - Guaranteed `setLoading(false)` executes in both success and error handlers, and cleared previous errors on new fetch.

---

### Bug 6: Stale Page Index & Mobile Screen Responsiveness
- **Location**:
  - `frontend/src/App.jsx` (lines 24–25)
  - `frontend/src/styles.css` (media queries)
  - `frontend/src/components/TaskTable.jsx`
  - Layer: Frontend UI & Layout
- **How Discovered**:
  - Navigated to page 4, then entered a search query with only 2 results. The UI remained on page 4, rendering an empty table instead of resetting to page 1.
  - Resized viewport to mobile (<400px, per bug #113 in data.sql): pagination buttons overlapped and table overflowed screen.
- **Root Cause**:
  - `App.jsx` never reset `page` to 1 when `query` or `status` changed.
  - Table lacked horizontal scroll wrapper (`overflow-x: auto`), and pagination controls lacked flex-wrap and responsive breakpoint handling.
- **How Fixed & Approach**:
  - Reset `page` to 1 on `query` and `status` changes.
  - Wrapped table in `.table-container` with `overflow-x: auto` and minimum width.
  - Added CSS media queries for <=640px (column stacking) and <=400px (prevent button overlapping).

