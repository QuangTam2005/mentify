import { create } from 'zustand';
import type { AuthMeResponse } from '../types/auth.types';

interface AuthState {
  user: AuthMeResponse | null;
  isAuthenticated: boolean;
  isInitialized: boolean;
  setUser: (user: AuthMeResponse | null) => void;
  clearUser: () => void;
  setInitialized: (initialized: boolean) => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  isInitialized: false,
  setUser: (user) =>
    set({
      user,
      isAuthenticated: !!user,
      isInitialized: true,
    }),
  clearUser: () =>
    set({
      user: null,
      isAuthenticated: false,
      isInitialized: true,
    }),
  setInitialized: (initialized) =>
    set({
      isInitialized: initialized,
    }),
}));
