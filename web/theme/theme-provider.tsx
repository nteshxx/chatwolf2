'use client';

import { Theme } from '@/types/theme.type';
import { createContext, useContext, useState } from 'react';

// themes
export const themes = {
  night: {
    id: 'night',
    name: 'Night Hunt',
    // Backgrounds
    bg: 'from-black via-slate-950 to-slate-900',
    bgSolid: 'bg-slate-950',
    bgCard: 'bg-slate-900/50',
    
    // Primary colors
    primary: 'from-blue-500 to-indigo-500',
    primarySolid: 'bg-blue-500',
    primaryHover: 'hover:from-blue-600 hover:to-indigo-600',
    
    // Text colors
    textPrimary: 'text-slate-100',
    textSecondary: 'text-slate-400',
    textMuted: 'text-slate-600',

    // Loader
    loader: 'border-slate-400',
    
    // Borders
    border: 'border-slate-800',
    borderHover: 'hover:border-slate-700',
    
    // Glassmorphism
    glass: 'bg-slate-900/30 backdrop-blur-xl border border-slate-800/50',
    glassHover: 'hover:bg-slate-900/50 hover:border-slate-700/50',
    
    // Buttons
    button: {
      primary: 'bg-gradient-to-r from-blue-500 to-indigo-500 hover:from-blue-600 hover:to-indigo-600 text-white',
      secondary: 'bg-slate-800 hover:bg-slate-700 text-slate-100 border border-slate-700',
      ghost: 'hover:bg-slate-800/50 text-slate-300 hover:text-slate-100',
      glass: 'bg-slate-900/30 backdrop-blur-xl border border-slate-800/50 hover:bg-slate-900/50 text-slate-100',
    },
    
    // Tabs
    tabs: {
      inactive: 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50',
      active: 'text-slate-100 bg-gradient-to-r from-blue-500/20 to-indigo-500/20 border-b-2 border-blue-500',
      container: 'border-b border-slate-800',
    },
    
    // Input fields
    input: 'bg-slate-900/50 border border-slate-800 text-slate-100 placeholder:text-slate-600 focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20',
    
    description: 'Deep, focused UI tuned for low‑light wolfpacks.',
  },
  
  neon: {
    id: 'neon',
    name: 'Neon Howl',
    // Backgrounds
    bg: 'from-slate-950 via-purple-950 to-fuchsia-900',
    bgSolid: 'bg-purple-950',
    bgCard: 'bg-purple-900/50',
    
    // Primary colors
    primary: 'from-fuchsia-500 to-cyan-400',
    primarySolid: 'bg-fuchsia-500',
    primaryHover: 'hover:from-fuchsia-600 hover:to-cyan-500',
    
    // Text colors
    textPrimary: 'text-slate-100',
    textSecondary: 'text-purple-300',
    textMuted: 'text-purple-600',

    // Loader
    loader: 'border-purple-300',
    
    // Borders
    border: 'border-purple-800',
    borderHover: 'hover:border-purple-700',
    
    // Glassmorphism
    glass: 'bg-purple-900/30 backdrop-blur-xl border border-purple-800/50',
    glassHover: 'hover:bg-purple-900/50 hover:border-purple-700/50',
    
    // Buttons
    button: {
      primary: 'bg-gradient-to-r from-fuchsia-500 to-cyan-400 hover:from-fuchsia-600 hover:to-cyan-500 text-white',
      secondary: 'bg-purple-800 hover:bg-purple-700 text-purple-100 border border-purple-700',
      ghost: 'hover:bg-purple-800/50 text-purple-300 hover:text-purple-100',
      glass: 'bg-purple-900/30 backdrop-blur-xl border border-purple-800/50 hover:bg-purple-900/50 text-purple-100',
    },
    
    // Tabs
    tabs: {
      inactive: 'text-purple-400 hover:text-purple-200 hover:bg-purple-800/50',
      active: 'text-purple-100 bg-gradient-to-r from-fuchsia-500/20 to-cyan-400/20 border-b-2 border-fuchsia-500',
      container: 'border-b border-purple-800',
    },
    
    // Input fields
    input: 'bg-purple-900/50 border border-purple-800 text-purple-100 placeholder:text-purple-600 focus:border-fuchsia-500 focus:ring-2 focus:ring-fuchsia-500/20',
    
    description: 'Vibrant gradients for high‑energy communities.',
  },
  
  sunset: {
    id: 'sunset',
    name: 'Dusk Run',
    // Backgrounds
    bg: 'from-slate-950 via-rose-900 to-amber-800',
    bgSolid: 'bg-rose-950',
    bgCard: 'bg-rose-900/50',
    
    // Primary colors
    primary: 'from-amber-400 to-rose-400',
    primarySolid: 'bg-amber-500',
    primaryHover: 'hover:from-amber-500 hover:to-rose-500',
    
    // Text colors
    textPrimary: 'text-slate-100',
    textSecondary: 'text-amber-300',
    textMuted: 'text-rose-800',

    // Loader
    loader: 'border-amber-300',
    
    // Borders
    border: 'border-rose-800',
    borderHover: 'hover:border-rose-700',
    
    // Glassmorphism
    glass: 'bg-rose-900/30 backdrop-blur-xl border border-rose-800/50',
    glassHover: 'hover:bg-rose-900/50 hover:border-rose-700/50',
    
    // Buttons
    button: {
      primary: 'bg-gradient-to-r from-amber-400 to-rose-400 hover:from-amber-500 hover:to-rose-500 text-white',
      secondary: 'bg-rose-800 hover:bg-rose-700 text-rose-100 border border-rose-700',
      ghost: 'hover:bg-rose-800/50 text-amber-300 hover:text-amber-100',
      glass: 'bg-rose-900/30 backdrop-blur-xl border border-rose-800/50 hover:bg-rose-900/50 text-rose-100',
    },
    
    // Tabs
    tabs: {
      inactive: 'text-amber-400 hover:text-amber-200 hover:bg-rose-800/50',
      active: 'text-amber-100 bg-gradient-to-r from-amber-400/20 to-rose-400/20 border-b-2 border-amber-400',
      container: 'border-b border-rose-800',
    },
    
    // Input fields
    input: 'bg-rose-900/50 border border-rose-800 text-rose-100 placeholder:text-rose-700 focus:border-amber-400 focus:ring-2 focus:ring-amber-400/20',
    
    description: 'Warm, cinematic glow for casual conversation.',
  },
  
  dawn: {
    id: 'dawn',
    name: 'Dawn Break',
    // Backgrounds
    bg: 'from-slate-50 via-blue-50 to-indigo-100',
    bgSolid: 'bg-slate-50',
    bgCard: 'bg-white/50',
    
    // Primary colors
    primary: 'from-blue-600 to-indigo-600',
    primarySolid: 'bg-blue-600',
    primaryHover: 'hover:from-blue-700 hover:to-indigo-700',
    
    // Text colors
    textPrimary: 'text-slate-900',
    textSecondary: 'text-slate-600',
    textMuted: 'text-slate-400',

    // Loader
    loader: 'border-slate-600',
    
    // Borders
    border: 'border-slate-200',
    borderHover: 'hover:border-slate-300',
    
    // Glassmorphism
    glass: 'bg-white/40 backdrop-blur-xl border border-slate-200/60',
    glassHover: 'hover:bg-white/60 hover:border-slate-300/60',
    
    // Buttons
    button: {
      primary: 'bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white',
      secondary: 'bg-slate-100 hover:bg-slate-200 text-slate-900 border border-slate-300',
      ghost: 'hover:bg-slate-100/80 text-slate-700 hover:text-slate-900',
      glass: 'bg-white/40 backdrop-blur-xl border border-slate-200/60 hover:bg-white/60 text-slate-900',
    },
    
    // Tabs
    tabs: {
      inactive: 'text-slate-600 hover:text-slate-900 hover:bg-slate-100/50',
      active: 'text-slate-900 bg-gradient-to-r from-blue-600/10 to-indigo-600/10 border-b-2 border-blue-600',
      container: 'border-b border-slate-200',
    },
    
    // Input fields
    input: 'bg-white/60 border border-slate-200 text-slate-900 placeholder:text-slate-400 focus:border-blue-600 focus:ring-2 focus:ring-blue-600/20',
    
    description: 'Clean, bright interface for daylight sessions.',
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
