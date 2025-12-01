export interface Message {
  id: string;
  sender: string;
  text: string;
  timestamp: string;
  status?: 'sending' | 'sent' | 'failed';
}
