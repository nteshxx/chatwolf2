'use client'

import { useThemeStore } from '@/store/theme.store';
import Image from 'next/image';

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { theme } = useThemeStore();

  return (
    <div className={`min-h-screen bg-linear-to-br ${theme.bg} ${theme.textPrimary} flex`}>
      {/* Left Side - Branding */}
      <div className="hidden lg:flex lg:w-1/2 items-center justify-center p-12">
        <div className="text-center">
          <Image
            src="/icons/logo.svg"
            alt="ChatWolf Logo"
            width={200}
            height={200}
            className="mx-auto mb-8"
          />
          <h1 className="text-4xl font-bold mb-4">ChatWolf</h1>
          <p className={`${theme.textSecondary} text-lg`}>
            The Untamed Network
          </p>
        </div>
      </div>

      {/* Right Side - Auth Forms */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-8">
        <div className="w-full max-w-md">
          {/* Mobile Logo */}
          <div className="lg:hidden text-center mb-8">
            <Image
              src="/logo.svg"
              alt="ChatWolf Logo"
              width={80}
              height={80}
              className="mx-auto mb-4"
            />
            <h2 className="text-2xl font-bold">ChatWolf</h2>
          </div>

          {children}
        </div>
      </div>
    </div>
  );
}
