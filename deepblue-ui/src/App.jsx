import { useState } from 'react'
import CaseList from './components/CaseList'
import CaseDetail from './components/CaseDetail'

export default function App() {
  const [selected, setSelected] = useState(null)

  return (
    <>
      <h1>DeepBlue Rescue</h1>
      <main className="layout">
        <CaseList selected={selected} onSelect={setSelected} />
        {selected ? (
          <CaseDetail caseItem={selected} onChanged={setSelected} />
        ) : (
          <p>Selecciona un caso</p>
        )}
      </main>
    </>
  )
}