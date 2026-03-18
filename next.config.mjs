/** @type {import('next').NextConfig} */
const nextConfig = {
  typescript: {
    ignoreBuildErrors: true,
  },
  images: {
    unoptimized: true,
  },
  // Configuración para Capacitor
  assetPrefix: process.env.NODE_ENV === 'production' ? './' : undefined,
  basePath: '',
  trailingSlash: true,
}

export default nextConfig
