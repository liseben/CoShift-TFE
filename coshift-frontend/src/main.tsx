import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import "./index.css";
// 1. L'import Google
import { GoogleOAuthProvider } from "@react-oauth/google";

// 2. Ta clé magique
const GOOGLE_CLIENT_ID =
  "415112384949-i0jihhuatgp8hrnuvptqujenhmmn0kb1.apps.googleusercontent.com";

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    {/* 3. On enveloppe l'App */}
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
      <App />
    </GoogleOAuthProvider>
  </React.StrictMode>,
);
