import { AuthState } from '@/interfaces/auth-state'
import { create } from 'zustand'
import { persist } from 'zustand/middleware'

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:7000/api/auth'

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      refreshToken: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,
      pendingVerificationEmail: null,

      login: async (email, password) => {
        set({ isLoading: true, error: null })
        
        try {
          const response = await fetch(`${API_URL}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password }),
          })

          const data = await response.json()

          if (!response.ok) {
            throw new Error(data.message || 'Login failed')
          }

          if (!data.emailVerified) {
            set({ 
              pendingVerificationEmail: email,
              isLoading: false,
              error: 'Please verify your email first'
            })
            return
          }

          set({
            user: data.user,
            token: data.accessToken,
            refreshToken: data.refreshToken,
            isAuthenticated: true,
            isLoading: false,
            error: null,
          })
        } catch (error: any) {
          set({ 
            isLoading: false, 
            error: error.message || 'Login failed' 
          })
          throw error
        }
      },

      register: async (name, email, password) => {
        set({ isLoading: true, error: null })
        
        try {
          const response = await fetch(`${API_URL}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, email, password }),
          })

          const data = await response.json()

          if (!response.ok) {
            throw new Error(data.message || 'Registration failed')
          }

          set({ 
            pendingVerificationEmail: email,
            isLoading: false,
            error: null,
          })
        } catch (error: any) {
          set({ 
            isLoading: false, 
            error: error.message || 'Registration failed' 
          })
          throw error
        }
      },

      verifyEmail: async (email, otp) => {
        set({ isLoading: true, error: null })
        
        try {
          const response = await fetch(`${API_URL}/verify-email`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, otp }),
          })

          const data = await response.json()

          if (!response.ok) {
            throw new Error(data.message || 'Verification failed')
          }

          set({
            user: data.user,
            token: data.accessToken,
            refreshToken: data.refreshToken,
            isAuthenticated: true,
            isLoading: false,
            error: null,
            pendingVerificationEmail: null,
          })
        } catch (error: any) {
          set({ 
            isLoading: false, 
            error: error.message || 'Verification failed' 
          })
          throw error
        }
      },

      resendOTP: async (email) => {
        set({ isLoading: true, error: null })
        
        try {
          const response = await fetch(`${API_URL}/resend-otp`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email }),
          })

          const data = await response.json()

          if (!response.ok) {
            throw new Error(data.message || 'Failed to resend OTP')
          }

          set({ isLoading: false, error: null })
        } catch (error: any) {
          set({ 
            isLoading: false, 
            error: error.message || 'Failed to resend OTP' 
          })
          throw error
        }
      },

      logout: () => {
        set({
          user: null,
          token: null,
          refreshToken: null,
          isAuthenticated: false,
          error: null,
          pendingVerificationEmail: null,
        })
      },

      refreshAccessToken: async () => {
        const { refreshToken } = get()
        if (!refreshToken) {
          get().logout()
          return
        }

        try {
          const response = await fetch(`${API_URL}/refresh-token`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken }),
          })

          const data = await response.json()

          if (!response.ok) {
            throw new Error('Token refresh failed')
          }

          set({
            token: data.accessToken,
            refreshToken: data.refreshToken,
          })
        } catch (error) {
          get().logout()
          throw error
        }
      },

      updateUser: (userData) => 
        set((state) => ({
          user: state.user ? { ...state.user, ...userData } : null
        })),

      setError: (error: string) => set({ error }),

      clearError: () => set({ error: null }),
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        user: state.user,
        token: state.token,
        refreshToken: state.refreshToken,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
)