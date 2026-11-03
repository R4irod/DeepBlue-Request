import { useQuery } from '@tanstack/react-query'
import { getCasesByStatus } from '../api/rescueCases'

export function useRescueCases(status) {
  return useQuery({
    queryKey: ['rescue-cases', status],
    queryFn: () => getCasesByStatus(status),
  })
}