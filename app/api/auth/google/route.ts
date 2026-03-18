import { NextRequest, NextResponse } from 'next/server'
import { getApiUrl } from '@/lib/api-config'

export const dynamic = 'force-static'; // <--- AGREGA SOLO ESTO

export async function GET(request: NextRequest) {
  try {
    // Redirect to Spring Boot OAuth2 authorization
    const oauthUrl = getApiUrl('/oauth2/authorization/google')
    return NextResponse.redirect(oauthUrl)
  } catch (error) {
    console.error('[GOOGLE_AUTH] Error initiating Google OAuth:', error)
    return NextResponse.json(
      { error: 'Error iniciando autenticación con Google' },
      { status: 500 }
    )
  }
}