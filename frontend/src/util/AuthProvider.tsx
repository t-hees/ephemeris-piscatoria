import { useCallback, useState, type ReactNode } from "react";
import { fetchApi } from './fetchApi.ts'
import { AuthContext, type CurrentUser } from "./AuthContext.tsx";

export function AuthProvider({children}: {children: ReactNode}) {
  const [userName, setUserName] = useState<string|null>(null);
  // Role only comes from users/me, not from the login response
  const [details, setDetails] = useState<CurrentUser|null>(null);

  const fetchName = useCallback(() => {
    fetchApi("users/me", "GET", async (response) => {
      const user = await response.json() as CurrentUser;
      setUserName(user.username);
      setDetails(user);
    }, (error) => console.error(error), () => {});
  }, []);

  const isAuthenticated = () => {
    return userName != null;
  }

  const clear = () => {
    setUserName(null);
    setDetails(null);
  }

  const setName = (name: string) => {
    setUserName(name);
    fetchName();
  }

  return (
    <AuthContext.Provider value={{name: userName, isAdmin: details?.role === "ADMIN",
      isAuthenticated: isAuthenticated,
      fetchName: fetchName, setName: setName, authClear: clear}}>
      {children}
    </AuthContext.Provider>
  )
}
