/**
 * GET /api/sensores/ultima
 * Obtener última lectura de sensores del usuario
 */
export const dynamic = 'force-static';

import { NextRequest, NextResponse } from 'next/server'
import prisma from '@/lib/prisma'
import { extractTokenFromHeader, verifyToken } from '@/lib/auth'
import { ApiResponse } from '@/lib/types'

export async function GET(req: NextRequest) {
  try {
    const authHeader = req.headers.get('authorization')
    const token = extractTokenFromHeader(authHeader)

    // Si no está autenticado, retornar datos simulados (fallback)
    if (!token) {
      return NextResponse.json(
        {
          success: true,
          data: {
            mq4: 245,
            mq7: 310,
            mq135: 320,
            nivel: 'NORMAL',
            timestamp: new Date().toISOString(),
          },
        } as ApiResponse,
        { status: 200 }
      )
    }

    const payload = verifyToken(token)
    if (!payload) {
      return NextResponse.json(
        { success: false, error: 'Token inválido' } as ApiResponse,
        { status: 401 }
      )
    }

    // Obtener última lectura
    const ultimaLectura = await prisma.lecturaSensor.findFirst({
      where: { usuarioId: payload.id },
      orderBy: { creadoEn: 'desc' },
    })

    if (!ultimaLectura) {
      // Retornar datos por defecto si no hay lecturas
      return NextResponse.json(
        {
          success: true,
          data: {
            mq4: 245,
            mq7: 310,
            mq135: 320,
            nivel: 'NORMAL',
            timestamp: new Date().toISOString(),
          },
        } as ApiResponse,
        { status: 200 }
      )
    }

    return NextResponse.json(
      {
        success: true,
        data: {
          mq4: ultimaLectura.mq4,
          mq7: ultimaLectura.mq7,
          mq135: ultimaLectura.mq135,
          nivel: ultimaLectura.nivel,
          timestamp: ultimaLectura.lecturaMQTT.toISOString(),
        },
      } as ApiResponse,
      { status: 200 }
    )
  } catch (error) {
    console.error('[API] Error obteniendo última lectura:', error)
    return NextResponse.json(
      { success: false, error: 'Error obteniendo datos' } as ApiResponse,
      { status: 500 }
    )
  }
}
