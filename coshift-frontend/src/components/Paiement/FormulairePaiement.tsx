import { useMemo, useState, type FormEvent } from "react";
import { Elements, PaymentElement, useElements, useStripe } from "@stripe/react-stripe-js";
import { loadStripe, type Stripe } from "@stripe/stripe-js";
import { Alert, Button } from "../ui";
import { useTheme } from "../../context/ThemeContext";
import { useT } from "../../context/LangContext";

/**
 * Chargement de Stripe.js, une seule fois pour toute l'application.
 *
 * <p>`loadStripe` insère un script tiers dans la page. L'appeler au rendu d'un
 * composant le rejouerait à chaque ouverture du formulaire ; il est donc hors
 * du composant, résolu une fois.</p>
 *
 * <p>Sans clé publique configurée, la promesse vaut `null` : le formulaire ne
 * s'affiche pas et l'écran le dit, plutôt que de charger un script qui
 * échouerait silencieusement.</p>
 */
const CLE_PUBLIQUE = import.meta.env.VITE_STRIPE_PUBLIC_KEY as string | undefined;
const stripePromise: Promise<Stripe | null> | null =
  CLE_PUBLIQUE && !CLE_PUBLIQUE.includes("REMPLACE") ? loadStripe(CLE_PUBLIQUE) : null;

/**
 * Formulaire de paiement par carte.
 *
 * <h2>Les coordonnées bancaires ne passent pas par CoShift</h2>
 *
 * <p>C'est la raison d'être de ce composant. Le champ affiché appartient à
 * Stripe : il est rendu dans un cadre isolé, servi par leurs serveurs. Le
 * numéro de carte n'entre jamais dans le code de CoShift, ne part jamais vers
 * son serveur, et ne peut donc pas fuiter d'une base compromise. Écrire nous-
 * mêmes trois champs `input` serait plus simple, et nous rendrait responsables
 * de données que nous ne savons pas protéger.</p>
 *
 * <h2>Ce que ce composant ne décide pas</h2>
 *
 * <p>Il ne déclare rien payé. Il confirme auprès de Stripe, puis prévient le
 * parent, qui demande au <em>serveur</em> de vérifier. Un écran ne peut pas
 * établir qu'un paiement a eu lieu : il est entre les mains de la personne qui
 * paie.</p>
 */
function Champs({ onRegle, onErreur }: { onRegle: () => void; onErreur: (m: string) => void }) {
  const stripe = useStripe();
  const elements = useElements();
  const t = useT();
  const [envoi, setEnvoi] = useState(false);

  const soumettre = async (e: FormEvent) => {
    e.preventDefault();
    if (!stripe || !elements) return;

    setEnvoi(true);
    const { error } = await stripe.confirmPayment({
      elements,
      /* `if_required` évite une redirection pour une carte qui n'en demande
         pas. Les moyens qui l'exigent vraiment — certaines authentifications
         bancaires — redirigent quand même ; c'est Stripe qui décide, pas nous. */
      redirect: "if_required",
    });
    setEnvoi(false);

    if (error) {
      /* Le message de Stripe est traduit et destiné au porteur de la carte
         (« votre carte a été refusée ») : le remplacer par un texte générique
         lui retirerait la seule information utile. */
      onErreur(error.message ?? t("commun.erreurGenerique"));
      return;
    }
    onRegle();
  };

  return (
    <form onSubmit={soumettre} className="stack-6">
      <PaymentElement />
      <Button type="submit" loading={envoi} disabled={!stripe} block>
        {t("paiement.confirmerCarte")}
      </Button>
    </form>
  );
}

export default function FormulairePaiement({
  secretClient,
  onRegle,
}: {
  secretClient: string;
  onRegle: () => void;
}) {
  const { theme } = useTheme();
  const t = useT();
  /* L'erreur est rangée avec le secret auquel elle se rapporte. Deux états
     séparés obligeraient à l'effacer dans un effet à chaque nouveau secret,
     ce qui déclenche une cascade de rendus — et l'analyse statique le refuse
     à juste titre. Ici, une erreur qui ne correspond plus au secret courant
     cesse simplement d'être affichée. */
  const [erreurDe, setErreurDe] = useState<{ secret: string; message: string } | null>(null);
  const erreur = erreurDe?.secret === secretClient ? erreurDe.message : null;

  /* Le cadre de Stripe ne suit pas la feuille de style de la page : il faut lui
     passer l'apparence. Sans cela, un champ blanc apparaît au milieu d'une
     interface sombre. */
  const options = useMemo(
    () => ({
      clientSecret: secretClient,
      appearance: { theme: (theme === "dark" ? "night" : "stripe") as "night" | "stripe" },
    }),
    [secretClient, theme],
  );

  if (!stripePromise) {
    return <Alert tone="warning">{t("paiement.stripeAbsent")}</Alert>;
  }

  return (
    <div className="stack-6">
      <p className="paiement__essai">{t("paiement.carteEssai")}</p>
      {erreur && <Alert tone="danger" onDismiss={() => setErreurDe(null)}>{erreur}</Alert>}
      <Elements stripe={stripePromise} options={options}>
        <Champs onRegle={onRegle} onErreur={(m) => setErreurDe({ secret: secretClient, message: m })} />
      </Elements>
    </div>
  );
}
