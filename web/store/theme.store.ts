import { ThemeState } from '@/interfaces/theme-state'
import { themes } from '@/theme/themes'
import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export const useThemeStore = create<ThemeState>()(
  persist(
    (set) => ({
      themeId: 'night',
      theme: themes.night,
      setTheme: (id) => set({ themeId: id, theme: themes[id] }),
    }),
    {
      name: 'theme-storage',
    }
  )
)