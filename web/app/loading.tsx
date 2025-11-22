'use client';

import { motion } from 'framer-motion';

export default function Loading() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-black">
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
          <div className="w-16 h-16 border-4 border-white border-t-transparent rounded-full" />
        </motion.div>
      </div>
    </div>
  );
}
