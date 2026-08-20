import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import "./index.css";
import { AuthProvider } from "./context/AuthContext";
import { ThemeProvider } from "./context/ThemeContext";
import { ConsentProvider } from "./context/ConsentContext";
import { LangProvider } from "./context/LangContext";

/**
 * Le fournisseur Google a quitté cet emplacement.
 *
 * <p>Monté ici, `GoogleOAuthProvider` injectait le script Google Identity
 * Services dans toutes les pages du site — accueil comprise — au premier
 * rendu. Un visiteur qui lisait la page « À propos » sans intention de se
 * connecter transmettait donc son adresse IP à Google, sans consentement et
 * sans qu'aucune fonction de la page ne l'exige.</p>
 *
 * <p>Le fournisseur est désormais monté par l'écran de connexion, et
 * seulement lorsque le consentement a été donné. Voir
 * {@link ./components/Consent/GoogleGate}.</p>
 */
ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    {/* La langue enveloppe tout : le bandeau de consentement et les écrans
        d'authentification doivent eux aussi se traduire. */}
    <LangProvider>
      <ThemeProvider>
        <ConsentProvider>
          <AuthProvider>
            <App />
          </AuthProvider>
        </ConsentProvider>
      </ThemeProvider>
    </LangProvider>
  </React.StrictMode>,
);
