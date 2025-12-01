import { Message } from '@/interfaces/message';
import { useCallback, useEffect, useRef, useState } from 'react';
import { MessageBubble } from './MessageBubble';
import { useThemeStore } from '@/store/theme.store';

export function ChatWindow() {
  const { theme } = useThemeStore();
  const [messageInput, setMessageInput] = useState('');
  const [messages, setMessages] = useState<Message[]>([
    {
      id: '1',
      text: 'Hey! Check out this new theme system.',
      sender: 'other',
      timestamp: '10:30 AM',
      status: 'sent',
    },
    {
      id: '2',
      text: 'Looks amazing! The glassmorphism is perfect.',
      sender: 'me',
      timestamp: '10:32 AM',
      status: 'sent',
    },
    {
      id: '3',
      text: 'All components adapt to the theme automatically! 🎨',
      sender: 'other',
      timestamp: '10:33 AM',
      status: 'sent',
    },
  ]);
  const [isTyping, setIsTyping] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages, scrollToBottom]);

  const getTimestamp = useCallback(() => {
    return new Date().toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
    });
  }, []);

  const handleSendMessage = useCallback(async () => {
    const trimmedMessage = messageInput.trim();

    if (!trimmedMessage) {
      return;
    }

    const newMessage: Message = {
      id: Date.now().toString(),
      text: trimmedMessage,
      sender: 'me',
      timestamp: getTimestamp(),
      status: 'sending',
    };

    setMessages(prev => [...prev, newMessage]);
    setMessageInput('');

    try {
      await new Promise(resolve => setTimeout(resolve, 500));

      setMessages(prev =>
        prev.map(msg =>
          msg.id === newMessage.id ? { ...msg, status: 'sent' as const } : msg
        )
      );

      setTimeout(() => setIsTyping(true), 1000);
      setTimeout(() => {
        setIsTyping(false);
        const responseMessage: Message = {
          id: (Date.now() + 1).toString(),
          text: "That's a great message! 👍",
          sender: 'other',
          timestamp: getTimestamp(),
          status: 'sent',
        };
        setMessages(prev => [...prev, responseMessage]);
      }, 3000);
    } catch (error) {
      console.error('Failed to send message:', error);
      setMessages(prev =>
        prev.map(msg =>
          msg.id === newMessage.id ? { ...msg, status: 'failed' as const } : msg
        )
      );
    }
  }, [messageInput, getTimestamp]);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        handleSendMessage();
      }
    },
    [handleSendMessage]
  );

  const handleInputChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const value = e.target.value;
      if (value.length <= 1000) {
        setMessageInput(value);
      }
    },
    []
  );

  return (
    <div className={`h-full flex flex-col rounded-2xl ${theme.glass} mx-4 p-6`}>
      {/* Header - Fixed */}
      <div className="mb-4 shrink-0">
        <div className="flex items-center gap-3">
          <div className="relative">
            <div className="h-12 w-12 rounded-full bg-linear-to-br from-blue-500 via-purple-500 to-pink-500 flex items-center justify-center text-white font-semibold text-lg shadow-lg shadow-purple-500/50">
              AS
            </div>
          </div>
          <div>
            <h2
              className={`text-base font-semibold ${theme.textPrimary} flex items-center gap-2`}
            >
              Alice Smith
            </h2>
            <div
              className={`rounded text-xs px-2 py-0.5 inline-block transition-all ${theme.button.secondary}`}
            >
              Active
            </div>
          </div>
        </div>
      </div>

      {/* Messages Container - Flexible/Scrollable */}
      <div
        className={`flex-1 rounded-xl ${theme.bgCard} ${theme.border} border p-4 flex flex-col min-h-0`}
      >
        {/* Messages List - Scrollable */}
        <div
          className={`flex-1 mb-4 overflow-y-auto px-4 space-y-4 ${theme.scrollbar}`}
        >
          {messages.map(message => (
            <MessageBubble
              key={message.id}
              message={message}
              isOwn={message.sender === 'me'}
            />
          ))}

          {/* Typing indicator */}
          {isTyping && (
            <div className="flex items-start gap-3">
              <div className={`rounded-2xl ${theme.glass} px-4 py-2`}>
                <div className="flex gap-1">
                  <span className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" />
                  <span
                    className="w-2 h-2 bg-gray-400 rounded-full animate-bounce"
                    style={{ animationDelay: '0.1s' }}
                  />
                  <span
                    className="w-2 h-2 bg-gray-400 rounded-full animate-bounce"
                    style={{ animationDelay: '0.2s' }}
                  />
                </div>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Input Area - Fixed at Bottom */}
        <div className="shrink-0">
          <div className="flex gap-2">
            <button
              onClick={() => {
                // Handle attachment click
                console.log('Attachment button clicked');
              }}
              className={`rounded-full p-2 text-sm font-medium ${theme.glass} ${theme.glassHover} cursor-pointer`}
              title="Attach file"
            >
              <svg
                className="w-5 h-5 text-gray-300"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13"
                />
              </svg>
            </button>
            <input
              ref={inputRef}
              type="text"
              value={messageInput}
              onChange={handleInputChange}
              onKeyDown={handleKeyDown}
              placeholder="Type a message..."
              className={`flex-1 rounded-full px-4 py-2 text-sm outline-none transition-shadow ${theme.input}`}
              maxLength={1000}
            />
            <button
              onClick={handleSendMessage}
              disabled={!messageInput.trim()}
              className={`rounded-full px-6 py-2 text-sm font-medium ${theme.button.primary}`}
            >
              Send
            </button>
          </div>

          {/* Character count */}
          {messageInput.length > 900 && (
            <div className="text-xs text-gray-400 text-right mt-1">
              {messageInput.length}/1000
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
