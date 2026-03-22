/** @type {import('next').NextConfig} */
const nextConfig = {
  // The backend SSE endpoint is on a different origin during development
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: 'http://localhost:8080/api/:path*',
      },
    ];
  },
};

module.exports = nextConfig;
