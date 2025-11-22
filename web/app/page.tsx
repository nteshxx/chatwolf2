'use client';

import { motion } from 'motion/react';
import Link from 'next/link';

const Home = () => {
  return (
    <div className="max-h-screen max-w-svw flex justify-center items-center overflow-hidden bg-black">
      <div className="grid grid-cols-2 h-screen max-w-5xl:grid-cols-1">
        <div
          className="flex flex-col text-white justify-center items-start px-24
            max-w-lg:px-12 max-w-lg:items-center max-w-sm:px-4"
        >
          <motion.h1
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 1 }}
            className="mb-2 text-[1.8rem] tracking-wide text-white
           max-w-md:mb-0 max-w-md:text-[1.4rem]"
          >
            Welcome to ChatWolf
          </motion.h1>
          <motion.p
            variants={{
              hidden: { opacity: 0, x: -10 },
              visible: { opacity: 1, x: 0 },
            }}
            initial="hidden"
            animate="visible"
            transition={{ duration: 1 }}
            className="my-12 text-[4rem] font-light leading-normal tracking-wide text-white
          max-w-lg:my-4 max-w-lg:text-[3rem]"
          >
            Designed For You!
          </motion.p>

          <Link href="/login">
            <motion.button
              initial={{ opacity: 0 }}
              animate={{
                opacity: 1,
                transition: { duration: 1 },
              }}
              className="mt-8 px-12 py-3 text-xl border-2 tracking-wider border-white rounded-lg cursor-pointer bg-black text-white hover:bg-white hover:text-black"
            >
              Get Started
            </motion.button>
          </Link>
        </div>
        <div
          className="flex justify-center items-center p-8 relative
         max-[992px]:col-start-1 max-[992px]:row-start-1
         max-[280px]:p-4"
        >
          <motion.img
            src="/icons/logo.svg"
            alt="logo"
            initial={{ opacity: 0, y: 20 }}
            animate={{
              opacity: 1,
              y: [0, -20, 0],
            }}
            transition={{
              opacity: { duration: 1 },
              y: {
                duration: 2,
                repeat: Infinity,
                ease: 'easeInOut',
                delay: 1,
              },
            }}
            className="absolute w-full h-full 
  max-w-[360px] max-h-[360px]
  max-[768px]:min-w-[300px] max-[768px]:min-h-[300px]
  max-[576px]:min-w-[150px] max-[576px]:min-h-[150px]"
          />
        </div>
      </div>
    </div>
  );
};

export default Home;
