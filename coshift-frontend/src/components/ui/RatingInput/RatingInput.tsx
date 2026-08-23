import { useId, useState } from "react";
import { FaStar } from "react-icons/fa";
import "./RatingInput.css";

interface Props {
  label: string;
  value: number;
  onChange: (note: number) => void;
  /** Libellé de chaque note, du 1 au 5, lu par les technologies d'assistance. */
  libelles: readonly [string, string, string, string, string];
  error?: string;
  disabled?: boolean;
}

const NOTES = [1, 2, 3, 4, 5] as const;

/**
 * Sélection d'une note de 1 à 5.
 *
 * <h2>Pourquoi des boutons radio et non des boutons cliquables</h2>
 *
 * Une rangée d'étoiles est un choix unique parmi cinq : c'est exactement ce que
 * décrit un groupe de boutons radio. En construire un vrai — avec `fieldset`,
 * `legend` et cinq `input[type=radio]` visuellement masqués — donne
 * gratuitement la navigation aux flèches, l'annonce « 3 sur 5 » par les
 * lecteurs d'écran, et la participation à la soumission du formulaire. Une
 * rangée de `<button>` avec des `aria-*` posés à la main imiterait tout cela,
 * moins bien.
 *
 * <h2>L'aperçu au survol ne modifie pas la valeur</h2>
 *
 * Survoler la quatrième étoile allume les quatre premières, sans rien
 * sélectionner. L'état de survol est donc séparé de la valeur : les confondre
 * ferait qu'un simple passage de souris changerait la note, ce qui est le
 * défaut classique de ce composant.
 */
export default function RatingInput({
  label, value, onChange, libelles, error, disabled = false,
}: Props) {
  const groupId = useId();
  const errorId = `${groupId}-error`;
  const [survol, setSurvol] = useState(0);

  /* La valeur affichée suit le survol s'il y en a un, la sélection sinon. */
  const allumees = survol || value;

  return (
    <fieldset
      className={`rating ${error ? "rating--invalid" : ""}`.trim()}
      disabled={disabled}
      aria-describedby={error ? errorId : undefined}
      onMouseLeave={() => setSurvol(0)}
    >
      <legend className="field__label">{label}</legend>

      <div className="rating__etoiles">
        {NOTES.map((note) => (
          <label
            key={note}
            className={`rating__etoile ${note <= allumees ? "is-active" : ""}`.trim()}
            onMouseEnter={() => setSurvol(note)}
          >
            <input
              type="radio"
              name={groupId}
              value={note}
              checked={value === note}
              onChange={() => onChange(note)}
              className="rating__radio"
            />
            {/* Le libellé textuel n'est visible que des lecteurs d'écran : une
                étoile seule ne dit pas ce qu'elle vaut. */}
            <span className="sr-only">{libelles[note - 1]}</span>
            <FaStar aria-hidden="true" />
          </label>
        ))}
      </div>

      {error && (
        <p className="field__error" id={errorId} role="alert">
          {error}
        </p>
      )}
    </fieldset>
  );
}
