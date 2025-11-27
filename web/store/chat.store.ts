import { ChatState } from '@/interfaces/chat-state';
import { Message } from '@/interfaces/message';
import { create } from 'zustand';
import { devtools } from 'zustand/middleware';

export const useChatStore = create<ChatState>()(
  devtools(
    (set, get) => ({
      ws: null,
      messages: [],
      rooms: [],
      activeRoomId: null,
      isConnected: false,
      typingUsers: new Set(),

      connect: (url, token) => {
        const ws = new WebSocket(`${url}?token=${token}`);

        ws.onopen = () => {
          console.log('WebSocket connected');
          set({ isConnected: true });
        };

        ws.onmessage = event => {
          const data = JSON.parse(event.data);

          switch (data.type) {
            case 'message':
              get().addMessage(data.payload);
              break;
            case 'typing':
              get().setUserTyping(data.userId, data.isTyping);
              break;
            case 'room_update':
              get().addRoom(data.payload);
              break;
          }
        };

        ws.onerror = error => {
          console.error('WebSocket error:', error);
        };

        ws.onclose = () => {
          console.log('WebSocket disconnected');
          set({ isConnected: false, ws: null });

          // Auto-reconnect after 3 seconds
          setTimeout(() => {
            if (!get().ws) {
              get().connect(url, token);
            }
          }, 3000);
        };

        set({ ws });
      },

      disconnect: () => {
        const { ws } = get();
        if (ws) {
          ws.close();
          set({ ws: null, isConnected: false });
        }
      },

      sendMessage: content => {
        const { ws, activeRoomId } = get();
        if (!ws || !activeRoomId) return;

        const tempMessage: Message = {
          id: `temp-${Date.now()}`,
          userId: 'current-user',
          username: 'You',
          content,
          timestamp: Date.now(),
          status: 'sending',
        };

        set(state => ({
          messages: [...state.messages, tempMessage],
        }));

        ws.send(
          JSON.stringify({
            type: 'message',
            roomId: activeRoomId,
            content,
          })
        );
      },

      addMessage: message => {
        set(state => ({
          messages: [...state.messages, message],
        }));
      },

      updateMessageStatus: (id, status) => {
        set(state => ({
          messages: state.messages.map(msg =>
            msg.id === id ? { ...msg, status } : msg
          ),
        }));
      },

      setActiveRoom: roomId => {
        set({ activeRoomId: roomId });
        get().updateUnreadCount(roomId, 0);
      },

      addRoom: room => {
        set(state => {
          const exists = state.rooms.find(r => r.id === room.id);
          if (exists) {
            return {
              rooms: state.rooms.map(r => (r.id === room.id ? room : r)),
            };
          }
          return { rooms: [...state.rooms, room] };
        });
      },

      updateUnreadCount: (roomId, count) => {
        set(state => ({
          rooms: state.rooms.map(room =>
            room.id === roomId ? { ...room, unreadCount: count } : room
          ),
        }));
      },

      setUserTyping: (userId, isTyping) => {
        set(state => {
          const newTypingUsers = new Set(state.typingUsers);
          if (isTyping) {
            newTypingUsers.add(userId);
          } else {
            newTypingUsers.delete(userId);
          }
          return { typingUsers: newTypingUsers };
        });
      },
    }),
    { name: 'ChatStore' }
  )
);
