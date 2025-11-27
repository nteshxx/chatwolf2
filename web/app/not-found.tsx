'use client';

import Link from 'next/link';
import { motion } from 'framer-motion';
import { useThemeStore } from '@/store/theme.store';

export default function NotFound() {
  const { theme } = useThemeStore();

  return (
    <div
      className={`min-h-screen flex items-center justify-center bg-linear-to-br ${theme.bg}`}
    >
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="text-center px-6"
      >
        <h1
          className={`bg-linear-to-r ${theme.primary} bg-clip-text text-9xl font-bold text-transparent mb-4`}
        >
          404
        </h1>
        <h2 className={`text-3xl font-semibold ${theme.textPrimary} mb-4`}>
          Page Not Found
        </h2>
        <p className={`${theme.textSecondary} mb-8`}>
          {"The page you're looking for doesn't exist."}
        </p>

        <Link href="/">
          <motion.button
            className={`px-8 py-3 font-medium rounded-lg ${theme.button.primary} tracking-wider cursor-pointer`}
          >
            Go Home
          </motion.button>
        </Link>
      </motion.div>
    </div>
  );
}
