import { useT } from "../../context/LangContext";
import "./MarqueeText.css";

export default function MarqueeText() {
  const t = useT();
  /* Le tiret cadratin sépare les répétitions du défilement : il appartient à
     la mise en forme, pas au texte, et n'a donc pas sa place au catalogue. */
  const phrase = `${t("banniere.defilante")} — `;

  return (
    <div className="marquee-wrapper">
      <div className="marquee-container">
        <div className="marquee-content">
          {/* On augmente la répétition pour la fluidité à basse vitesse */}
          <span>{phrase.repeat(30)}</span>
        </div>
      </div>
    </div>
  );
}
