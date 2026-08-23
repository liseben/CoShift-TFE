import {
  createContext,
  useState,
  useEffect,
  useContext,
} from "react";
import type { ReactNode } from "react";
import axios from "axios";
import { API_BASE } from "../config/api";

// 1. On définit la forme de notre Utilisateur
interface User {
  /** Identifiant public, seul moyen fiable de comparer deux comptes. */
  uuid: string;
  firstname: string;
  lastname: string;
  email: string;
  pictureUrl?: string;
  phoneNumber?: string;
  role: string;
  emailVerified: boolean;
  averageRating: number;
  tripsCount: number;
}

// 2. On définit ce que notre Contexte va fournir à l'application
interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  login: (token: string) => void;
  logout: () => void;
  /**
   * Relit le profil depuis le serveur, jeton inchangé.
   *
   * Le profil transporte des valeurs que d'autres écrans font bouger — le
   * nombre de trajets effectués change dès qu'une prestation est confirmée.
   * Sans ce rappel, l'en-tête du tableau de bord garderait l'ancienne valeur
   * jusqu'à la prochaine connexion, et donnerait l'impression que l'action
   * n'a rien fait.
   */
  rafraichir: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

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

  const rafraichir = () => {
    const token = localStorage.getItem("coshift_token");
    // Sans jeton, il n'y a rien à relire : la session est déjà close.
    if (token) fetchUser(token);
  };

  return (
    <AuthContext.Provider value={{ user, isLoading, login, logout, rafraichir }}>
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
