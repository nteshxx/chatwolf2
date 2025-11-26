'use client';

import { useThemeStore } from '@/store/theme.store';
import { Theme } from '@/types/theme.type';
import { motion } from 'motion/react';
import { themes } from './themes';

export default function ThemeToggle() {
  const { themeId, theme, setTheme } = useThemeStore();

  return (
    <motion.div
      variants={{
        hidden: { opacity: 0, x: 10 },
        visible: { opacity: 1, x: 0 },
      }}
      initial="hidden"
      animate="visible"
      transition={{ duration: 1 }}
      className="absolute top-5 right-5 inline-block z-10"
    >
      <section className="mb-6 flex gap-2">
        {(Object.keys(themes) as Theme[]).map(id => (
          <button
            key={id}
            onClick={() => setTheme(id)}
            className={`rounded-full px-3 py-1 text-xs capitalize ${
              themeId === id
                ? theme.button.primary
                : theme.button.secondary
            }`}
          >
            {themes[id].name}
          </button>
        ))}
      </section>
    </motion.div>
  );
}
