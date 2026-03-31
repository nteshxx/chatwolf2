export interface Message {
  id: string;
  userId: string;
  username: string;
  content: string;
  timestamp: string;
  status?: 'sending' | 'sent' | 'failed';
}
