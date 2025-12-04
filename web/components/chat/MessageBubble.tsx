import { Message } from '@/interfaces/message';
import { useThemeStore } from '@/store/theme.store';

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
        <div className="inline">
          <span
            className={`text-base ${isOwn ? 'text-white' : theme.textPrimary}`}
          >
            {message.text}
          </span>
          <span
            className={`float-right ml-1 mt-2 text-xs ${
              isOwn ? 'text-white' : theme.textSecondary
            } whitespace-nowrap leading-none`}
          >
            {message.timestamp}
          </span>
        </div>
      </div>
    </div>
  );
}
