import type { Metadata } from "next";
import { Providers } from "@/components/providers";
import "./globals.css";

export const metadata: Metadata = {
  title: "SubMeter — Subscription Billing Engine",
  description: "Usage-metered subscription billing with real-time MRR dashboards.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" data-scroll-behavior="smooth">
      <body>
        <Providers>
          {children}
        </Providers>
      </body>
    </html>
  );
}
