'use client';

import { useThemeStore } from '@/store/theme.store';
import {
  AdjustmentsHorizontalIcon,
  ChatBubbleLeftIcon,
  UserIcon,
} from '@heroicons/react/24/solid';

export default function Navigation() {
  const { theme } = useThemeStore();

  return (
    <div className="px-2 space-y-2">
      <button
        className={`w-full flex items-center space-x-3 px-4 py-3 rounded-lg relative transition-all ${theme.button.ghost} cursor-pointer`}
      >
        <ChatBubbleLeftIcon className="size-5" />
        <span>Messages</span>
        <span
          className={`absolute right-4 w-6 h-6 font-semibold rounded-full bg-linear-to-r ${theme.primary} text-xs text-white flex items-center justify-center`}
        >
          3
        </span>
      </button>
      <button
        className={`w-full flex items-center space-x-3 px-4 py-3 rounded-lg relative transition-all ${theme.button.ghost} cursor-pointer`}
      >
        <UserIcon className="size-5" />
        <span>Profile</span>
      </button>
      <button
        className={`w-full flex items-center space-x-3 px-4 py-3 rounded-lg relative transition-all ${theme.button.ghost} cursor-pointer`}
      >
        <AdjustmentsHorizontalIcon className="size-5" />
        <span>Settings</span>
      </button>
    </div>
  );
}
