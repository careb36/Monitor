/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',

  // Proxy /api/* to the backend. In Docker the BACKEND_URL build-arg
  // resolves to http://backend:8080; locally it falls back to localhost.
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: `${process.env.BACKEND_URL || 'http://localhost:8080'}/api/:path*`,
      },
    ];
  },
};

module.exports = nextConfig;
