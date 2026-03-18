import React, {
  createContext,
  useState,
  useEffect,
  useContext,
} from "react";
import type { ReactNode } from "react";
import axios from "axios";

// 1. On définit la forme de notre Utilisateur
interface User {
  firstname: string;
  lastname: string;
  email: string;
  pictureUrl?: string;
  role: string;
}

// 2. On définit ce que notre Contexte va fournir à l'application
interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  login: (token: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const API_BASE = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

  // Fonction pour aller chercher le profil avec le Token
  const fetchUser = async (token: string) => {
    try {
      const response = await axios.get(`${API_BASE}/api/users/me`, {
        headers: { Authorization: `Bearer ${token}` }, // ⚠️ On envoie le token ici !
      });
      setUser(response.data);
    } catch (error) {
      console.error("Erreur lors de la récupération du profil :", error);
      logout(); // Si le token est expiré ou invalide, on déconnecte
    } finally {
      setIsLoading(false);
    }
  };

  // Au chargement du site, on vérifie s'il y a déjà un token en mémoire
  useEffect(() => {
    const token = localStorage.getItem("coshift_token");
    if (token) {
      fetchUser(token);
    } else {
      setIsLoading(false);
    }
  }, []);

  const login = (token: string) => {
    localStorage.setItem("coshift_token", token);
    fetchUser(token);
  };

  const logout = () => {
    localStorage.removeItem("coshift_token");
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, isLoading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

// Petit Hook personnalisé (raccourci) pour utiliser le contexte facilement
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error(
      "useAuth doit être utilisé à l'intérieur d'un AuthProvider",
    );
  }
  return context;
};
