import type { NextConfig } from "next";

const reactCompiler = process.env.NEXT_REACT_COMPILER === "1";

const nextConfig: NextConfig = {
  ...(reactCompiler ? { reactCompiler: true } : {}),
  webpack: (config, { dev }) => {
    if (dev) {
      config.watchOptions = {
        ...config.watchOptions,
        poll: 1500,
        aggregateTimeout: 300,
      };
    }
    return config;
  },
};

export default nextConfig;