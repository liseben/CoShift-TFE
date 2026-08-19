import type { ReactElement } from "react";
import { FaStar, FaBolt, FaLeaf, FaGasPump, FaSuitcase, FaDog, FaMusic } from "react-icons/fa";
import { FiArrowRight } from "react-icons/fi";
import { Avatar, Card } from "../ui";
import "./TripCard.css";

export interface TripSummary {
  uuid: string;
  departureCity: string;
  arrivalCity: string;
  departureTime: string;
  availableSeats: number;
  pricePerSeat: number;
  acceptsLuggage?: boolean;
  acceptsPets?: boolean;
  musicAllowed?: boolean;
  driver: {
    firstname: string;
    lastname: string;
    pictureUrl?: string;
    averageRating: number;
  };
  vehicule: { brand: string; model: string; energy: string };
}

const ENERGY: Record<string, { icon: ReactElement; label: string }> = {
  ELECTRIC: { icon: <FaBolt />, label: "Électrique" },
  HYBRID:   { icon: <FaLeaf />, label: "Hybride" },
  GASOLINE: { icon: <FaGasPump />, label: "Essence" },
  DIESEL:   { icon: <FaGasPump />, label: "Diesel" },
  LPG:      { icon: <FaGasPump />, label: "GPL" },
};

export function formatTripMoment(iso: string) {
  const d = new Date(iso);
  return (
    d.toLocaleDateString("fr-FR", { weekday: "long", day: "numeric", month: "long" }) +
    " à " +
    d.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" })
  );
}

/**
 * Carte d'un trajet dans une liste.
 *
 * Partagée par la page d'accueil et la recherche : les deux montraient la
 * même chose, il n'y a aucune raison de l'écrire deux fois.
 *
 * La carte entière est le lien — plus facile à viser au doigt qu'un petit
 * « voir plus », et une seule tabulation au clavier.
 */
export default function TripCard({ trip }: { trip: TripSummary }) {
  const energy = ENERGY[trip.vehicule.energy];
  const name = `${trip.driver.firstname} ${trip.driver.lastname}`;

  return (
    <Card
      to={`/trips/${trip.uuid}`}
      title={
        <span className="tc__route">
          {trip.departureCity}
          <FiArrowRight aria-hidden="true" />
          {trip.arrivalCity}
        </span>
      }
      action={<span className="tc__price">{trip.pricePerSeat.toFixed(2)} €</span>}
    >
      <p className="tc__when">{formatTripMoment(trip.departureTime)}</p>

      <div className="tc__driver">
        <Avatar src={trip.driver.pictureUrl} name={name} size="sm" />
        <div>
          <p className="tc__driver-name">{name}</p>
          <p className="tc__meta">
            {trip.driver.averageRating > 0 ? (
              <>
                <FaStar aria-hidden="true" className="tc__star" />
                {trip.driver.averageRating.toFixed(1)}
              </>
            ) : (
              "Nouveau conducteur"
            )}
            {" · "}
            {trip.vehicule.brand} {trip.vehicule.model}
          </p>
        </div>
      </div>

      <ul className="tc__tags">
        <li className="tc__tag tc__tag--seats">
          {trip.availableSeats} place{trip.availableSeats > 1 ? "s" : ""}
        </li>
        {energy && (
          <li className="tc__tag">
            <span aria-hidden="true">{energy.icon}</span> {energy.label}
          </li>
        )}
        {trip.acceptsLuggage && (
          <li className="tc__tag"><FaSuitcase aria-hidden="true" /> Bagages</li>
        )}
        {trip.acceptsPets && (
          <li className="tc__tag"><FaDog aria-hidden="true" /> Animaux</li>
        )}
        {trip.musicAllowed && (
          <li className="tc__tag"><FaMusic aria-hidden="true" /> Musique</li>
        )}
      </ul>

      <p className="tc__cta" aria-hidden="true">
        Voir le trajet <FiArrowRight />
      </p>
    </Card>
  );
}
