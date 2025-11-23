'use client';

import { themes, useTheme } from '@/theme/theme-provider';
import { Theme } from '@/types/theme.type';
import { motion } from 'motion/react';

export default function ThemeToggle() {
  const { themeId, setTheme } = useTheme();

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
            className={`rounded-full border px-3 py-1 text-xs capitalize ${
              themeId === id
                ? 'border-white/80 bg-white/10'
                : 'border-white/20 bg-black/20 text-slate-300 hover:border-white/40'
            }`}
          >
            {themes[id].name}
          </button>
        ))}
      </section>
    </motion.div>
  );
}
