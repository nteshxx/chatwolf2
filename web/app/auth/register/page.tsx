'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/store/auth.store';
import { useThemeStore } from '@/store/theme.store';

export default function RegisterPage() {
  const router = useRouter();
  const { register, isLoading, error, setError, clearError } = useAuthStore();
  const { theme } = useThemeStore();
  
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
  })

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    clearError()

    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    try {
      await register(formData.name, formData.email, formData.password)
      router.push('/verify-email')

    } catch (error) {
      console.error('Registration error:', error)
    }
  }

  return (
    <div className={`rounded-2xl ${theme.glass} ${theme.textPrimary} space-y-6 p-8`}>
      <div>
        <h2 className={`bg-linear-to-r ${theme.primary} bg-clip-text text-3xl font-bold text-transparent mb-2`}>Join The Pack</h2>
        <p className={`${theme.textSecondary} text-sm`}>Sign up to get started with ChatWolf</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="bg-red-500/10 border border-red-500 text-red-500 px-4 py-3 rounded">
            {error}
          </div>
        )}

        <div>
          <label
            htmlFor="name"
            className={`mb-2 block text-sm font-medium ${theme.textPrimary}`}
          >
            Full Name
          </label>
          <input
            id="name"
            type="text"
            required
            value={formData.name}
            onChange={e => setFormData({ ...formData, name: e.target.value })}
            className={`w-full rounded-lg px-4 py-2.5 outline-none transition-all ${theme.input}`}
            placeholder="John Doe"
            autoComplete='off'
          />
        </div>

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
            autoComplete='off'
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
            minLength={8}
            value={formData.password}
            onChange={e =>
              setFormData({ ...formData, password: e.target.value })
            }
            className={`w-full rounded-lg px-4 py-2.5 outline-none transition-all ${theme.input}`}
            placeholder="••••••••"
            autoComplete='off'
          />
        </div>

        <div>
          <label
            htmlFor="confirmPassword"
            className={`mb-2 block text-sm font-medium ${theme.textPrimary}`}
          >
            Confirm Password
          </label>
          <input
            id="confirmPassword"
            type="password"
            required
            minLength={8}
            value={formData.confirmPassword}
            onChange={e =>
              setFormData({ ...formData, confirmPassword: e.target.value })
            }
            className={`w-full rounded-lg px-4 py-2.5 mb-3 outline-none transition-all ${theme.input}`}
            placeholder="••••••••"
            autoComplete='off'
          />
        </div>

        <button
          type="submit"
          disabled={isLoading}
          className={`w-full py-3 font-medium rounded-lg px-6 text-sm  ${theme.button.primary} cursor-pointer`}
        >
          {isLoading ? 'Creating Account...' : 'Create Account'}
        </button>
      </form>

      <div className="text-center">
        <p className={`${theme.textSecondary}`}>
          Already have an account?{' '}
          <Link
            href="/auth/login"
            className={`font-medium ${theme.textPrimary} hover:underline`}
          >
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
