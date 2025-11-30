'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/store/auth.store';
import { useThemeStore } from '@/store/theme.store';
import Options from '@/components/dashboard/Options';
import Profile from '@/components/dashboard/Profile';

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const { theme } = useThemeStore();
  // const { isAuthenticated } = useAuthStore()
  const isAuthenticated = true; // TODO: Replace with actual auth

  useEffect(() => {
    // Check authentication on mount
    if (!isAuthenticated) {
      router.push('/auth/login');
    }
  }, [isAuthenticated, router]);

  return (
    <div
      className={`min-h-screen flex items-center justify-center bg-linear-to-br ${theme.bg} ${theme.textPrimary}`}
    >
      <div
        className={`w-[96vw] min-h-[92vh] p-4 m-4 flex rounded-2xl ${theme.glass} ${theme.textPrimary}`}
      >
        {/* LEFT SECTION - User and Online Users */}
        <div className="w-56">
          <Profile />
          <Options />
        </div>
        {children}
      </div>
    </div>
  );
}
