import { useState, useEffect } from "react";
import "./MobilityTagline.css";

export default function MobilityTagline() {
  const words = ["simple", "verte", "intelligente", "rapide", "collaborative"];

  const [index, setIndex] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => {
      setIndex((prev) => (prev + 1) % words.length);
    }, 2500);
    return () => clearInterval(interval);
  }, [words.length]);

  return (
    <div className="mobility-tagline">
      <span>Une mobilité plus </span>
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
