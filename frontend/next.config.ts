import type { NextConfig } from "next";

const backendUrl = process.env.API_URL || process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";

const nextConfig: NextConfig = {
  typescript: {
    ignoreBuildErrors: true,
  },
  async rewrites() {
    return {
      fallback: [
        {
          source: '/api/auth/login',
          destination: `${backendUrl}/auth/login`,
        },
        {
          source: '/api/auth/signup',
          destination: `${backendUrl}/auth/signup`,
        },
        {
          source: '/api/auth/refresh',
          destination: `${backendUrl}/auth/refresh`,
        },
        {
          source: '/api/:path*',
          destination: `${backendUrl}/:path*`,
        },
      ]
    }
  },
};

export default nextConfig;
