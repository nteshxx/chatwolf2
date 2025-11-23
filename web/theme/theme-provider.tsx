'use client';

import { Theme } from '@/types/theme.type';
import { createContext, useContext, useState } from 'react';

// themes
export const themes = {
  night: {
    id: 'night',
    name: 'Night Hunt',
    bg: 'from-black via-slate-950 to-slate-900',
    primary: 'from-blue-500 to-indigo-500',
    description: 'Deep, focused UI tuned for low‑light wolfpacks.',
  },
  neon: {
    id: 'neon',
    name: 'Neon Howl',
    bg: 'from-slate-950 via-purple-950 to-fuchsia-900',
    primary: 'from-fuchsia-500 to-cyan-400',
    description: 'Vibrant gradients for high‑energy communities.',
  },
  sunset: {
    id: 'sunset',
    name: 'Dusk Run',
    bg: 'from-slate-950 via-rose-900 to-amber-800',
    primary: 'from-amber-400 to-rose-400',
    description: 'Warm, cinematic glow for casual conversation.',
  },
};

// props
interface ThemeContextType {
  themeId: Theme;
  theme: (typeof themes)[Theme];
  setTheme: (id: Theme) => void;
}

// context
export const ThemeContext = createContext<ThemeContextType | null>(null);

// provider
export default function ThemeProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const [themeId, setTheme] = useState<Theme>('night');

  return (
    <ThemeContext.Provider
      value={{
        themeId,
        theme: themes[themeId],
        setTheme,
      }}
    >
      {children}
    </ThemeContext.Provider>
  );
}

// hook
export function useTheme() {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useTheme must be used inside <ThemeProvider>');
  return ctx;
}
