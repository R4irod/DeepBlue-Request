import { useMutation, useQueryClient } from '@tanstack/react-query'
import { registerTreatment } from '../api/treatments'

export function useRegisterTreatment() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: registerTreatment,
    onSuccess: (created) => {
      // Refresca tratamientos y elegibilidad del animal
      queryClient.invalidateQueries({
        queryKey: ['animal', created.animalCode],
      })
    },
  })
}