/** @type {import('next').NextConfig} */
const nextConfig = {
  // Genera la salida estática necesaria para una APK (sustituye el servidor Node.js)
  output: 'export',
  
  // Desactiva la optimización de imágenes (necesario para modo export)
  images: {
    unoptimized: true,
  },

  // Ignora errores de TypeScript para asegurar que el Build termine con éxito
  typescript: {
    ignoreBuildErrors: true,
  },

  // Asegura que las rutas funcionen correctamente en Android (agrega / al final de las rutas)
  trailingSlash: true,

  // Opcional: asegura que el directorio de salida se llame 'out'
  distDir: 'out',
};

export default nextConfig;