/**
 * POST /api/auth/register
 * Registrar nuevo usuario (versión corregida y segura)
 */

import { NextRequest, NextResponse } from 'next/server'
import prisma from '@/lib/prisma'
import { hashPassword, generateToken, validatePasswordStrength } from '@/lib/auth'
import { ApiResponse, RegisterRequest } from '@/lib/types'

export async function POST(req: NextRequest) {
  try {
    const body: RegisterRequest = await req.json()

    // ======================== NORMALIZACIÓN ========================
    const email = body.email?.trim().toLowerCase()
    const nombre = body.nombre?.trim()
    const password = body.password
    const confirmPassword = body.confirmPassword

    // ======================== VALIDACIONES ========================
    if (!email || !nombre || !password) {
      return NextResponse.json(
        {
          success: false,
          error: 'Faltan campos requeridos',
        } as ApiResponse,
        { status: 400 }
      )
    }

    // Email regex robusto
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(email)) {
      return NextResponse.json(
        {
          success: false,
          error: 'Email inválido',
        } as ApiResponse,
        { status: 400 }
      )
    }

    // Password strength
    const passwordCheck = validatePasswordStrength(password)
    if (!passwordCheck.valid) {
      return NextResponse.json(
        {
          success: false,
          error: 'Contraseña débil',
          errors: { password: passwordCheck.errors },
        } as ApiResponse,
        { status: 400 }
      )
    }

    // Confirm password
    if (password !== confirmPassword) {
      return NextResponse.json(
        {
          success: false,
          error: 'Las contraseñas no coinciden',
        } as ApiResponse,
        { status: 400 }
      )
    }

    // ======================== VERIFICAR DUPLICADO ========================
    const usuarioExistente = await prisma.usuario.findUnique({
      where: { email },
    })

    if (usuarioExistente) {
      return NextResponse.json(
        {
          success: false,
          error: 'El email ya está registrado',
        } as ApiResponse,
        { status: 409 }
      )
    }

    // ======================== HASH PASSWORD ========================
    const passwordHash = await hashPassword(password)

    // ======================== CREAR USUARIO ========================
    const usuarioNuevo = await prisma.usuario.create({
      data: {
        email,
        nombre,
        password: passwordHash, // ✅ SOLO UNO
        rol: 'usuario',
        activo: true,
      },
    })

    // ======================== GENERAR TOKEN ========================
    const token = generateToken({
      id: usuarioNuevo.id,
      email: usuarioNuevo.email,
      rol: usuarioNuevo.rol,
    })

    // ======================== OBTENER IP REAL ========================
    const forwarded = req.headers.get('x-forwarded-for')
    const ip = forwarded ? forwarded.split(',')[0] : 'unknown'

    // ======================== REGISTRAR SESIÓN ========================
    await prisma.sesionUsuario.create({
      data: {
        usuarioId: usuarioNuevo.id,
        token,
        expiraEn: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
        dispositivo: req.headers.get('user-agent') || 'unknown',
        direccionIP: ip,
      },
    })

    // ======================== PREFERENCIAS ========================
    await prisma.preferenciaNotificacion.create({
      data: {
        usuarioId: usuarioNuevo.id,
      },
    })

    // ======================== RESPUESTA ========================
    return NextResponse.json(
      {
        success: true,
        message: 'Usuario registrado exitosamente',
        data: {
          token,
          usuario: {
            id: usuarioNuevo.id,
            email: usuarioNuevo.email,
            nombre: usuarioNuevo.nombre,
            rol: usuarioNuevo.rol,
            activo: usuarioNuevo.activo,
            verificado: usuarioNuevo.verificado ?? false,
          },
        },
      } as ApiResponse,
      { status: 201 }
    )

  } catch (error: any) {
    console.error('[AUTH REGISTER ERROR]', error)

    // Manejo específico Prisma (unique constraint race condition)
    if (error.code === 'P2002') {
      return NextResponse.json(
        {
          success: false,
          error: 'El email ya está registrado',
        } as ApiResponse,
        { status: 409 }
      )
    }

    return NextResponse.json(
      {
        success: false,
        error: 'Error interno del servidor',
      } as ApiResponse,
      { status: 500 }
    )
  }
}