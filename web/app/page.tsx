'use client';

import { useThemeStore } from '@/store/theme.store';
import Logo from '@/theme/logo';
import ThemeToggle from '@/theme/toggle';
import { motion } from 'motion/react';
import Link from 'next/link';

const Home = () => {
  const theme = useThemeStore((state) => state.theme);

  return (
    <div className={`min-h-screen bg-linear-to-br ${theme.bg}`}>
      <ThemeToggle />
      <div className="grid grid-cols-2 h-screen max-w-5xl:grid-cols-1">
        <div
          className="flex flex-col justify-center items-start pl-48
            max-w-lg:px-12 max-w-lg:items-center max-w-sm:px-4"
        >
          <motion.h1
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 1 }}
            className={`mb-2 text-2xl tracking-wide ${theme.textSecondary}`}
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
            className={`my-12 text-6xl leading-normal tracking-wide ${theme.textPrimary} font-light`}
          >
            Built for the Modern Pack!
          </motion.p>

          <Link href="/auth/login">
            <motion.button
              initial={{ opacity: 0 }}
              animate={{
                opacity: 1,
                transition: { duration: 1 },
              }}
              className={`mt-8 px-12 py-3 rounded-lg text-baserounded-lg font-medium cursor-pointer transition-all ${theme.button.primary}`}
            >
              Start Howling
            </motion.button>
          </Link>
        </div>
        <div
          className="flex justify-center items-center p-8 relative
         max-[992px]:col-start-1 max-[992px]:row-start-1
         max-[280px]:p-4"
        >
          <Logo width={300} height={300} />
        </div>
      </div>
    </div>
  );
};

export default Home;
