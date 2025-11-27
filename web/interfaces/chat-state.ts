import { ChatRoom } from './chat-room';

export interface ChatState {
  ws: WebSocket | null;
  messages: Message[];
  rooms: ChatRoom[];
  activeRoomId: string | null;
  isConnected: boolean;
  typingUsers: Set<string>;

  // WebSocket actions
  connect: (url: string, token: string) => void;
  disconnect: () => void;

  // Message actions
  sendMessage: (content: string) => void;
  addMessage: (message: Message) => void;
  updateMessageStatus: (id: string, status: Message['status']) => void;

  // Room actions
  setActiveRoom: (roomId: string) => void;
  addRoom: (room: ChatRoom) => void;
  updateUnreadCount: (roomId: string, count: number) => void;

  // Typing indicator
  setUserTyping: (userId: string, isTyping: boolean) => void;
}
