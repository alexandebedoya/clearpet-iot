/**
 * POST /api/auth/register
 * Registrar nuevo usuario
 */

import { NextRequest, NextResponse } from 'next/server'
import { getApiUrl } from '@/lib/api-config'

export const dynamic = 'force-static'; // <--- AGREGA SOLO ESTO

export async function POST(req: NextRequest) {
  try {
    const body = await req.json()

    const response = await fetch(getApiUrl('/api/auth/register'), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    })

    const data = await response.json()

    return NextResponse.json(data, { status: response.status })
  } catch (error) {
    console.error('[AUTH] Error en register:', error)
    return NextResponse.json(
      {
        success: false,
        error: 'Error al registrar usuario',
      },
      { status: 500 }
    )
  }
}