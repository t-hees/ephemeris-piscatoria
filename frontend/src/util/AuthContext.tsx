import { createContext, useContext } from "react";

export type UserInfo = {
  username: string,
}

export type CurrentUser = UserInfo & {
  role: "USER" | "ADMIN",
}

export type AuthType = {
  name: string|null,
  // Only admins may upload images
  isAdmin: boolean,
  isAuthenticated: () => boolean,
  fetchName: () => void,
  setName: (name: string) => void,
  authClear: () => void
}

export const AuthContext = createContext<AuthType>({
  name: null,
  isAdmin: false,
  isAuthenticated: () => false,
  fetchName: () => {},
  setName: () => {},
  authClear: () => {}
});

export function useAuth(): AuthType {
  const ctx = useContext(AuthContext);
  // if (!ctx) throw new Error("useAuth must be used with an AuthProvider!");
  return ctx;
}
