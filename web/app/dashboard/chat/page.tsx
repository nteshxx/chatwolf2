'use client';

import { Search } from '@/components/chat/Search';
import { Chats } from '@/components/chat/Chats';
import { ChatWindow } from '@/components/chat/ChatWindow';
import { useThemeStore } from '@/store/theme.store';

export default function ChatPage() {
  const { theme } = useThemeStore();

  return (
    <>
      <div className="w-96 flex flex-col shrink-0">
        <Search />
        <div className={`m-4 h-px ${theme.border} border-t`} />
        <Chats />
      </div>
      <div className="flex-1 overflow-hidden">
        <ChatWindow />
      </div>
    </>
  );
}
