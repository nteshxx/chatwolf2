'use client';

import { useEffect } from 'react';
import { motion } from 'framer-motion';

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // Log the error to an error reporting service
    console.error(error);
  }, [error]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-black">
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="text-center px-6"
      >
        <div className="mb-8">
          <h1 className="text-6xl font-bold text-red-500 mb-4">Oops!</h1>
          <h2 className="text-2xl font-semibold text-white mb-2">
            Something went wrong
          </h2>
          <p className="text-gray-400 mb-6">
            {error.message || 'An unexpected error occurred'}
          </p>
        </div>

        <div className="space-x-4">
          <motion.button
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            onClick={reset}
            className="px-6 py-3 bg-white text-black rounded-lg font-medium hover:bg-gray-200 transition-colors"
          >
            Try Again
          </motion.button>

          <motion.button
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            onClick={() => (window.location.href = '/')}
            className="px-6 py-3 border-2 border-white text-white rounded-lg font-medium hover:bg-white hover:text-black transition-colors"
          >
            Go Home
          </motion.button>
        </div>
      </motion.div>
    </div>
  );
}
