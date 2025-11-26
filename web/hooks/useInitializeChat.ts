'use client'

import { useAuthStore } from '@/store/auth.store'
import { useChatStore } from '@/store/chat.store'
import { useEffect } from 'react'

export function useInitializeChat() {
  const { token, isAuthenticated } = useAuthStore()
  const { connect, disconnect } = useChatStore()

  useEffect(() => {
    if (isAuthenticated && token) {
      connect(process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:7000', token)
    }

    return () => {
      disconnect()
    }
  }, [isAuthenticated, token, connect, disconnect])
}