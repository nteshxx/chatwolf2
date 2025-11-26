export interface Message {
  id: string
  userId: string
  username: string
  content: string
  timestamp: number
  status?: 'sending' | 'sent' | 'failed'
}