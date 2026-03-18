/**
 * POST /api/auth/login
 * Autenticarse con email y contraseña
 */

import { NextRequest, NextResponse } from 'next/server'
import prisma from '@/lib/prisma'
import { verifyPassword, generateToken, extractTokenFromHeader } from '@/lib/auth'
import { ApiResponse, LoginRequest } from '@/lib/types'

// ======================== HELPER: OBTENER IP ========================
function getClientIP(req: NextRequest): string {
  const forwarded = req.headers.get('x-forwarded-for')
  if (forwarded) {
    return forwarded.split(',')[0].trim()
  }
  return req.headers.get('x-real-ip') || 'unknown'
}

export async function POST(req: NextRequest) {
  try {
    const body: LoginRequest = await req.json()

    // ======================== VALIDACIONES ========================
    if (!body.email || !body.password) {
      return NextResponse.json(
        {
          success: false,
          error: 'Email y contraseña son requeridos',
        } as ApiResponse,
        { status: 400 }
      )
    }

    // ======================== BUSCAR USUARIO ========================
    const usuario = await prisma.usuario.findUnique({
      where: { email: body.email.toLowerCase() },
    })

    if (!usuario) {
      return NextResponse.json(
        {
          success: false,
          error: 'Email o contraseña incorrectos',
        } as ApiResponse,
        { status: 401 }
      )
    }

    // ======================== VERIFICAR CONTRASEÑA ========================
    const contrasenaValida = await verifyPassword(body.password, usuario.password)

    if (!contrasenaValida) {
      return NextResponse.json(
        {
          success: false,
          error: 'Email o contraseña incorrectos',
        } as ApiResponse,
        { status: 401 }
      )
    }

    // ======================== VERIFICAR USUARIO ACTIVO ========================
    if (!usuario.activo) {
      return NextResponse.json(
        {
          success: false,
          error: 'Usuario desactivado',
        } as ApiResponse,
        { status: 403 }
      )
    }

    // ======================== GENERAR TOKEN ========================
    const token = generateToken({
      id: usuario.id,
      email: usuario.email,
      rol: usuario.rol,
    })

    // ======================== IP DEL CLIENTE ========================
    const ip = getClientIP(req)

    // ======================== REGISTRAR SESIÓN ========================
    const expiraEn = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)

    await prisma.sesionUsuario.create({
      data: {
        usuarioId: usuario.id,
        token,
        expiraEn,
        dispositivo: req.headers.get('user-agent') || undefined,
        direccionIP: ip,
      },
    })

    // ======================== ACTUALIZAR ÚLTIMO LOGIN ========================
    await prisma.usuario.update({
      where: { id: usuario.id },
      data: { ultimoLogin: new Date() },
    })

    // ======================== REGISTRAR AUDITORÍA ========================
    await prisma.auditLog.create({
      data: {
        usuarioId: usuario.id,
        accion: 'LOGIN',
        entidad: 'Usuario',
        entidadId: usuario.id,
        direccionIP: ip,
      },
    })

    return NextResponse.json(
      {
        success: true,
        message: 'Inicio de sesión exitoso',
        data: {
          token,
          usuario: {
            id: usuario.id,
            email: usuario.email,
            nombre: usuario.nombre,
            rol: usuario.rol,
            activo: usuario.activo,
            verificado: usuario.verificado,
          },
        },
      } as ApiResponse,
      { status: 200 }
    )
  } catch (error) {
    console.error('[AUTH] Error en login:', error)
    return NextResponse.json(
      {
        success: false,
        error: 'Error al iniciar sesión',
      } as ApiResponse,
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
    const token = extractTokenFromHeader(authHeader)

    if (!token) {
      return NextResponse.json(
        {
          success: false,
          error: 'Token no proporcionado',
        } as ApiResponse,
        { status: 401 }
      )
    }

    const sesion = await prisma.sesionUsuario.findUnique({
      where: { token },
      include: {
        usuario: true,
      },
    })

    if (!sesion || !sesion.activa || sesion.expiraEn < new Date()) {
      return NextResponse.json(
        {
          success: false,
          error: 'Sesión inválida o expirada',
        } as ApiResponse,
        { status: 401 }
      )
    }

    return NextResponse.json(
      {
        success: true,
        data: {
          usuario: {
            id: sesion.usuario.id,
            email: sesion.usuario.email,
            nombre: sesion.usuario.nombre,
            rol: sesion.usuario.rol,
            activo: sesion.usuario.activo,
            verificado: sesion.usuario.verificado,
          },
        },
      } as ApiResponse,
      { status: 200 }
    )
  } catch (error) {
    console.error('[AUTH] Error verificando token:', error)
    return NextResponse.json(
      {
        success: false,
        error: 'Error verificando token',
      } as ApiResponse,
      { status: 500 }
    )
  }
}