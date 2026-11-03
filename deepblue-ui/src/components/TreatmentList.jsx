import ErrorMessage from './ErrorMessage'

export default function TreatmentList({ query }) {
  const { data, isPending, isError, error } = query

  return (
    <section>
      <h3>Tratamientos</h3>

      {isPending && <p>Cargando...</p>}
      {isError && <ErrorMessage error={error} />}
      {data && data.length === 0 && <p>Sin tratamientos registrados</p>}

      {data && data.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Tipo</th>
              <th>Especialista</th>
              <th>Descripción</th>
            </tr>
          </thead>
          <tbody>
            {data.map((t) => (
              <tr key={t.id}>
                <td>{t.performedAt}</td>
                <td>{t.type}</td>
                <td>{t.specialistCode}</td>
                <td>{t.description}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}