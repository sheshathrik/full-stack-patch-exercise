# Notes

### Summary of Changes
1. **SQL Operator Precedence**: Fixed missing parentheses around `OR` conditions in `TaskRepository.java`, `search_tasks.sql`, and `task_search_package.sql`. This prevents status filter bypass and stops archived tasks from leaking into search results.
2. **Removed Latency Bottleneck**: Eliminated artificial `Thread.sleep` (up to 1,000ms) in `TaskController.java`, reducing search latency from ~1,050ms to ~16ms.
3. **Input Validation & Error Handling**: Validated `status` enum inputs (returning HTTP 400 Bad Request rather than uncaught 500 errors) and bounded `page`/`pageSize` to eliminate negative sublist crashes.
4. **Data Fetching & Race Conditions**: Added `AbortController` cancellation and 300ms debouncing to `useTasks.js`; resolved stuck loading state on network failures; reset pagination to page 1 upon search/filter changes.
5. **Responsive UI & Accessibility**: Added responsive horizontal scroll wrapper, full-width mobile controls, narrow-screen (<400px) pagination adjustments, and accessible `aria-label` tags.
6. **Automated Test Suite**: Added Spring Boot integration tests verifying status filtering, archive exclusion, validation, and latency.

### What I Chose Not to Change
- **In-Memory Slicing**: Kept repository query returning `List<Task>` sliced in memory rather than refactoring to Spring Data `Pageable`. For current dataset size, this kept the diff minimal and low-risk.
- **Task Mutation APIs**: Omitted creating new CRUD endpoints (`POST /api/tasks`, editing, deleting) to stay strictly focused on fixing the search and viewer experience.

### Biggest Remaining Risk
- **Scalability of In-Memory Native Queries**: Loading all matching rows into application memory will degrade performance and spike heap usage as task volume grows beyond thousands of rows. Database-level pagination (`LIMIT`/`OFFSET`) and proper indexing are needed for large-scale production.

### Tools and AI Used
- Used AI to identify the SQL precedence flaw, draft the debounced fetch hook, and generate integration test cases. Manually tuned mobile breakpoints (<400px, <640px) and refined error response structures.

