import { ThemeState } from '@/features/theme/theme.interface';
import { themes } from '@/features/theme/themes';
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export const useThemeStore = create<ThemeState>()(
  persist(
    set => ({
      themeId: 'night',
      theme: themes.night,
      setTheme: id => set({ themeId: id, theme: themes[id] }),
    }),
    {
      name: 'theme-storage',
    }
  )
);
