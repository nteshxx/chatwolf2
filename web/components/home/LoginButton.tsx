import { useThemeStore } from '@/features/theme/theme.store';
import { motion } from 'motion/react';
import Link from 'next/link';

export function LoginButton() {
  const theme = useThemeStore(state => state.theme);

  return (
    <Link href="/auth/login">
      <motion.button
        initial={{ opacity: 0 }}
        animate={{
          opacity: 1,
          transition: { duration: 1 },
        }}
        className={`mt-8 px-12 py-3 rounded-lg text-baserounded-lg font-medium cursor-pointer transition-all ${theme['--color-button-primary']}`}
      >
        Start Howling
      </motion.button>
    </Link>
  );
}
