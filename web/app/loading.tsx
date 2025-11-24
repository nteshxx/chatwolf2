'use client';

import { useTheme } from '@/theme/theme-provider';
import { motion } from 'framer-motion';

export default function Loading() {
  const { theme } = useTheme();

  return (
    <div className={`min-h-screen flex items-center justify-center bg-linear-to-br ${theme.bg}`}>
      <div className="text-center">
        <motion.div
          className="inline-block"
          animate={{
            rotate: 360,
          }}
          transition={{
            duration: 1,
            repeat: Infinity,
            ease: 'linear',
          }}
        >
          <div className={`w-16 h-16 border-4 ${theme.loader} border-t-transparent rounded-full`} />
        </motion.div>
      </div>
    </div>
  );
}
