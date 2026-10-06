function formatStatus(status) {
  if (!status) return '';
  return status.replace(/_/g, ' ');
}

export default function TaskTable({ tasks, loading, error }) {
  if (loading) {
    return <div className="state-message" role="status" aria-live="polite">Loading tasks...</div>;
  }

  if (error) {
    return (
      <div className="state-message error" role="alert">
        <strong>Error:</strong> {error}
      </div>
    );
  }

  if (!tasks || tasks.length === 0) {
    return <div className="state-message">No tasks found.</div>;
  }

  return (
    <div className="table-container">
      <table className="task-table">
        <thead>
          <tr>
            <th scope="col" className="col-id">ID</th>
            <th scope="col" className="col-title">Title & Description</th>
            <th scope="col" className="col-status">Status</th>
            <th scope="col" className="col-priority">Priority</th>
            <th scope="col" className="col-assignee">Assignee</th>
          </tr>
        </thead>
        <tbody>
          {tasks.map((task) => (
            <tr key={task.id}>
              <td className="col-id">{task.id}</td>
              <td className="col-title">
                <div className="task-title">{task.title}</div>
                {task.description && <div className="task-desc">{task.description}</div>}
              </td>
              <td className="col-status">
                <span className={`status-badge ${(task.status || '').toLowerCase()}`}>
                  {formatStatus(task.status)}
                </span>
              </td>
              <td className="col-priority">{task.priority || '\u2014'}</td>
              <td className="col-assignee">{task.assignee || '\u2014'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
