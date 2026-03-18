import { NextRequest, NextResponse } from 'next/server'
import { getApiUrl } from '@/lib/api-config'

export const dynamic = 'force-static'; // <--- AGREGA SOLO ESTO

export async function GET(req: NextRequest) {
  try {
    const authHeader = req.headers.get('authorization')

    const response = await fetch(getApiUrl('/api/sensores/latest'), {
      method: 'GET',
      headers: {
        'Authorization': authHeader || '',
      },
    })

    const data = await response.json()

    return NextResponse.json(data, { status: response.status })
  } catch (error) {
    console.error('[SENSORES] Error obteniendo datos:', error)
    return NextResponse.json(
      { error: 'Error obteniendo datos de sensores' },
      { status: 500 }
    )
  }
}
