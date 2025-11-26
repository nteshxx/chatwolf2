export interface ChatRoom {
  id: string
  name: string
  lastMessage?: Message
  unreadCount: number
}