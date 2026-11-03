import { useQuery } from '@tanstack/react-query'
import {
  getAnimal,
  getAnimalTreatments,
  getTreatmentEligibility,
} from '../api/animals'

export function useAnimal(animalCode) {
  const enabled = Boolean(animalCode)

  const animal = useQuery({
    queryKey: ['animal', animalCode],
    queryFn: () => getAnimal(animalCode),
    enabled,
  })

  const treatments = useQuery({
    queryKey: ['animal', animalCode, 'treatments'],
    queryFn: () => getAnimalTreatments(animalCode),
    enabled,
  })

  const eligibility = useQuery({
    queryKey: ['animal', animalCode, 'eligibility'],
    queryFn: () => getTreatmentEligibility(animalCode),
    enabled,
  })

  return { animal, treatments, eligibility }
}