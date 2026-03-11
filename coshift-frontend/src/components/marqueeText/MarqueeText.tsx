import "./MarqueeText.css";

export default function MarqueeText() {
  const phrase = "PRENEZ PART AU CHANGEMENT AVEC COSHIFT — ";

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
