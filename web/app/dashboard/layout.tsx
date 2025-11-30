'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/store/auth.store';
import { useThemeStore } from '@/store/theme.store';
import Navigation from '@/components/dashboard/Navigation';
import Profile from '@/components/dashboard/Profile';
import Footer from '@/components/dashboard/Footer';

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
        className={`w-[96vw] h-[92vh] p-4 m-4 flex rounded-xl ${theme.bgCard} ${theme.border} ${theme.textPrimary} border overflow-hidden`}
      >
        <div className="w-56 flex flex-col shrink-0">
          <Profile />
          <div className={`my-4 h-px ${theme.border} border-t`} />
          <div className="flex-1 overflow-y-auto">
            <Navigation />
          </div>
          <div className={`my-2 h-px ${theme.border} border-t`} />
          <Footer />
        </div>
        <div className="flex-1 flex overflow-hidden">{children}</div>
      </div>
    </div>
  );
}
