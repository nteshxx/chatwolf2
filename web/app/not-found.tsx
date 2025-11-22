'use client';

import Link from 'next/link';
import { motion } from 'framer-motion';

export default function NotFound() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-black">
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="text-center px-6"
      >
        <h1 className="text-9xl font-bold text-white mb-4">404</h1>
        <h2 className="text-3xl font-semibold text-white mb-4">
          Page Not Found
        </h2>
        <p className="text-gray-400 mb-8">
          The page you're looking for doesn't exist.
        </p>

        <Link href="/">
          <motion.button
            className="px-8 py-3 bg-black text-white border-2 tracking-wider border-white rounded-lg cursor-pointer font-medium hover:bg-white hover:text-black"
          >
            Go Home
          </motion.button>
        </Link>
      </motion.div>
    </div>
  );
}
