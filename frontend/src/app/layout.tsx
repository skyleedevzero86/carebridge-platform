import type { Metadata } from "next";
import { Noto_Sans_KR, Space_Grotesk } from "next/font/google";

import "./globals.css";

const heading = Space_Grotesk({ variable: "--font-heading", subsets: ["latin"] });
const body = Noto_Sans_KR({ variable: "--font-body", subsets: ["latin"], weight: ["400", "500", "700"] });

export const metadata: Metadata = {
  title: "CareBridge 연동 콘솔",
  description: "의료기기·HL7 연동 운영 콘솔. Spring Boot, PostgreSQL, Redis, Next.js, 웹소켓.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="ko">
      <body className={`${heading.variable} ${body.variable}`}>{children}</body>
    </html>
  );
}