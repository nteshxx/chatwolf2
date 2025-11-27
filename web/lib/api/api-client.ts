import { useAuthStore } from '@/store/auth.store';

export async function apiClient(endpoint: string, options: RequestInit = {}) {
  const { token, refreshAccessToken, logout } = useAuthStore.getState();

  const config: RequestInit = {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token && { Authorization: `Bearer ${token}` }),
      ...options.headers,
    },
  };

  let response = await fetch(endpoint, config);

  // Handle token expiration
  if (response.status === 401 && token) {
    try {
      await refreshAccessToken();
      const newToken = useAuthStore.getState().token;

      config.headers = {
        ...config.headers,
        Authorization: `Bearer ${newToken}`,
      };

      response = await fetch(endpoint, config);
    } catch (error) {
      logout();
      throw new Error('Session expired. Please login again.');
    }
  }

  return response;
}
