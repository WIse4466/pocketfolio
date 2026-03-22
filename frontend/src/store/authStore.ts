import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { User, AuthResponse } from '../types/auth.types';

interface AuthState {
  token: string | null;
  user: User | null;
  isAuthenticated: boolean;
  
  // Actions
  login: (authResponse: AuthResponse) => void;
  logout: () => void;
  setUser: (user: User) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      isAuthenticated: false,

      login: (authResponse: AuthResponse) => {
        const { token, userId, email, displayName } = authResponse;
        const user: User = { id: userId, email, displayName };
        
        // 儲存到 localStorage
        localStorage.setItem('token', token);
        localStorage.setItem('user', JSON.stringify(user));
        
        set({ token, user, isAuthenticated: true });
      },

      logout: () => {
        // 清除 localStorage
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        
        set({ token: null, user: null, isAuthenticated: false });
      },

      setUser: (user: User) => {
        set({ user });
      },
    }),
    {
      name: 'auth-storage',
    }
  )
);