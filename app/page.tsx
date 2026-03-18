"use client"

import { AppHeader } from "@/components/iot/premium/app-header"
import { AppSidebar } from "@/components/iot/premium/app-sidebar"
import { BottomNavigation } from "@/components/iot/premium/bottom-navigation"
import { LoginScreen } from "@/components/iot/premium/login-screen"
import { AlertsView } from "@/components/iot/premium/views/alerts-view"
import { AnalysisView } from "@/components/iot/premium/views/analysis-view"
import { DashboardView } from "@/components/iot/premium/views/dashboard-view"
import { MonitoringView } from "@/components/iot/premium/views/monitoring-view"
import { ProfileView } from "@/components/iot/premium/views/profile-view"
import { RecommendationsView } from "@/components/iot/premium/views/recommendations-view"
import { SensorsView } from "@/components/iot/premium/views/sensors-view"
import { useSensorData } from "@/hooks/use-sensor-data"
import { SensorData } from "@/lib/types"
import { useState } from "react"

type ViewType = "dashboard" | "monitoreo" | "analisis" | "alertas" | "sensores" | "recomendaciones" | "perfil"

export default function AirQualityApp() {
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [currentView, setCurrentView] = useState<ViewType>("dashboard")
  const [sidebarOpen, setSidebarOpen] = useState(false)

  // 1. Obtenemos los datos del hook. Usamos alias para evitar conflictos.
  const { data: rawData, dataSource, isLoading } = useSensorData()

  // 2. Objeto de respaldo (Fallback) para que TypeScript no vea "null" o "undefined"
  // Esto quita los errores rojos en las líneas de "data={data}"
  const data: SensorData = rawData || {
    mq4: 0,
    mq7: 0,
    mq135: 0,
    nivel: 'NORMAL',
    timestamp: new Date().toISOString()
  };

  const handleLogin = () => {
    setIsAuthenticated(true)
  }

  // 3. Función de cierre de sesión corregida para el compilador
  const handleLogout = () => {
    setIsAuthenticated(false)
    setSidebarOpen(false)
    setCurrentView("dashboard")
  }

  if (!isAuthenticated) {
    return <LoginScreen onLogin={handleLogin} />
  }

  // 4. Renderizado de vistas con datos seguros
  const renderView = () => {
    switch (currentView) {
      case "dashboard":
        return (
          <DashboardView 
            data={data} 
            onNavigateToAlerts={() => setCurrentView("alertas")}
            onNavigateToRecommendations={() => setCurrentView("recomendaciones")}
          />
        )
      case "monitoreo":
        return <MonitoringView data={data} />
      case "alertas":
        return <AlertsView />
      case "analisis":
        return <AnalysisView />
      case "sensores":
        return <SensorsView />
      case "recomendaciones":
        return <RecommendationsView data={data} />
      case "perfil":
        return <ProfileView />
      default:
        return (
          <DashboardView 
            data={data} 
            onNavigateToAlerts={() => setCurrentView("alertas")} 
            onNavigateToRecommendations={() => setCurrentView("recomendaciones")} 
          />
        )
    }
  }

  return (
    <div className="min-h-screen bg-background">
      {/* Header que muestra si estamos en MQTT o API simulada */}
      <AppHeader 
        title={isLoading ? "Cargando..." : `ClearPet (${dataSource})`} 
        onMenuClick={() => setSidebarOpen(true)}
        notificationCount={3}
      />

      {/* Sidebar con la función onLogout correctamente vinculada */}
      <AppSidebar 
        isOpen={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
        currentView={currentView}
        onNavigate={setCurrentView}
        onLogout={handleLogout}
      />

      <main className="min-h-[calc(100vh-4rem)] max-w-lg mx-auto">
        {renderView()}
      </main>

      <BottomNavigation 
        currentView={currentView}
        onNavigate={setCurrentView}
        alertCount={3}
      />
    </div>
  )
}