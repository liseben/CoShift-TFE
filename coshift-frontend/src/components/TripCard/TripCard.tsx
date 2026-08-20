import type { ReactElement } from "react";
import { FaStar, FaBolt, FaLeaf, FaGasPump, FaSuitcase, FaDog, FaMusic } from "react-icons/fa";
import { FiArrowRight } from "react-icons/fi";
import { Avatar, Card } from "../ui";
import { useLang } from "../../context/LangContext";
import { LANGUES } from "../../i18n";
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

/* Le libellé a quitté cette table : elle ne porte plus que l'icône, et la clé
   de traduction est le nom de la motorisation lui-même. Un libellé écrit dans
   une constante de module échappe au contexte de langue — il serait figé au
   chargement du fichier, avant même que la langue soit connue. */
const ENERGY: Record<string, ReactElement> = {
  ELECTRIC: <FaBolt />,
  HYBRID:   <FaLeaf />,
  GASOLINE: <FaGasPump />,
  DIESEL:   <FaGasPump />,
  LPG:      <FaGasPump />,
};

export function formatTripMoment(iso: string, balise: string, liaison: string) {
  const d = new Date(iso);
  return (
    d.toLocaleDateString(balise, { weekday: "long", day: "numeric", month: "long" }) +
    liaison +
    d.toLocaleTimeString(balise, { hour: "2-digit", minute: "2-digit" })
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
  const { langue, t } = useLang();
  const icone = ENERGY[trip.vehicule.energy];
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
      <p className="tc__when">
        {formatTripMoment(trip.departureTime, LANGUES[langue].balise, t("carte.aHeure"))}
      </p>

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
              t("carte.nouveauConducteur")
            )}
            {" · "}
            {trip.vehicule.brand} {trip.vehicule.model}
          </p>
        </div>
      </div>

      <ul className="tc__tags">
        <li className="tc__tag tc__tag--seats">
          {trip.availableSeats > 1
            ? t("commun.places_plusieurs", { n: trip.availableSeats })
            : t("commun.places_une", { n: trip.availableSeats })}
        </li>
        {icone && (
          <li className="tc__tag">
            <span aria-hidden="true">{icone}</span>{" "}
            {t(`energie.${trip.vehicule.energy}`)}
          </li>
        )}
        {trip.acceptsLuggage && (
          <li className="tc__tag"><FaSuitcase aria-hidden="true" /> {t("carte.bagages")}</li>
        )}
        {trip.acceptsPets && (
          <li className="tc__tag"><FaDog aria-hidden="true" /> {t("carte.animaux")}</li>
        )}
        {trip.musicAllowed && (
          <li className="tc__tag"><FaMusic aria-hidden="true" /> {t("carte.musique")}</li>
        )}
      </ul>

      <p className="tc__cta" aria-hidden="true">
        {t("carte.voirLeTrajet")} <FiArrowRight />
      </p>
    </Card>
  );
}
