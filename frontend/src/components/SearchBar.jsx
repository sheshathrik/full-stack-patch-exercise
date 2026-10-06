export default function SearchBar({ value, onChange }) {
  return (
    <input
      type="text"
      className="search-input"
      placeholder="Search tasks..."
      aria-label="Search tasks by title or description"
      value={value}
      onChange={(e) => onChange(e.target.value)}
    />
  );
}
