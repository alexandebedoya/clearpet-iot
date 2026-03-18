'use client'

import { useState, useCallback, useEffect } from 'react'
import { UsuarioPublico, LoginRequest, RegisterRequest, AuthContext } from '@/lib/types'
import { getApiUrl } from '@/lib/api-config'

/**
 * Hook para manejar autenticación en toda la app
 */
export function useAuth(): AuthContext {
  const [usuario, setUsuario] = useState<UsuarioPublico | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isAutenticado, setIsAutenticado] = useState(false)

  // ======================== CARGAR STATE DEL STORAGE ========================
  useEffect(() => {
    const cargarAuth = () => {
      try {
        const tokenGuardado = localStorage.getItem('auth_token')
        const usuarioGuardado = localStorage.getItem('auth_usuario')

        if (tokenGuardado && usuarioGuardado) {
          setToken(tokenGuardado)
          setUsuario(JSON.parse(usuarioGuardado))
          setIsAutenticado(true)
        }
      } catch (error) {
        console.error('[AUTH] Error cargando auth desde storage:', error)
        logout() // Cambiado de limpiar() a logout()
      } finally {
        setIsLoading(false)
      }
    }

    cargarAuth()
  }, [])

  // ======================== LOGIN ========================
  const login = useCallback(
    async (email: string, password: string) => {
      try {
        setIsLoading(true)

        const response = await fetch(getApiUrl('/api/auth/login'), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email, password } as LoginRequest),
        })

        if (!response.ok) {
          const data = await response.json()
          throw new Error(data.error || 'Error en login')
        }

        const data = await response.json()

        // Guardar en estado y storage
        setToken(data.data.token)
        setUsuario(data.data.usuario)
        setIsAutenticado(true)

        localStorage.setItem('auth_token', data.data.token)
        localStorage.setItem('auth_usuario', JSON.stringify(data.data.usuario))

        console.log('[AUTH] ✅ Login exitoso')
      } catch (error) {
        const mensaje = error instanceof Error ? error.message : 'Error desconocido'
        console.error('[AUTH] Error en login:', mensaje)
        logout() // Cambiado de limpiar() a logout()
        throw error
      } finally {
        setIsLoading(false)
      }
    },
    []
  )

  // ======================== REGISTER ========================
  const register = useCallback(
    async (data: RegisterRequest) => {
      try {
        setIsLoading(true)

        if (data.password !== data.confirmPassword) {
          throw new Error('Las contraseñas no coinciden')
        }

        if (data.password.length < 8) {
          throw new Error('La contraseña debe tener mínimo 8 caracteres')
        }

        const response = await fetch(getApiUrl('/api/auth/register'), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(data),
        })

        if (!response.ok) {
          const responseData = await response.json()
          throw new Error(responseData.error || 'Error en registro')
        }

        const responseData = await response.json()

        setToken(responseData.data.token)
        setUsuario(responseData.data.usuario)
        setIsAutenticado(true)

        localStorage.setItem('auth_token', responseData.data.token)
        localStorage.setItem('auth_usuario', JSON.stringify(responseData.data.usuario))

        console.log('[AUTH] ✅ Registro exitoso')
      } catch (error) {
        const mensaje = error instanceof Error ? error.message : 'Error desconocido'
        console.error('[AUTH] Error en registro:', mensaje)
        logout() // Cambiado de limpiar() a logout()
        throw error
      } finally {
        setIsLoading(false)
      }
    },
    []
  )

  // ======================== GOOGLE LOGIN ========================
  const googleLogin = useCallback(async () => {
    try {
      setIsLoading(true)
      window.location.href = getApiUrl('/api/auth/google')
    } catch (error) {
      const mensaje = error instanceof Error ? error.message : 'Error desconocido'
      console.error('[AUTH] Error en Google login:', mensaje)
      setIsLoading(false)
      throw error
    }
  }, [])

  // ======================== LOGOUT (Antes se llamaba limpiar) ========================
  const logout = useCallback(() => {
    setToken(null)
    setUsuario(null)
    setIsAutenticado(false)
    localStorage.removeItem('auth_token')
    localStorage.removeItem('auth_usuario')
    console.log('[AUTH] 🚪 Sesión cerrada')
  }, [])

  return {
    usuario,
    token,
    isLoading,
    isAutenticado,
    login,
    register,
    googleLogin,
    logout, // Ahora sí existe esta variable para retornar
  }
}

/**
 * Hook para obtener token
 */
export function useAuthToken(): string | null {
  const [token, setToken] = useState<string | null>(null)

  useEffect(() => {
    setToken(localStorage.getItem('auth_token'))
  }, [])

  return token
}

/**
 * Helper para hacer fetch con autenticación
 */
export async function fetchWithAuth(
  url: string,
  options: RequestInit = {},
  token?: string | null
) {
  const authToken = token || localStorage.getItem('auth_token')

  return fetch(url, {
    ...options,
    headers: {
      ...options.headers,
      ...(authToken && { Authorization: `Bearer ${authToken}` }),
    },
  })
}