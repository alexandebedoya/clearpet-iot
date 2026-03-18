/**
 * POST /api/sensores/guardar
 * Guardar una lectura de sensores en la base de datos
 * Llamado por el hook MQTT cuando recibe datos del ESP32
 */

import { NextRequest, NextResponse } from 'next/server'
import prisma from '@/lib/prisma'
import { protectedRouteHandler } from '@/lib/auth-middleware'
import { ApiResponse } from '@/lib/types'
import { extractTokenFromHeader, verifyToken } from '@/lib/auth'

interface GuardarLecturaRequest {
  mq4: number
  mq7: number
  mq135: number
  nivel: string
  timestamp: number
  configuracionId?: string
}

export async function POST(req: NextRequest) {
  try {
    // Verificar autenticación
    const authHeader = req.headers.get('authorization')
    const token = extractTokenFromHeader(authHeader)

    if (!token) {
      return NextResponse.json(
        { success: false, error: 'No autenticado' } as ApiResponse,
        { status: 401 }
      )
    }

    const payload = verifyToken(token)
    if (!payload) {
      return NextResponse.json(
        { success: false, error: 'Token inválido' } as ApiResponse,
        { status: 401 }
      )
    }

    const body: GuardarLecturaRequest = await req.json()

    // Validaciones
    if (body.mq4 === undefined || body.mq7 === undefined || body.mq135 === undefined) {
      return NextResponse.json(
        { success: false, error: 'Faltan datos de sensores' } as ApiResponse,
        { status: 400 }
      )
    }

    // Obtener configuración (o crear por defecto)
    let config = await prisma.sensorConfig.findFirst({
      where: { usuarioId: payload.id },
    })

    if (!config) {
      config = await prisma.sensorConfig.create({
        data: {
          usuarioId: payload.id,
          nombre: 'Configuración por Defecto',
          codigoMQ4: 'MQ4_DEFAULT',
          codigoMQ7: 'MQ7_DEFAULT',
          codigoMQ135: 'MQ135_DEFAULT',
        },
      })
    }

    // Calcular deltas
    const deltaMQ4 = body.mq4 - config.baseMQ4
    const deltaMQ7 = body.mq7 - config.baseMQ7
    const deltaMQ135 = body.mq135 - config.baseMQ135
    const valorPromedio = Math.max(deltaMQ4, deltaMQ7, deltaMQ135)

    // Guardar lectura
    const lectura = await prisma.lecturaSensor.create({
      data: {
        usuarioId: payload.id,
        configuracionId: config.id,
        mq4: body.mq4,
        mq7: body.mq7,
        mq135: body.mq135,
        deltaMQ4,
        deltaMQ7,
        deltaMQ135,
        valorPromedio,
        nivel: body.nivel || 'NORMAL',
        lecturaMQTT: new Date(body.timestamp),
      },
    })

    // Verificar si hay alerta que crear
    if (body.nivel === 'MODERADO' || body.nivel === 'PELIGRO') {
      const alerta = await prisma.alerta.create({
        data: {
          usuarioId: payload.id,
          configuracionId: config.id,
          nivel: body.nivel,
          titulo: `Alerta de ${body.nivel}`,
          mensaje: `MQ4: ${body.mq4}, MQ7: ${body.mq7}, MQ135: ${body.mq135}`,
        },
      })

      console.log('[API] Alerta creada:', alerta.id)
    }

    return NextResponse.json(
      {
        success: true,
        message: 'Lectura guardada exitosamente',
        data: lectura,
      } as ApiResponse,
      { status: 201 }
    )
  } catch (error) {
    console.error('[API] Error guardando lectura:', error)
    return NextResponse.json(
      { success: false, error: 'Error guardando lectura' } as ApiResponse,
      { status: 500 }
    )
  }
}

/**
 * GET /api/sensores/guardar
 * Obtener últimas lecturas del usuario
 */
export async function GET(req: NextRequest) {
  try {
    const authHeader = req.headers.get('authorization')
    const token = extractTokenFromHeader(authHeader)

    if (!token) {
      return NextResponse.json(
        { success: false, error: 'No autenticado' } as ApiResponse,
        { status: 401 }
      )
    }

    const payload = verifyToken(token)
    if (!payload) {
      return NextResponse.json(
        { success: false, error: 'Token inválido' } as ApiResponse,
        { status: 401 }
      )
    }

    const horas = parseInt(req.nextUrl.searchParams.get('horas') || '24')

    const desde = new Date(Date.now() - horas * 60 * 60 * 1000)

    const lecturas = await prisma.lecturaSensor.findMany({
      where: {
        usuarioId: payload.id,
        creadoEn: { gte: desde },
      },
      orderBy: { creadoEn: 'desc' },
      take: 1000,
    })

    return NextResponse.json(
      {
        success: true,
        data: lecturas,
      } as ApiResponse,
      { status: 200 }
    )
  } catch (error) {
    console.error('[API] Error obteniendo lecturas:', error)
    return NextResponse.json(
      { success: false, error: 'Error obteniendo lecturas' } as ApiResponse,
      { status: 500 }
    )
  }
}
