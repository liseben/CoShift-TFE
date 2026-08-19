import { useState } from "react";
import {
  Alert, Avatar, Button, Card, EmptyState, Input, Modal,
  Select, Spinner, StatusBadge, Textarea,
} from "../../components/ui";
import Logo from "../../components/Logo/Logo";
import "./StyleguidePage.css";

/**
 * Planche des composants CoShift.
 *
 * Sert de reference vivante : chaque composant y est montre dans ses
 * variantes et ses etats. Toute regression visuelle se voit ici d'abord.
 */
export default function StyleguidePage() {
  const [modalOpen, setModalOpen] = useState(false);
  const [note, setNote] = useState("");

  return (
    <div className="container page sg">
      <header className="sg__header">
        <Logo size={44} />
        <p className="sg__intro">
          Socle de composants. Chaque couleur porte une information :
          le bleu la route, le vert le partage, l'ambre l'attente,
          le rouge la rupture.
        </p>
      </header>

      <section className="sg__section">
        <h2>Boutons</h2>
        <div className="sg__row">
          <Button>Principal</Button>
          <Button variant="eco">Réserver</Button>
          <Button variant="secondary">Secondaire</Button>
          <Button variant="ghost">Discret</Button>
          <Button variant="danger">Annuler</Button>
        </div>
        <div className="sg__row">
          <Button size="sm">Petit</Button>
          <Button size="md">Moyen</Button>
          <Button size="lg">Grand</Button>
          <Button loading>Chargement</Button>
          <Button disabled>Désactivé</Button>
        </div>
      </section>

      <section className="sg__section">
        <h2>Statuts</h2>
        <div className="sg__row">
          <StatusBadge status="PENDING" />
          <StatusBadge status="ACCEPTED" />
          <StatusBadge status="REJECTED" />
          <StatusBadge status="CANCELLED" />
          <StatusBadge status="PLANNED" />
          <StatusBadge status="COMPLETED" />
          <StatusBadge status="FULL" />
        </div>
      </section>

      <section className="sg__section">
        <h2>Champs</h2>
        <div className="sg__grid">
          <Input label="Ville de départ" placeholder="Namur" required />
          <Input label="E-mail" type="email" error="Adresse invalide" defaultValue="abc@" />
          <Input label="Téléphone" hint="Visible par vos passagers uniquement" />
          <Select
            label="Type d'énergie"
            placeholder="Choisir…"
            defaultValue=""
            options={[
              { value: "ELECTRIC", label: "Électrique" },
              { value: "DIESEL", label: "Diesel" },
              { value: "LPG", label: "GPL" },
            ]}
          />
        </div>
        <Textarea
          label="Précisions sur le trajet"
          hint="Point de rendez-vous, bagages acceptés…"
          maxLength={280}
          showCount
          value={note}
          onChange={(e) => setNote(e.target.value)}
        />
      </section>

      <section className="sg__section">
        <h2>Cartes et avatars</h2>
        <div className="grid-auto">
          <Card title="Namur → Bruxelles" action={<StatusBadge status="PLANNED" size="sm" />} tone="brand">
            <div className="sg__trip">
              <Avatar name="Élisabeth Kileba" verified />
              <div>
                <p className="sg__driver">Élisabeth K.</p>
                <p className="sg__meta">Départ 07 h 45 · 3 places</p>
              </div>
              <p className="sg__price">4,50 €</p>
            </div>
          </Card>
          <Card title="Réservation acceptée" action={<StatusBadge status="ACCEPTED" size="sm" />} tone="eco">
            <p className="sg__meta">Votre place est confirmée pour demain.</p>
          </Card>
          <Card title="Demande en attente" action={<StatusBadge status="PENDING" size="sm" />} tone="pending">
            <p className="sg__meta">Le conducteur n'a pas encore répondu.</p>
          </Card>
        </div>
        <div className="sg__row">
          <Avatar name="Ana Lopez" size="sm" />
          <Avatar name="Bruno Tshimanga" size="md" />
          <Avatar name="Chloé Martin" size="lg" verified />
          <Avatar name="David N" size="xl" />
        </div>
      </section>

      <section className="sg__section">
        <h2>Messages</h2>
        <div className="stack">
          <Alert tone="info">Votre trajet part dans 30 minutes.</Alert>
          <Alert tone="success" title="Réservation confirmée">Vous économisez 3,2 kg de CO₂.</Alert>
          <Alert tone="warning" title="E-mail non vérifié">Vérifiez votre adresse pour réserver.</Alert>
          <Alert tone="danger" title="Trajet annulé">Le conducteur a annulé ce trajet.</Alert>
        </div>
      </section>

      <section className="sg__section">
        <h2>Chargement, vide, modale</h2>
        <div className="sg__row">
          <Spinner size="sm" />
          <Spinner size="md" />
          <Spinner size="lg" showLabel />
        </div>
        <EmptyState
          icon="◎"
          title="Aucun trajet pour cette recherche"
          description="Élargissez la plage horaire ou créez une alerte pour être prévenu."
          action={<Button variant="secondary">Modifier la recherche</Button>}
        />
        <Button onClick={() => setModalOpen(true)}>Ouvrir la modale</Button>
        <Modal
          open={modalOpen}
          onClose={() => setModalOpen(false)}
          title="Annuler cette réservation ?"
          footer={
            <>
              <Button variant="ghost" onClick={() => setModalOpen(false)}>Retour</Button>
              <Button variant="danger" onClick={() => setModalOpen(false)}>Confirmer</Button>
            </>
          }
        >
          <p>
            Le conducteur sera prévenu. Cette action est définitive et la place
            sera remise à disposition.
          </p>
        </Modal>
      </section>
    </div>
  );
}
