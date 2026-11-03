import { useReducer } from 'react'
import { useRegisterTreatment } from '../hooks/useRegisterTreatment'
import ErrorMessage from './ErrorMessage'

const TREATMENT_TYPES = [
  'WOUND_CARE',
  'HYDRATION',
  'MEDICATION',
  'SURGERY',
  'NUTRITION',
  'PHYSIOTHERAPY',
  'OBSERVATION',
]

const initialState = {
  specialistCode: '',
  performedAt: '',
  type: 'WOUND_CARE',
  description: '',
}

function formReducer(state, action) {
  switch (action.type) {
    case 'field':
      return { ...state, [action.name]: action.value }
    case 'reset':
      return initialState
    default:
      return state
  }
}

export default function TreatmentForm({ animalCode }) {
  const [form, dispatch] = useReducer(formReducer, initialState)
  const mutation = useRegisterTreatment()

  const handleChange = (e) =>
    dispatch({ type: 'field', name: e.target.name, value: e.target.value })

  const handleSubmit = (e) => {
    e.preventDefault()
    mutation.mutate(
      { animalCode, ...form },
      { onSuccess: () => dispatch({ type: 'reset' }) },
    )
  }

  return (
    <section>
      <h3>Registrar tratamiento</h3>

      <form onSubmit={handleSubmit}>
        <label>
          Código del especialista
          <input
            name="specialistCode"
            value={form.specialistCode}
            onChange={handleChange}
          />
        </label>

        <label>
          Fecha y hora
          <input
            type="datetime-local"
            name="performedAt"
            value={form.performedAt}
            onChange={handleChange}
          />
        </label>

        <label>
          Tipo
          <select name="type" value={form.type} onChange={handleChange}>
            {TREATMENT_TYPES.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </label>

        <label>
          Descripción
          <textarea
            name="description"
            rows={3}
            value={form.description}
            onChange={handleChange}
          />
        </label>

        <button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Guardando...' : 'Registrar'}
        </button>
      </form>

      <ErrorMessage error={mutation.error} />
      {mutation.isSuccess && <p className="ok">Tratamiento registrado ✔</p>}
    </section>
  )
}