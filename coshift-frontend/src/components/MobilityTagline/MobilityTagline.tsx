import { useState, useEffect } from "react";
import { useT } from "../../context/LangContext";
import "./MobilityTagline.css";

export default function MobilityTagline() {
  const t = useT();
  /* Les mots défilent sur une accroche accordée au féminin en français.
     L'anglais n'accorde pas : chaque mot est une clé, ce qui laisse au
     traducteur la liberté de la tournure. */
  const words = [1, 2, 3, 4, 5].map((i) => t(`banniere.mot${i}`));

  const [index, setIndex] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => {
      setIndex((prev) => (prev + 1) % words.length);
    }, 2500);
    return () => clearInterval(interval);
  }, [words.length]);

  return (
    <div className="mobility-tagline">
      <span>{t("banniere.accroche")} </span>
      <div className="roller-container">
        <div
          className="roller-list"
          style={{
            transform: `translateY(-${index * (100 / words.length)}%)`,
            transition:
              "transform 0.5s cubic-bezier(0.6, -0.28, 0.735, 0.045)" /* Un petit effet ressort fluide */,
          }}
        >
          {words.map((word, i) => (
            <span key={i} className="roller-item">
              {word}
            </span>
          ))}
        </div>
      </div>
    </div>
  );
}
