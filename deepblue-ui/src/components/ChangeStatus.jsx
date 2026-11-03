import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { changeCaseStatus } from '../api/rescueCases'
import { STATUSES } from './CaseList'
import ErrorMessage from './ErrorMessage'

export default function ChangeStatus({ caseItem, onChanged }) {
  const queryClient = useQueryClient()
  const [next, setNext] = useState(caseItem.status)

  const mutation = useMutation({
    mutationFn: (status) => changeCaseStatus(caseItem.caseCode, status),
    onSuccess: (updatedCase) => {
      // Todas las listas por estado quedaron desactualizadas
      queryClient.invalidateQueries({ queryKey: ['rescue-cases'] })
      onChanged(updatedCase)
    },
  })

  return (
    <section>
      <h3>Cambiar estado</h3>

      <label>
        Nuevo estado
        <select value={next} onChange={(e) => setNext(e.target.value)}>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </label>

      <button disabled={mutation.isPending} onClick={() => mutation.mutate(next)}>
        {mutation.isPending ? 'Aplicando...' : 'Aplicar'}
      </button>

      <ErrorMessage error={mutation.error} />
    </section>
  )
}