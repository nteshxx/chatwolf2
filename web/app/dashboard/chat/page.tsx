'use client';

import { Search } from '@/components/dashboard/Search';
import { Conversations } from '@/components/dashboard/Conversations';
import { ChatBox } from '@/components/dashboard/ChatBox';
import { useThemeStore } from '@/store/theme.store';

export default function ChatPage() {
  const { theme } = useThemeStore();

  return (
    <>
      {/* MID SECTION - Search and Conversations */}
      <div className="pr-4 w-96">
        <Search />
        <Conversations />
      </div>

      {/* RIGHT SECTION - Chat Box */}
      <div className="w-3xl">
        <ChatBox />
      </div>
    </>
  );
}
