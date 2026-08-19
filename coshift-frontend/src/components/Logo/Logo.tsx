import "./Logo.css";

type LogoProps = {
  /** `full` affiche la marque et le mot ; `mark` la marque seule. */
  variant?: "full" | "mark";
  /** Hauteur de la marque en pixels. Le mot suit proportionnellement. */
  size?: number;
  /** Sur fond sombre, les tracés passent aux teintes claires. */
  onDark?: boolean;
  className?: string;
};

/**
 * Logo CoShift.
 *
 * Deux trajets distincts convergent en un seul : c'est le covoiturage.
 * Le tracé commun est vert parce qu'il porte le gain — une voiture de moins.
 *
 * Le SVG est inline plutôt qu'importé en <img> pour que les couleurs
 * s'adaptent au fond et que le titre reste lisible par les lecteurs d'écran.
 */
export default function Logo({
  variant = "full",
  size = 32,
  onDark = false,
  className = "",
}: LogoProps) {
  const trace = onDark ? "var(--brand-on-dark)" : "var(--brand)";
  const gain = onDark ? "var(--eco-on-dark)" : "var(--eco)";

  return (
    <span className={`logo ${className}`.trim()}>
      <svg
        viewBox="0 0 48 48"
        width={size}
        height={size}
        fill="none"
        strokeWidth={4.5}
        strokeLinecap="round"
        strokeLinejoin="round"
        role="img"
        aria-label={variant === "mark" ? "CoShift" : undefined}
        aria-hidden={variant === "full" || undefined}
        focusable="false"
      >
        <path d="M8 11 C 19 11, 21 24, 29 24" stroke={trace} />
        <path d="M8 37 C 19 37, 21 24, 29 24" stroke={trace} opacity={0.55} />
        <path d="M29 24 H 41" stroke={gain} />
        <path d="M35.5 18.5 L 41 24 L 35.5 29.5" stroke={gain} />
        <circle cx="8" cy="11" r="3.2" fill={trace} stroke="none" />
        <circle cx="8" cy="37" r="3.2" fill={trace} stroke="none" opacity={0.55} />
      </svg>

      {variant === "full" && (
        <span className="logo__wordmark" style={{ fontSize: size * 0.72 }}>
          <span className="logo__co">Co</span>Shift
        </span>
      )}
    </span>
  );
}
