import { useState, type FormEvent } from "react";
import { Button, Input, Select } from "../ui";
import { useT } from "../../context/LangContext";
import "./TripSearchForm.css";

export type TripCriteria = {
  departure: string;
  arrival: string;
  date: string;
  time: string;
  seats: string;
};

export const EMPTY_CRITERIA: TripCriteria = {
  departure: "",
  arrival: "",
  date: "",
  time: "",
  seats: "",
};

type Props = {
  initial?: Partial<TripCriteria>;
  onSubmit: (c: TripCriteria) => void;
  loading?: boolean;
  /** `hero` sur fond d'image, `page` dans le flux d'une page. */
  layout?: "hero" | "page";
};

/**
 * Formulaire de recherche de trajet.
 *
 * Partagé par la page d'accueil et la page de recherche : un seul endroit
 * où corriger un champ ou en ajouter un.
 *
 * L'heure n'est pas envoyée au serveur — `GET /api/trips/search` n'accepte
 * qu'une `LocalDate`. Elle sert de borne basse appliquée aux résultats.
 */
export default function TripSearchForm({
  initial,
  onSubmit,
  loading = false,
  layout = "page",
}: Props) {
  const t = useT();
  const [c, setC] = useState<TripCriteria>({ ...EMPTY_CRITERIA, ...initial });
  const set = (k: keyof TripCriteria) => (v: string) => setC((p) => ({ ...p, [k]: v }));

  const today = new Date().toISOString().slice(0, 10);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    onSubmit(c);
  };

  return (
    <form className={`tsf tsf--${layout}`} onSubmit={submit}>
      <div className="tsf__fields">
        <Input
          label={t("recherche.depart")}
          placeholder={t("recherche.departExemple")}
          value={c.departure}
          onChange={(e) => set("departure")(e.target.value)}
          autoComplete="off"
        />
        <Input
          label={t("recherche.arrivee")}
          placeholder={t("recherche.arriveeExemple")}
          value={c.arrival}
          onChange={(e) => set("arrival")(e.target.value)}
          autoComplete="off"
        />
        <Input
          label={t("recherche.date")}
          type="date"
          min={today}
          value={c.date}
          onChange={(e) => set("date")(e.target.value)}
        />
        <Input
          label={t("recherche.aPartirDe")}
          type="time"
          value={c.time}
          onChange={(e) => set("time")(e.target.value)}
        />
        <Select
          label={t("recherche.places")}
          value={c.seats}
          onChange={(e) => set("seats")(e.target.value)}
          /* Les libellés sont construits à partir des clés de pluriel plutôt
             qu'écrits un à un : « 1 place » et « 1 seat » n'accordent pas de
             la même façon, et une liste figée aurait imposé le pluriel
             français à toutes les langues. */
          options={[
            { value: "", label: t("recherche.peuImporte") },
            ...[1, 2, 3, 4].map((n) => ({
              value: String(n),
              label: n > 1
                ? t("commun.places_plusieurs", { n })
                : t("commun.places_une", { n }),
            })),
          ]}
        />
      </div>

      <Button type="submit" size="lg" loading={loading} className="tsf__submit">
        {t("commun.rechercher")}
      </Button>
    </form>
  );
}
