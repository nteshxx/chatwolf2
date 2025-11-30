'use client';

import { useThemeStore } from '@/store/theme.store';
import { MagnifyingGlassIcon } from '@heroicons/react/24/solid';
import { useState } from 'react';

export function Search() {
  const { theme } = useThemeStore();
  const [searchQuery, setSearchQuery] = useState('');

  return (
    <div className="p-4">
      <div className="relative">
        <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 size-5 z-10" />
        <input
          type="text"
          placeholder="Search"
          value={searchQuery}
          onChange={e => setSearchQuery(e.target.value)}
          className={`w-full rounded-lg pl-10 pr-4 py-2 text-sm ${theme.textPrimary} outline-none transition-all ${theme.input}`}
        />
      </div>
    </div>
  );
}
