import { useState } from "react";
import "./Avatar.css";

type Props = {
  /** Photo de profil. En cas d'absence ou d'echec, les initiales prennent le relais. */
  src?: string | null;
  /** Nom complet, utilise pour les initiales et le texte alternatif. */
  name: string;
  size?: "sm" | "md" | "lg" | "xl";
  /** Liseré vert : utilisateur dont l'e-mail est verifie. */
  verified?: boolean;
};

function initials(name: string) {
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((w) => w[0]?.toUpperCase() ?? "")
    .join("");
}

/**
 * Avatar.
 *
 * La photo peut echouer a charger — lien expire, compte Google supprime.
 * On bascule alors sur les initiales plutot que d'afficher une image cassee.
 */
export default function Avatar({ src, name, size = "md", verified = false }: Props) {
  const [failed, setFailed] = useState(false);
  const showImage = src && !failed;

  return (
    <span
      className={`avatar avatar--${size} ${verified ? "avatar--verified" : ""}`.trim()}
      title={name}
    >
      {showImage ? (
        <img src={src} alt={name} onError={() => setFailed(true)} className="avatar__img" />
      ) : (
        <span className="avatar__initials" aria-hidden="true">
          {initials(name)}
        </span>
      )}
      {!showImage && <span className="sr-only">{name}</span>}
    </span>
  );
}
