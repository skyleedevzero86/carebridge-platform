import type { Metadata } from "next";
import { Noto_Sans_KR, Space_Grotesk } from "next/font/google";

import "./globals.css";

const heading = Space_Grotesk({ variable: "--font-heading", subsets: ["latin"] });
const body = Noto_Sans_KR({ variable: "--font-body", subsets: ["latin"], weight: ["400", "500", "700"] });

export const metadata: Metadata = {
  title: "CareBridge Interface Console",
  description: "Medical device interface server console with Spring Boot 4, PostgreSQL, Redis, Next.js, and WebSocket chat.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="ko">
      <body className={`${heading.variable} ${body.variable}`}>{children}</body>
    </html>
  );
}