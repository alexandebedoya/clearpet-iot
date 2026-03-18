"use client";

import React from "react";
import { useSensorData } from "@/hooks/use-sensor-data";
import { Card } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Loader2, AlertCircle } from "lucide-react";
import { SensorDetail } from "../components/sensor-detail"; 

export function SensorsView() {
  const { data, isLoading } = useSensorData();

  // Función para generar datos simulados para la gráfica (mientras llegan reales)
  const generateHistoricalData = (baseValue: number) => {
    return Array.from({ length: 10 }, (_, i) => ({
      time: `${i}:00`,
      value: (baseValue || 0) + Math.random() * 20 - 10,
    }));
  };

  // --- ESTADO: CARGANDO ---
  if (isLoading) {
    return (
      <div className="p-4 pb-24">
        <Card className="p-8 border-dashed">
          <div className="flex flex-col items-center gap-3 text-muted-foreground">
            <Loader2 className="w-8 h-8 animate-spin" />
            <p>Cargando sensores de ClearPet...</p>
          </div>
        </Card>
      </div>
    );
  }

  // --- ESTADO: ERROR ---
  if (!data) {
    return (
      <div className="p-4 pb-24">
        <Card className="p-4 border-red-200 bg-red-50">
          <div className="flex gap-3">
            <AlertCircle className="w-5 h-5 text-red-500 flex-shrink-0" />
            <p className="text-sm text-red-800">Error al conectar con la base de datos de sensores.</p>
          </div>
        </Card>
      </div>
    );
  }

  // --- ESTADO: ÉXITO (RENDER PRINCIPAL) ---
  return (
    <div className="p-4 pb-24">
      <div className="mb-6">
        <h1 className="text-2xl font-bold tracking-tight">Detalle de Sensores</h1>
        <p className="text-muted-foreground text-sm mt-1">
          Información completa de cada sensor en tiempo real.
        </p>
      </div>

      <Tabs defaultValue="mq4" className="w-full">
        <TabsList className="grid w-full grid-cols-3 bg-muted/50 border">
          <TabsTrigger value="mq4">MQ-4</TabsTrigger>
          <TabsTrigger value="mq7">MQ-7</TabsTrigger>
          <TabsTrigger value="mq135">MQ-135</TabsTrigger>
        </TabsList>

        {/* --- Pestaña MQ-4 (Metano) --- */}
        <TabsContent value="mq4" className="mt-4">
          <SensorDetail
            sensorName="MQ-4"
            sensorLabel="Sensor de Gas Metano"
            currentValue={data.mq4}
            baseLine={260}
            delta={data.mq4 - 260}
            safeLimit={300}
            warningLimit={200}
            unit="ppm"
            trend={1.2}
            historicalData={generateHistoricalData(data.mq4)}
          />
        </TabsContent>

        {/* --- Pestaña MQ-7 (Monóxido de Carbono) --- */}
        <TabsContent value="mq7" className="mt-4">
          <SensorDetail
            sensorName="MQ-7"
            sensorLabel="Sensor de Monóxido de Carbono"
            currentValue={data.mq7}
            baseLine={220}
            delta={data.mq7 - 220}
            safeLimit={200}
            warningLimit={150}
            unit="ppm"
            trend={-0.5}
            historicalData={generateHistoricalData(data.mq7)}
          />
        </TabsContent>

        {/* --- Pestaña MQ-135 (Calidad General) --- */}
        <TabsContent value="mq135" className="mt-4">
          <SensorDetail
            sensorName="MQ-135"
            sensorLabel="Calidad del Aire General"
            currentValue={data.mq135 || 0}
            baseLine={280}
            delta={(data.mq135 || 0) - 280}
            safeLimit={350}
            warningLimit={300}
            unit="ppm"
            trend={2.3}
            historicalData={generateHistoricalData(data.mq135 || 0)}
          />
        </TabsContent>
      </Tabs>
    </div>
  );
}