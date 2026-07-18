import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    return {
      fallback: [
        {
          source: '/api/auth/login',
          destination: 'http://localhost:8080/api/auth/login',
        },
        {
          source: '/api/auth/signup',
          destination: 'http://localhost:8080/api/auth/signup',
        },
        {
          source: '/api/auth/refresh',
          destination: 'http://localhost:8080/api/auth/refresh',
        },
        {
          source: '/api/:path*',
          destination: 'http://localhost:8080/api/:path*',
        },
      ]
    }
  },
};

export default nextConfig;
