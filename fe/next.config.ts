import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  reactStrictMode: true,
  async rewrites() {
    const backendInternalUrl =
      process.env.NEXT_PUBLIC_BACKEND_INTERNAL_URL || 'http://localhost:8080';

    return [
      {
        source: '/api/:path*',
        destination: `${backendInternalUrl}/api/:path*`,
      },
      {
        source: '/oauth2/:path*',
        destination: `${backendInternalUrl}/oauth2/:path*`,
      },
    ];
  },
};

export default nextConfig;
