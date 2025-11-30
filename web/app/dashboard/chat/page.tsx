'use client';

import { Search } from '@/components/chat/Search';
import { Conversations } from '@/components/chat/Conversations';
import { ChatBox } from '@/components/chat/ChatBox';
import { useThemeStore } from '@/store/theme.store';

export default function ChatPage() {
  const { theme } = useThemeStore();

  return (
    <>
      <div className="pr-4 w-96">
        <Search />
        <Conversations />
      </div>
      <div className="w-3xl">
        <ChatBox />
      </div>
    </>
  );
}
