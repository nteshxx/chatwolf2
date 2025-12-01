'use client';

import { Chat } from '@/interfaces/chat';
import { useThemeStore } from '@/store/theme.store';
import { useState } from 'react';

export function Chats() {
  const { theme } = useThemeStore();
  const [selectedId, setSelectedId] = useState<string | null>('1');

  const conversations: Chat[] = [
    {
      id: '1',
      name: 'Alice Smith',
      lastMessage: 'Hey! How are you?',
      timestamp: '2m',
      unread: 2,
      online: true,
    },
    {
      id: '2',
      name: 'Bob Johnson',
      lastMessage: 'See you tomorrow!',
      timestamp: '1h',
      unread: 0,
      online: false,
    },
    {
      id: '3',
      name: 'Carol Williams',
      lastMessage: 'Thanks for your help',
      timestamp: '3h',
      unread: 5,
      online: true,
    },
    {
      id: '4',
      name: 'Katy Williams',
      lastMessage: 'Thanks for your help',
      timestamp: '3h',
      unread: 5,
      online: true,
    },
    {
      id: '5',
      name: 'Shelly Williams',
      lastMessage: 'Thanks for your help',
      timestamp: '3h',
      unread: 5,
      online: true,
    },
    {
      id: '6',
      name: 'Emily Williams',
      lastMessage: 'Thanks for your help',
      timestamp: '3h',
      unread: 5,
      online: true,
    },
    {
      id: '7',
      name: 'Alexa Williams',
      lastMessage: 'Thanks for your help',
      timestamp: '3h',
      unread: 5,
      online: true,
    },
    {
      id: '8',
      name: 'Neville Williams',
      lastMessage: 'Thanks for your help',
      timestamp: '3h',
      unread: 5,
      online: true,
    },
  ];

  return (
    <div
      className={`flex-1 mx-4 px-4 space-y-4 overflow-y-auto ${theme.scrollbar}`}
    >
      {conversations.map(conv => (
        <div
          key={conv.id}
          onClick={() => setSelectedId(conv.id)}
          className={`rounded-xl ${theme.glass} p-4 ${theme.glassHover} transition-all cursor-pointer`}
        >
          <div className="flex items-center gap-3">
            {/* Avatar */}
            <div className="relative shrink-0">
              <div
                className={`w-12 h-12 rounded-full ${theme.primarySolid} flex items-center justify-center text-white font-semibold`}
              >
                {conv.name.charAt(0)}
              </div>
            </div>

            {/* Info */}
            <div className="flex-1 min-w-0">
              <div className="flex flex-col items-start space-y-1">
                <h3
                  className={`${theme.textPrimary} font-medium text-sm truncate`}
                >
                  {conv.name}
                </h3>
                <div className={`${theme.textSecondary} text-sm truncate`}>
                  {conv.lastMessage}
                </div>
              </div>
            </div>

            {/* Last Seen & Unread Messages */}
            <div className="flex flex-col items-center justify-evenly">
              <span
                className={`${theme.textSecondary} text-xs shrink-0 w-12 h-6 text-center`}
              >
                {conv.timestamp}
              </span>
              {conv.unread > 0 && (
                <div
                  className={`w-6 h-6 font-semibold rounded-full bg-linear-to-r ${theme.primary} text-xs text-white flex items-center justify-center`}
                >
                  {conv.unread}
                </div>
              )}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
