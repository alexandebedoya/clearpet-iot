import { NextRequest, NextResponse } from 'next/server'

// Google OAuth configuration
const GOOGLE_CLIENT_ID = process.env.GOOGLE_CLIENT_ID
const GOOGLE_CLIENT_SECRET = process.env.GOOGLE_CLIENT_SECRET
const BASE_URL = process.env.NEXTAUTH_URL || 'http://localhost:3000'

export async function GET(request: NextRequest) {
  try {
    // Build Google OAuth URL
    const googleAuthUrl = new URL('https://accounts.google.com/o/oauth2/v2/auth')

    googleAuthUrl.searchParams.set('client_id', GOOGLE_CLIENT_ID!)
    googleAuthUrl.searchParams.set('redirect_uri', `${BASE_URL}/api/auth/google/callback`)
    googleAuthUrl.searchParams.set('response_type', 'code')
    googleAuthUrl.searchParams.set('scope', 'openid email profile')
    googleAuthUrl.searchParams.set('access_type', 'offline')
    googleAuthUrl.searchParams.set('prompt', 'consent')

    // Redirect to Google
    return NextResponse.redirect(googleAuthUrl.toString())
  } catch (error) {
    console.error('[GOOGLE_AUTH] Error initiating Google OAuth:', error)
    return NextResponse.json(
      { error: 'Error iniciando autenticación con Google' },
      { status: 500 }
    )
  }
}