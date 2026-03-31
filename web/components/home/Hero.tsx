import { useThemeStore } from '@/features/theme/theme.store';
import { motion } from 'motion/react';

export function Hero() {
  const theme = useThemeStore(state => state.theme);

  return (
    <>
      <motion.h1
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 1 }}
        className={`mb-2 text-2xl tracking-wide ${theme['--color-text-secondary']}`}
      >
        Realtime Messaging
      </motion.h1>
      <motion.p
        variants={{
          hidden: { opacity: 0, x: -10 },
          visible: { opacity: 1, x: 0 },
        }}
        initial="hidden"
        animate="visible"
        transition={{ duration: 1 }}
        className={`my-12 text-6xl leading-normal tracking-wide  ${theme['--color-text-primary']} font-light`}
      >
        Built for the Modern Pack!
      </motion.p>
    </>
  );
}
