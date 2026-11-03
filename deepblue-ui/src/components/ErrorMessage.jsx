export default function ErrorMessage({ error }) {
  if (!error) return null

  const details = Object.entries(error.details ?? {})

  return (
    <div role="alert" className="error">
      <strong>Error {error.status}:</strong> {error.message}
      {details.length > 0 && (
        <ul>
          {details.map(([field, msg]) => (
            <li key={field}>
              {field}: {msg}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}