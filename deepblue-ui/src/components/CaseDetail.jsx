import { useAnimal } from '../hooks/useAnimal'
import ErrorMessage from './ErrorMessage'
import TreatmentList from './TreatmentList'
import ChangeStatus from './ChangeStatus'
import TreatmentForm from './TreatmentForm'

export default function CaseDetail({ caseItem, onChanged }) {
  const { animal, treatments, eligibility } = useAnimal(caseItem.animalCode)

  return (
    <div>
      <section>
        <h2>Caso {caseItem.caseCode}</h2>
        <p>
          <strong>Estado:</strong> {caseItem.status} <br />
          <strong>Centro:</strong> {caseItem.centerCode} <br />
          <strong>Lugar:</strong> {caseItem.rescueLocation} <br />
          <strong>Fecha:</strong> {caseItem.rescueDate}
        </p>
      </section>
      <ChangeStatus
        key={caseItem.caseCode}
        caseItem={caseItem}
        onChanged={onChanged}
      />
      <section>
        <h3>Animal {caseItem.animalCode}</h3>
        {animal.isPending && <p>Cargando...</p>}
        {animal.isError && <ErrorMessage error={animal.error} />}

        {/* Muestra cualquier campo que devuelva tu API de animales */}
        {animal.data && (
          <ul>
            {Object.entries(animal.data).map(([key, value]) => (
              <li key={key}>
                <strong>{key}:</strong> {String(value)}
              </li>
            ))}
          </ul>
        )}

        {eligibility.data && (
          <p>
            Puede recibir tratamiento:{' '}
            <span className={eligibility.data.eligible ? 'ok' : 'bad'}>
              {eligibility.data.eligible ? 'Sí' : 'No'}
            </span>
          </p>
        )}
      </section>

      <TreatmentList query={treatments} />
      <TreatmentForm key={caseItem.animalCode} animalCode={caseItem.animalCode} />
    </div>
  )
}