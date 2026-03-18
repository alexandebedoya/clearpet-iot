import { NextRequest, NextResponse } from 'next/server'
import { generateToken, hashPassword } from '@/lib/auth'
import { PrismaClient } from '@prisma/client'

const prisma = new PrismaClient()

// Google OAuth configuration
const GOOGLE_CLIENT_ID = process.env.GOOGLE_CLIENT_ID
const GOOGLE_CLIENT_SECRET = process.env.GOOGLE_CLIENT_SECRET
const BASE_URL = process.env.NEXTAUTH_URL || 'http://localhost:3000'

export async function GET(request: NextRequest) {
  try {
    const searchParams = request.nextUrl.searchParams
    const code = searchParams.get('code')
    const error = searchParams.get('error')

    if (error) {
      console.error('[GOOGLE_CALLBACK] OAuth error:', error)
      return NextResponse.redirect(`${BASE_URL}/login?error=google_auth_failed`)
    }

    if (!code) {
      return NextResponse.redirect(`${BASE_URL}/login?error=no_code`)
    }

    // Exchange code for access token
    const tokenResponse = await fetch('https://oauth2.googleapis.com/token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: new URLSearchParams({
        client_id: GOOGLE_CLIENT_ID!,
        client_secret: GOOGLE_CLIENT_SECRET!,
        code,
        grant_type: 'authorization_code',
        redirect_uri: `${BASE_URL}/api/auth/google/callback`,
      }),
    })

    if (!tokenResponse.ok) {
      console.error('[GOOGLE_CALLBACK] Token exchange failed')
      return NextResponse.redirect(`${BASE_URL}/login?error=token_exchange_failed`)
    }

    const tokenData = await tokenResponse.json()

    // Get user info from Google
    const userResponse = await fetch('https://www.googleapis.com/oauth2/v2/userinfo', {
      headers: {
        Authorization: `Bearer ${tokenData.access_token}`,
      },
    })

    if (!userResponse.ok) {
      console.error('[GOOGLE_CALLBACK] Failed to get user info')
      return NextResponse.redirect(`${BASE_URL}/login?error=user_info_failed`)
    }

    const googleUser = await userResponse.json()

    // Check if user exists, if not create them
    let user = await prisma.usuario.findUnique({
      where: { email: googleUser.email }
    })

    if (!user) {
      // Create new user with Google data
      user = await prisma.usuario.create({
        data: {
          email: googleUser.email,
          nombre: googleUser.name,
          password: await hashPassword(Math.random().toString(36)), // Random password for Google users
          rol: 'USER',
          activo: true,
          verificado: true, // Google accounts are pre-verified
          googleId: googleUser.id,
        }
      })
    } else {
      // Update Google ID if not set
      if (!user.googleId) {
        user = await prisma.usuario.update({
          where: { id: user.id },
          data: { googleId: googleUser.id }
        })
      }
    }

    // Generate JWT token
    const token = generateToken({
      id: user.id,
      email: user.email,
      nombre: user.nombre,
      rol: user.rol,
    })

    // Create session
    await prisma.sesionUsuario.create({
      data: {
        usuarioId: user.id,
        token,
        expiracion: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000), // 7 days
        activo: true,
      }
    })

    // Redirect to dashboard with token
    const redirectUrl = new URL('/dashboard', BASE_URL)
    redirectUrl.searchParams.set('token', token)

    return NextResponse.redirect(redirectUrl.toString())

  } catch (error) {
    console.error('[GOOGLE_CALLBACK] Error:', error)
    return NextResponse.redirect(`${BASE_URL}/login?error=auth_failed`)
  }
}