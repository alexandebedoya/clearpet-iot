# 🌍 Monitoreo Ambiental IoT - ClearPet

Aplicación web IoT para monitoreo de calidad de aire y alertas ambientales.

## 🚀 Despliegue en Vercel (Servidor Gratis)

### Opción 1: Despliegue Recomendado (Más fácil)

1. **Crea una cuenta en Vercel** (gratis):
   - Ve a https://vercel.com
   - Haz clic en "Sign Up"
   - Elige "Continue with GitHub" y crea una cuenta de GitHub

2. **Carga el código en GitHub**:
   - Ve a https://github.com/new
   - Crea un nuevo repositorio llamado `clearpet-iot`
   - Sigue las instrucciones para subir tu código (o en VS Code: Source Control > Publish to GitHub)

3. **Despliega en Vercel**:
   - Ve a https://vercel.com/new
   - Importa el repositorio de GitHub
   - Haz clic en "Deploy"
   - **¡Listo!** Tu app estará en vivo en `https://tu-proyecto.vercel.app`

4. **Configura variables de entorno en Vercel**:
   - En Vercel dashboard, ve a Settings > Environment Variables
   - Agrega: `NEXT_PUBLIC_API_URL` = `https://tu-proyecto.vercel.app`
   - Redeploy

### Opción 2: Despliegue Manual con Vercel CLI

```bash
# Instala Vercel CLI
npm install -g vercel

# Desde la carpeta del proyecto
cd c:\Users\alexi\Desktop\ClearPet1

# Despliega
vercel

# Sigue los pasos en la terminal
```

### Opción 3: Railway (Alternativa)

1. Ve a https://railway.app
2. Conecta tu GitHub
3. Haz clic en "New Project" > "Deploy from GitHub"
4. Selecciona tu repositorio
5. Railway detectará automáticamente que es Next.js
6. Click "Deploy"

## 📱 Acceder desde Móvil

Una vez desplegado:

1. **URL remota**: `https://tu-proyecto.vercel.app`
2. **Desde navegador móvil**: Simplemente abre la URL en el navegador de tu celular (cualquier teléfono, cualquier red)
3. **O generar APK**: 
   - La app ya está configurada con Capacitor
   - Ejecuta: `npx cap sync android`
   - Abre en Android Studio y genera APK

## 🛠 Desarrollo Local

```bash
# Instalar dependencias
npm install

# Ejecutar en desarrollo (http://localhost:3000)
npm run dev

# Compilar para producción
npm run build

# Ejecutar versión de producción
npm start
```

## 📡 Variables de Entorno

Copia `.env.example` a `.env.local` para desarrollo:

```
NEXT_PUBLIC_API_URL=http://localhost:3000
```

Para producción en Vercel, configura la variable en el dashboard de Vercel.

## 📊 API Endpoints

- `GET /api/sensores/latest` - Último dato de sensores

## 🔐 Seguridad

⚠️ **Antes de usar en producción**:
1. Agregar autenticación (JWT, Google Auth, etc)
2. Validar datos de entrada
3. Usar HTTPS (Vercel lo hace automáticamente)
4. Limitar acceso a API
5. Agregar rate limiting

## 📞 Soporte

Problemas comunes:

**"Cannot fetch data from API"**
- Verifica que `NEXT_PUBLIC_API_URL` esté configurada correctamente
- Revisa CORS en la API si está en dominio diferente

**"Vercel deployment failed"**
- Revisa los logs en Vercel dashboard
- Asegúrate que `package.json` tiene todos los scripts necesarios

## 📦 Estructura

```
├── app/                 # Next.js app directory
│   ├── api/            # API routes
│   ├── layout.tsx      # Layout principal
│   └── page.tsx        # Página principal
├── components/         # Componentes React
│   ├── iot/           # Componentes IoT
│   └── ui/            # Componentes UI base
├── lib/               # Utilidades
├── hooks/             # React hooks
└── public/            # Archivos estáticos
```

---

**Creado por:** ClearPet Team  
**Última actualización:** 2026
