'use client';

import Logo from '@/components/ui/ThemeLogo';
import ThemeToggle from '@/components/ui/ThemeToggle';
import { useTheme } from '@/theme/theme-provider';
import { motion } from 'motion/react';
import Link from 'next/link';

const Home = () => {
  const { themeId, theme, setTheme } = useTheme();

  return (
    <div className={`min-h-screen bg-linear-to-br ${theme.bg} text-slate-100`}>
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
            className="mb-2 text-2xl tracking-wide
           max-w-md:mb-0 max-w-md:text-[1.4rem]"
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
            className="my-12 text-6xl font-light leading-normal tracking-wide
          max-w-lg:my-4 max-w-lg:text-[3rem]"
          >
            Built for the Modern Pack!
          </motion.p>

          <Link href="/login">
            <motion.button
              initial={{ opacity: 0 }}
              animate={{
                opacity: 1,
                transition: { duration: 1 },
              }}
              className={`mt-8 px-12 py-3 text-base font-medium border-2 rounded-full cursor-pointer border-none bg-linear-to-r ${theme.primary} text-slate-950 shadow-lg`}
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
