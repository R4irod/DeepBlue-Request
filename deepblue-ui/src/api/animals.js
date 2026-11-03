import { request } from './client'

export const getAnimal = (animalCode) =>
  request(`/api/animals/${animalCode}`)

export const getAnimalTreatments = (animalCode) =>
  request(`/api/animals/${animalCode}/treatments`)

export const getTreatmentEligibility = (animalCode) =>
  request(`/api/animals/${animalCode}/treatment-eligibility`)