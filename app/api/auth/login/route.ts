/**
 * POST /api/auth/login
 * Autenticarse con email y contraseña
 */

import { NextRequest, NextResponse } from 'next/server'
import { getApiUrl } from '@/lib/api-config'

export const dynamic = 'force-static'; // <--- AGREGA SOLO ESTO

export async function POST(req: NextRequest) {
  try {
    const body = await req.json()

    const response = await fetch(getApiUrl('/api/auth/login'), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    })

    const data = await response.json()

    return NextResponse.json(data, { status: response.status })
  } catch (error) {
    console.error('[AUTH] Error en login:', error)
    return NextResponse.json(
      {
        success: false,
        error: 'Error al iniciar sesión',
      },
      { status: 500 }
    )
  }
}

/**
 * GET /api/auth/login
 * Verificar si está autenticado
 */
export async function GET(req: NextRequest) {
  try {
    const authHeader = req.headers.get('authorization')

    const response = await fetch(getApiUrl('/api/auth/me'), {
      method: 'GET',
      headers: {
        'Authorization': authHeader || '',
      },
    })

    const data = await response.json()

    return NextResponse.json(data, { status: response.status })
  } catch (error) {
    console.error('[AUTH] Error verificando token:', error)
    return NextResponse.json(
      {
        success: false,
        error: 'Error verificando token',
      },
      { status: 500 }
    )
  }
}