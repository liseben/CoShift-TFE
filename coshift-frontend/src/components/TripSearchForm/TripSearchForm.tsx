import { useState, type FormEvent } from "react";
import { Button, Input, Select } from "../ui";
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
          label="Départ"
          placeholder="Namur"
          value={c.departure}
          onChange={(e) => set("departure")(e.target.value)}
          autoComplete="off"
        />
        <Input
          label="Arrivée"
          placeholder="Bruxelles"
          value={c.arrival}
          onChange={(e) => set("arrival")(e.target.value)}
          autoComplete="off"
        />
        <Input
          label="Date"
          type="date"
          min={today}
          value={c.date}
          onChange={(e) => set("date")(e.target.value)}
        />
        <Input
          label="À partir de"
          type="time"
          value={c.time}
          onChange={(e) => set("time")(e.target.value)}
        />
        <Select
          label="Places"
          value={c.seats}
          onChange={(e) => set("seats")(e.target.value)}
          options={[
            { value: "", label: "Peu importe" },
            { value: "1", label: "1 place" },
            { value: "2", label: "2 places" },
            { value: "3", label: "3 places" },
            { value: "4", label: "4 places" },
          ]}
        />
      </div>

      <Button type="submit" size="lg" loading={loading} className="tsf__submit">
        Rechercher
      </Button>
    </form>
  );
}
