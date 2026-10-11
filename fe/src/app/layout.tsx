import type { Metadata } from 'next';
import { Be_Vietnam_Pro } from 'next/font/google';
import './globals.css';
import { Providers } from './providers';

// Cấu hình font chữ không chân (sans-serif) tối ưu 100% cho tiếng Việt
const beVietnamPro = Be_Vietnam_Pro({
  subsets: ['vietnamese', 'latin'],
  weight: ['400', '500', '600', '700', '800'],
  variable: '--font-sans',
  display: 'swap',
});

export const metadata: Metadata = {
  title: 'Mentify - Nền tảng Cố vấn Học thuật & Hướng nghiệp',
  description:
    'Hệ sinh thái kết nối học sinh THPT, sinh viên đại học với chuyên gia cố vấn và định hướng nghề nghiệp.',
  keywords: ['mentify', 'cố vấn', 'hướng nghiệp', 'thông tư 74/2026', 'học sinh', 'sinh viên'],
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="vi" className={beVietnamPro.variable} suppressHydrationWarning>
      <body className="min-h-screen bg-background text-foreground font-sans antialiased">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
