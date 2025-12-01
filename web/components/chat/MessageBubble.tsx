import { Message } from '@/interfaces/message';
import { useThemeStore } from '@/store/theme.store';
import { memo } from 'react';

export function MessageBubble({
  message,
  isOwn,
}: {
  message: Message;
  isOwn: boolean;
}) {
  const { theme } = useThemeStore();

  return (
    <div className={`flex items-start gap-3 ${isOwn ? 'justify-end' : ''}`}>
      <div
        className={`max-w-xs rounded-2xl px-4 py-2 ${
          isOwn ? `bg-linear-to-r ${theme.primary}` : theme.glass
        }`}
      >
        <div className="flex items-end gap-2">
          <p className={`text-sm ${theme.textPrimary}`}>{message.text}</p>
          <div className="flex items-center gap-1 shrink-0">
            <span
              className={`text-xs ${isOwn ? theme.textPrimary : theme.textMuted} leading-none`}
            >
              {message.timestamp}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
