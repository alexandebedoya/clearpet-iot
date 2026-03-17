/**
 * Obtiene la URL base de la API
 * En desarrollo: http://localhost:3000
 * En producción: la URL de Vercel o servidor remoto
 */
export function getApiBaseUrl(): string {
  // Para cliente (navegador)
  if (typeof window !== 'undefined') {
    return process.env.NEXT_PUBLIC_API_URL || window.location.origin
  }

  // Para servidor (SSR)
  return process.env.NEXT_PUBLIC_API_URL || 'http://localhost:3000'
}

/**
 * Construye la URL completa para un endpoint de API
 */
export function getApiUrl(endpoint: string): string {
  const baseUrl = getApiBaseUrl()
  return `${baseUrl}${endpoint.startsWith('/') ? endpoint : '/' + endpoint}`
}
