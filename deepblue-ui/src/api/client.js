export class ApiError extends Error {
  constructor({ status, message, details }) {
    super(message)
    this.status = status
    this.details = details ?? {}
  }
}

export async function request(path, options = {}) {
  const response = await fetch(path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })

  if (!response.ok) {
    // El backend responde ErrorResponse; si no (backend caído), usamos un mensaje genérico
    const body = await response.json().catch(() => null)
    throw new ApiError({
      status: body?.status ?? response.status,
      message: body?.message ?? 'No se pudo contactar el servidor',
      details: body?.details,
    })
  }

  return response.json()
}