'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useThemeStore } from '@/store/theme.store';
import { useAuthStore } from '@/store/auth.store';

export default function LoginPage() {
  const router = useRouter();
  const { login, isLoading, error, clearError, pendingVerificationEmail } =
    useAuthStore();
  const { theme } = useThemeStore();

  const [formData, setFormData] = useState({
    email: '',
    password: '',
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    clearError();

    try {
      await login(formData.email, formData.password);

      if (pendingVerificationEmail) {
        router.push('/verify-email');
      } else {
        router.push('/chat');
      }
    } catch (error) {
      console.error('Login error:', error);
    }
  };

  return (
    <div
      className={`rounded-2xl ${theme.glass} ${theme.textPrimary} space-y-6 p-8`}
    >
      <div>
        <h2
          className={`bg-linear-to-r ${theme.primary} bg-clip-text text-3xl font-bold text-transparent mb-2`}
        >
          Your Pack Awaits
        </h2>
        <p className={`${theme.textSecondary} text-sm`}>
          Sign in to continue to ChatWolf
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="bg-red-500/10 border border-red-500 text-red-500 px-4 py-3 rounded">
            {error}
          </div>
        )}

        <div>
          <label
            htmlFor="email"
            className={`mb-2 block text-sm font-medium ${theme.textPrimary}`}
          >
            Email
          </label>
          <input
            id="email"
            type="email"
            required
            value={formData.email}
            onChange={e => setFormData({ ...formData, email: e.target.value })}
            className={`w-full rounded-lg px-4 py-2.5 outline-none transition-all ${theme.input}`}
            placeholder="you@example.com"
            autoComplete="off"
          />
        </div>

        <div>
          <label
            htmlFor="password"
            className={`mb-2 block text-sm font-medium ${theme.textPrimary}`}
          >
            Password
          </label>
          <input
            id="password"
            type="password"
            required
            value={formData.password}
            onChange={e =>
              setFormData({ ...formData, password: e.target.value })
            }
            className={`w-full rounded-lg px-4 py-2.5 outline-none transition-all ${theme.input}`}
            placeholder="••••••••"
            autoComplete="off"
          />
        </div>

        <div className="flex items-center justify-end">
          <Link
            href="/auth/forgot-password"
            className={`text-sm ${theme.textSecondary} hover:underline hover:${theme.textPrimary}`}
          >
            Forgot password?
          </Link>
        </div>

        <button
          type="submit"
          disabled={isLoading}
          className={`w-full py-3 font-medium rounded-lg px-6 text-sm  ${theme.button.primary} cursor-pointer`}
        >
          {isLoading ? 'Signing In...' : 'Sign In'}
        </button>
      </form>

      <div className="text-center">
        <p className={`${theme.textSecondary}`}>
          Don't have an account?{' '}
          <Link
            href="/auth/register"
            className={`font-medium ${theme.textPrimary} hover:underline`}
          >
            Sign up
          </Link>
        </p>
      </div>
    </div>
  );
}
