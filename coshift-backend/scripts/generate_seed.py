#!/usr/bin/env python3
"""
Génère la migration Flyway de données de test de CoShift.

    python scripts/generate_seed.py

Écrit `src/main/resources/db/migration/V3__Seed_test_data.sql`.

Pourquoi un générateur plutôt qu'un fichier SQL écrit à la main : le jeu
compte plus de 800 lignes réparties sur sept tables, dont les valeurs
doivent rester cohérentes entre elles (le prix total d'une réservation
découle du prix au siège, une réservation confirmée ne dépasse jamais le
nombre de places, un passager ne réserve pas son propre trajet). Ces
invariants se vérifient en code, pas à l'œil.

Le tirage est déterministe (SEED fixé) : deux exécutions produisent le
même fichier, donc un diff lisible.
"""

import random
import unicodedata
from datetime import date, datetime, timedelta
from pathlib import Path

SEED = 20260819
random.seed(SEED)

# Référence temporelle fixe : sans elle, le fichier changerait à chaque
# exécution et le diff deviendrait illisible.
TODAY = date(2026, 8, 19)

# Mot de passe de test, identique pour tous les comptes générés.
# Empreinte BCrypt (force 10), vérifiée contre BCryptPasswordEncoder.
PASSWORD_PLAIN = "CoShift2026!"
PASSWORD_HASH = "$2b$10$zThsISGT7vVVZYemf2tDT.lF5dzhRbbn8NG3Z58AIvez7UXDKdThq"

N_USERS = 120
N_VEHICLES = 112
N_TRIPS = 150
N_ARTICLES = 100

# Décalage appliqué à tous les identifiants du jeu de test.
#
# Une base de développement contient presque toujours quelques lignes créées
# à la main pendant les essais, qui occupent les identifiants 1, 2, 3… Insérer
# le jeu à partir de 1 provoque alors une violation de clé primaire, et Flyway
# marque la migration en échec — l'application refuse ensuite de démarrer.
#
# En partant de 1001, le jeu cohabite avec les données existantes au lieu de
# leur rentrer dedans. L'AUTO_INCREMENT reprend au-dessus après insertion.
ID_OFFSET = 1000

# ─────────────────────────────────────────────────────────────────────────────
#  Vocabulaire de base
# ─────────────────────────────────────────────────────────────────────────────

PRENOMS = [
    "Élise", "Mathieu", "Camille", "Nicolas", "Sarah", "Julien", "Manon", "Thomas",
    "Léa", "Antoine", "Clara", "Maxime", "Chloé", "Romain", "Emma", "Benoît",
    "Julie", "Guillaume", "Marine", "Sébastien", "Alice", "Vincent", "Laura",
    "Damien", "Céline", "Olivier", "Aurélie", "Pierre", "Charlotte", "Nathan",
    "Amandine", "Grégoire", "Justine", "Xavier", "Noémie", "Adrien", "Sophie",
    "Quentin", "Élodie", "Simon", "Lucie", "Arnaud", "Pauline", "Florian",
    "Margaux", "Loïc", "Anaïs", "Cédric", "Fanny", "Jérôme", "Mélanie",
    "Bastien", "Ophélie", "Hugo", "Delphine", "Kevin", "Salomé", "Michaël",
    "Inès", "Corentin", "Aïcha", "Youssef", "Fatima", "Karim", "Nadia", "Samir",
    "Lina", "Mehdi", "Zoé", "Tanguy", "Marion", "Alexis", "Coralie", "Baptiste",
]

NOMS = [
    "Dubois", "Lambert", "Martin", "Dupont", "Simon", "Laurent", "Leroy",
    "Michel", "Lefèvre", "Moreau", "Girard", "Bonnet", "Dumont", "Fontaine",
    "Rousseau", "Vincent", "Muller", "Lefebvre", "Faure", "Andre", "Mercier",
    "Blanc", "Guerin", "Boyer", "Garnier", "Chevalier", "François", "Legrand",
    "Gauthier", "Garcia", "Perrin", "Robin", "Clement", "Morin", "Nicolas",
    "Henry", "Roussel", "Mathieu", "Gautier", "Masson", "Marchand", "Duval",
    "Denis", "Dumas", "Marie", "Lemaire", "Noel", "Meyer", "Dufour", "Meunier",
    "Brun", "Blanchard", "Giraud", "Joly", "Rivière", "Lucas", "Brunet",
    "Gaillard", "Barbier", "Arnaud", "Martinez", "Gerard", "Roche", "Renard",
    "Schmitt", "Roy", "Leroux", "Colin", "Vidal", "Caron", "Picard", "Roger",
    "Fabre", "Aubert", "Lemoine", "Renaud", "Dumas", "Lacroix", "Olivier",
    "Philippe", "Bourgeois", "Pierre", "Benoit", "Rey", "Leclerc", "Payet",
    "Rolland", "Leclercq", "Guillaume", "Lecomte", "Lopez", "Jean", "Dupuy",
    "Guillot", "Hubert", "Berger", "Carpentier", "Sanchez", "Dupuis", "Moulin",
]

# Villes belges réelles, avec leur position approximative en kilomètres sur un
# repère plan. Sert à calculer une distance, donc un prix crédible.
VILLES = {
    "Bruxelles":   (0, 0),      "Namur":       (35, -45),
    "Liège":       (85, -15),   "Charleroi":   (10, -55),
    "Mons":        (-40, -55),  "Louvain-la-Neuve": (22, -25),
    "Wavre":       (20, -15),   "Nivelles":    (5, -30),
    "Gembloux":    (28, -38),   "Ottignies":   (20, -23),
    "Anvers":      (10, 45),    "Gand":        (-45, 25),
    "Bruges":      (-80, 45),   "Louvain":     (25, 8),
    "Hasselt":     (60, 25),    "Tournai":     (-75, -45),
    "Verviers":    (105, -10),  "Arlon":       (110, -140),
    "Marche-en-Famenne": (75, -75), "Dinant":  (45, -70),
    "Huy":         (60, -35),   "Waterloo":    (8, -18),
    "Braine-l'Alleud": (2, -20), "Ath":        (-50, -40),
    "Soignies":    (-25, -45),  "La Louvière": (-10, -50),
}

# Marques et modèles réels, avec le nombre de places et les motorisations
# effectivement proposées sur ces modèles.
VOITURES = [
    ("Renault", "Clio", 5, ["GASOLINE", "DIESEL", "HYBRID"]),
    ("Renault", "Mégane", 5, ["DIESEL", "ELECTRIC", "HYBRID"]),
    ("Renault", "Scénic", 5, ["DIESEL", "HYBRID"]),
    ("Peugeot", "208", 5, ["GASOLINE", "ELECTRIC"]),
    ("Peugeot", "308", 5, ["DIESEL", "HYBRID"]),
    ("Peugeot", "3008", 5, ["DIESEL", "HYBRID"]),
    ("Peugeot", "5008", 7, ["DIESEL"]),
    ("Citroën", "C3", 5, ["GASOLINE", "DIESEL"]),
    ("Citroën", "C4", 5, ["GASOLINE", "ELECTRIC"]),
    ("Volkswagen", "Golf", 5, ["GASOLINE", "DIESEL", "HYBRID"]),
    ("Volkswagen", "Polo", 5, ["GASOLINE"]),
    ("Volkswagen", "ID.3", 5, ["ELECTRIC"]),
    ("Volkswagen", "Passat", 5, ["DIESEL", "HYBRID"]),
    ("Volkswagen", "Touran", 7, ["DIESEL"]),
    ("Toyota", "Yaris", 5, ["HYBRID"]),
    ("Toyota", "Corolla", 5, ["HYBRID"]),
    ("Toyota", "RAV4", 5, ["HYBRID"]),
    ("Škoda", "Octavia", 5, ["DIESEL", "GASOLINE"]),
    ("Škoda", "Fabia", 5, ["GASOLINE"]),
    ("Škoda", "Kodiaq", 7, ["DIESEL"]),
    ("Opel", "Corsa", 5, ["GASOLINE", "ELECTRIC"]),
    ("Opel", "Astra", 5, ["DIESEL", "HYBRID"]),
    ("Ford", "Focus", 5, ["GASOLINE", "DIESEL"]),
    ("Ford", "Puma", 5, ["GASOLINE", "HYBRID"]),
    ("BMW", "Série 1", 5, ["GASOLINE", "DIESEL"]),
    ("BMW", "i4", 5, ["ELECTRIC"]),
    ("Audi", "A3", 5, ["DIESEL", "HYBRID"]),
    ("Audi", "Q4 e-tron", 5, ["ELECTRIC"]),
    ("Mercedes-Benz", "Classe A", 5, ["DIESEL", "HYBRID"]),
    ("Dacia", "Sandero", 5, ["GASOLINE", "LPG"]),
    ("Dacia", "Duster", 5, ["GASOLINE", "LPG", "DIESEL"]),
    ("Dacia", "Jogger", 7, ["GASOLINE", "LPG"]),
    ("Fiat", "500", 4, ["GASOLINE", "ELECTRIC"]),
    ("Hyundai", "i30", 5, ["GASOLINE", "DIESEL"]),
    ("Hyundai", "Kona", 5, ["ELECTRIC", "HYBRID"]),
    ("Kia", "Niro", 5, ["ELECTRIC", "HYBRID"]),
    ("Tesla", "Model 3", 5, ["ELECTRIC"]),
    ("Volvo", "V40", 5, ["DIESEL"]),
    ("Nissan", "Qashqai", 5, ["GASOLINE", "HYBRID"]),
    ("Seat", "Leon", 5, ["GASOLINE", "DIESEL"]),
]

# Organisations fictives, volontairement : nommer de vraies entreprises
# laisserait croire à un partenariat commercial qui n'existe pas.
ORGANISATIONS = [
    ("Solvantis Belgium", "solvantis", "Entreprise"),
    ("Haute École du Condroz", "he-condroz", "Université"),
    ("Novaris Technologies", "novaris", "Entreprise"),
    ("Université de Basse-Meuse", "u-basse-meuse", "Université"),
    ("Ardenn'Son Festival", "ardenn-son", "Festival"),
    ("Groupe Verhaegen", "verhaegen", "Entreprise"),
    ("Institut Sainte-Gertrude", "sainte-gertrude", "Université"),
    ("Batiplus Construct", "batiplus", "Entreprise"),
    ("Salon Mobilité Wallonie", "salon-mobilite", "Salon"),
    ("Clinique du Val Vert", "val-vert", "Entreprise"),
    ("TechnoCampus Hainaut", "technocampus", "Université"),
    ("Meridian Consulting", "meridian", "Entreprise"),
]

DESCRIPTIONS = [
    "Départ ponctuel, je pars du parking derrière la gare.",
    "Trajet habituel, je fais l'aller-retour tous les jours de semaine.",
    "Je peux faire un détour de dix minutes maximum si c'est sur la route.",
    "Coffre déjà à moitié plein, prévoyez un petit sac seulement.",
    "Non-fumeur, merci de le respecter.",
    "Je prends l'autoroute, comptez large en cas de bouchons le matin.",
    "Rendez-vous au P+R, c'est plus simple que dans le centre.",
    "Je peux déposer en chemin sur demande, dites-le avant le départ.",
    "Trajet calme, j'écoute la radio à faible volume.",
    "J'ai un siège enfant à l'arrière, donc deux places libres seulement.",
    "Merci de confirmer la veille, je pars tôt.",
    "Possibilité de charger un vélo pliant dans le coffre.",
    "Je m'arrête cinq minutes sur l'aire d'autoroute si le trajet est long.",
    "Départ depuis le parking de l'entreprise, badge nécessaire pour entrer.",
    None,
    None,
]

# Sujets d'actualité mobilité. Les intitulés sont rédigés pour ce jeu de test.
#
# La catégorie n'est pas libre : ArticleService.classifyArticle() n'en produit
# que quatre — mobilite, ecologie, entreprises, technologie — et ce sont les
# seules que le filtre de la page Actus sait interroger. En inventer d'autres
# donnerait des articles invisibles dans l'interface.
ARTICLE_SUJETS = [
    ("Le covoiturage domicile-travail progresse de {n} % en Wallonie", "mobilite"),
    ("Les entreprises de plus de {n} salariés devront publier un plan de mobilité", "entreprises"),
    ("Une bande de covoiturage testée sur l'axe {a} — {b}", "mobilite"),
    ("{a} : le parking de délestage double sa capacité", "mobilite"),
    ("Prime de {n} euros par mois pour les navetteurs qui partagent leur voiture", "entreprises"),
    ("Le trafic aux heures de pointe recule de {n} % autour de {a}", "mobilite"),
    ("Les bornes de recharge se multiplient sur les parkings d'entreprise", "technologie"),
    ("Voiture électrique : l'autonomie réelle reste le premier frein à l'achat", "technologie"),
    ("{a} déploie {n} nouvelles places de stationnement partagé", "mobilite"),
    ("Mobilité douce : le vélo gagne du terrain sur les trajets de moins de 5 km", "ecologie"),
    ("Le budget mobilité séduit {n} % des employeurs interrogés", "entreprises"),
    ("Télétravail et covoiturage : les deux leviers qui font baisser les émissions", "ecologie"),
    ("Un employeur sur {n} finance désormais l'abonnement de ses salariés", "entreprises"),
    ("Les navetteurs de {a} passent en moyenne {n} minutes dans les bouchons", "mobilite"),
    ("Autopartage : le parc s'étoffe dans les villes moyennes", "technologie"),
    ("Les campus universitaires repensent l'accès en voiture", "entreprises"),
    ("Émissions du transport : l'objectif {n} reste hors d'atteinte sans report modal", "ecologie"),
    ("Comment les festivals organisent le retour des festivaliers sans voiture", "ecologie"),
    ("Zone de basses émissions : ce qui change pour les navetteurs", "ecologie"),
    ("{a} — {b} : la liaison ferroviaire renforcée aux heures de pointe", "mobilite"),
]

# Les quatre seules valeurs acceptées, reprises de ArticleService.
CATEGORIES_VALIDES = {"mobilite", "ecologie", "entreprises", "technologie"}

SOURCES = [
    "Bulletin Mobilité", "Le Navetteur", "Transport & Territoires",
    "Revue des Déplacements", "Observatoire Mobilité", "Cahiers du Transport",
]

VOIES_DEP = ["Place", "Rue", "Avenue", "Chaussée"]
LIEUX_DEP = ["de la Gare", "du Marché", "des Tilleuls", "Saint-Jacques", "de l'Industrie"]
VOIES_ARR = ["Parking", "Boulevard", "Rue", "Avenue"]
LIEUX_ARR = ["Central", "du Parc", "de l'Europe", "des Écoles", "du Nord"]


# ─────────────────────────────────────────────────────────────────────────────
#  Utilitaires
# ─────────────────────────────────────────────────────────────────────────────

def sql_str(v):
    """Littéral SQL, avec échappement des apostrophes."""
    if v is None:
        return "NULL"
    return "'" + str(v).replace("\\", "\\\\").replace("'", "''") + "'"


def slugify(text):
    n = unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode()
    return "".join(c if c.isalnum() else "-" for c in n.lower()).strip("-")


def uuid_for(prefix, i):
    """UUID stable et lisible : on doit pouvoir retrouver une ligne à l'œil."""
    return f"{prefix}-{i:04d}-0000-4000-8000-{i:012d}"


def distance(a, b):
    (xa, ya), (xb, yb) = VILLES[a], VILLES[b]
    return ((xa - xb) ** 2 + (ya - yb) ** 2) ** 0.5


def dt(d, h, m):
    return datetime(d.year, d.month, d.day, h, m).strftime("%Y-%m-%d %H:%M:%S")


# ─────────────────────────────────────────────────────────────────────────────
#  Génération
# ─────────────────────────────────────────────────────────────────────────────

def build():
    orgs, users, vehicles, trips, bookings, members, articles = [], [], [], [], [], [], []

    # ── Organisations ──
    for i, (name, slug, kind) in enumerate(ORGANISATIONS, start=1):
        orgs.append({
            "id": ID_OFFSET + i, "uuid": uuid_for("0a1", i), "name": name, "slug": slug,
            "kind": kind, "domain": f"{slug}.be",
            "created": dt(TODAY - timedelta(days=random.randint(400, 900)), 9, 0),
        })

    # ── Utilisateurs ──
    used_emails = set()
    for i in range(1, N_USERS + 1):
        prenom, nom = random.choice(PRENOMS), random.choice(NOMS)
        org = orgs[(i - 1) % len(orgs)]
        base = f"{slugify(prenom)}.{slugify(nom)}"
        email = f"{base}@{org['domain']}"
        n = 2
        while email in used_emails:
            email = f"{base}{n}@{org['domain']}"
            n += 1
        used_emails.add(email)

        verified = random.random() < 0.82
        trips_count = random.choices([0, 1, 2, 3, 5, 8, 12, 20], [25, 15, 15, 12, 12, 9, 7, 5])[0]
        # Un compte sans trajet n'a jamais été noté : garder une note serait incohérent.
        rating = 0.0 if trips_count == 0 else round(random.uniform(3.6, 5.0), 1)

        users.append({
            "id": ID_OFFSET + i, "uuid": uuid_for("0b2", i), "email": email,
            "firstname": prenom, "lastname": nom,
            "phone": f"+324{random.randint(70, 99)}{random.randint(100000, 999999)}",
            "role": "ADMIN" if i <= 2 else "USER",
            "verified": verified, "rating": rating, "trips_count": trips_count,
            "org_id": org["id"],
            "created": dt(TODAY - timedelta(days=random.randint(30, 700)),
                          random.randint(8, 20), random.choice([0, 15, 30, 45])),
        })
        members.append((ID_OFFSET + i, org["id"]))

    # Un utilisateur sur cinq appartient à une seconde organisation
    # (consultant, étudiant en alternance, prestataire).
    for u in users:
        if random.random() < 0.2:
            other = random.choice(orgs)["id"]
            if (u["id"], other) not in members:
                members.append((u["id"], other))

    # ── Véhicules ──
    plates = set()
    # Les conducteurs sont ceux qui ont déjà publié des trajets.
    drivers = [u for u in users if u["trips_count"] > 0]
    owners = [random.choice(drivers) for _ in range(N_VEHICLES)]
    for i, owner in enumerate(owners, start=1):
        brand, model, seats, energies = random.choice(VOITURES)
        while True:
            plate = (f"{random.randint(1,2)}-"
                     f"{''.join(random.choices('ABCDEFGHJKLMNPRSTVWXYZ', k=3))}-"
                     f"{random.randint(100, 999)}")
            if plate not in plates:
                plates.add(plate)
                break
        vehicles.append({
            "id": ID_OFFSET + i, "uuid": uuid_for("0c3", i), "brand": brand, "model": model,
            "plate": plate, "seats": seats, "energy": random.choice(energies),
            "owner_id": owner["id"],
            "created": dt(TODAY - timedelta(days=random.randint(20, 600)), 10, 0),
        })

    by_owner = {}
    for v in vehicles:
        by_owner.setdefault(v["owner_id"], []).append(v)

    # ── Trajets ──
    villes = list(VILLES)
    for i in range(1, N_TRIPS + 1):
        driver = random.choice([u for u in users if u["id"] in by_owner])
        veh = random.choice(by_owner[driver["id"]])

        dep, arr = random.sample(villes, 2)
        km = distance(dep, arr)
        # Environ 8 centimes du kilomètre, arrondi à 50 centimes, plancher 2 €.
        price = max(2.0, round(km * 0.08 * 2) / 2)

        # Trajets répartis de part et d'autre d'aujourd'hui, pour que la
        # recherche renvoie des résultats et que l'historique soit fourni.
        offset = random.randint(-120, 45)
        day = TODAY + timedelta(days=offset)
        # Les trajets domicile-travail se concentrent matin et fin de journée.
        hour = random.choices(
            [6, 7, 8, 9, 12, 14, 16, 17, 18, 19],
            [8, 22, 18, 8, 4, 4, 8, 16, 14, 6],
        )[0]
        minute = random.choice([0, 15, 30, 45])

        seats = min(veh["seats"] - 1, random.choices([1, 2, 3, 4], [30, 40, 20, 10])[0])

        if offset < 0:
            status = random.choices(["COMPLETED", "CANCELLED"], [92, 8])[0]
        else:
            status = random.choices(["PLANNED", "FULL", "CANCELLED"], [82, 12, 6])[0]

        trips.append({
            "id": ID_OFFSET + i, "uuid": uuid_for("0d4", i),
            "dep": dep, "arr": arr,
            # Guillemets doubles obligatoires ici : en Python, 'de l''Industrie'
            # concatène deux littéraux et fait disparaître l'apostrophe.
            "dep_addr": f"{random.choice(VOIES_DEP)} {random.choice(LIEUX_DEP)} "
                        f"{random.randint(1, 180)}",
            "arr_addr": f"{random.choice(VOIES_ARR)} {random.choice(LIEUX_ARR)} "
                        f"{random.randint(1, 180)}",
            "time": dt(day, hour, minute),
            "seats": seats, "price": price,
            "desc": random.choice(DESCRIPTIONS),
            "luggage": random.random() < 0.85,
            "pets": random.random() < 0.18,
            "music": random.random() < 0.9,
            "talking": random.random() < 0.88,
            "status": status,
            "driver_id": driver["id"], "vehicule_id": veh["id"],
            "org_id": driver["org_id"] if random.random() < 0.75 else None,
            "past": offset < 0,
            "created": dt(day - timedelta(days=random.randint(3, 30)), 11, 0),
        })

    # ── Réservations ──
    bid = 0
    for t in trips:
        if t["status"] == "CANCELLED":
            n_req = random.choices([0, 1, 2], [50, 35, 15])[0]
        else:
            n_req = random.choices([0, 1, 2, 3, 4], [12, 30, 28, 20, 10])[0]

        # Un conducteur ne réserve pas sa propre place.
        candidats = [u for u in users if u["id"] != t["driver_id"]]
        passagers = random.sample(candidats, min(n_req, len(candidats)))

        confirmed_seats = 0
        for p in passagers:
            bid += 1
            libre = t["seats"] - confirmed_seats
            if libre <= 0:
                seats_booked = 1
                statut = random.choice(["REJECTED", "CANCELLED"])
            else:
                seats_booked = random.choices([1, 2], [82, 18])[0]
                seats_booked = min(seats_booked, libre)

                if t["status"] == "CANCELLED":
                    statut = "CANCELLED"
                elif t["past"]:
                    statut = random.choices(
                        ["COMPLETED", "CANCELLED", "REJECTED"], [80, 12, 8])[0]
                else:
                    statut = random.choices(
                        ["PENDING", "CONFIRMED", "REJECTED", "CANCELLED"],
                        [38, 42, 12, 8])[0]

                if statut in ("CONFIRMED", "COMPLETED"):
                    confirmed_seats += seats_booked

            reason = None
            if statut == "REJECTED":
                reason = random.choice([
                    "La voiture est déjà complète pour ce trajet.",
                    "Je ne passe finalement pas par ce point de rendez-vous.",
                    "Le trajet est réservé aux collègues de mon service.",
                    "Trop de bagages annoncés pour le coffre disponible.",
                ])
            elif statut == "CANCELLED":
                reason = random.choice([
                    "Je peux finalement prendre le train.",
                    "Réunion annulée, je ne fais plus le déplacement.",
                    "Changement d'horaire de mon côté.",
                    None, None,
                ])

            bookings.append({
                "id": ID_OFFSET + bid, "uuid": uuid_for("0e5", bid),
                "trip_id": t["id"], "passenger_id": p["id"],
                "seats": seats_booked,
                "total": round(seats_booked * t["price"], 2),
                "status": statut, "reason": reason,
                "created": t["created"],
            })

    # ── Articles ──
    for i in range(1, N_ARTICLES + 1):
        modele, categorie = ARTICLE_SUJETS[(i - 1) % len(ARTICLE_SUJETS)]
        a, b = random.sample(list(VILLES), 2)
        titre = modele.format(n=random.choice([3, 5, 8, 12, 15, 18, 22, 25, 30, 40, 2030, 2035]), a=a, b=b)
        d = TODAY - timedelta(days=random.randint(1, 540))
        articles.append({
            "id": f"seed-{i:03d}",
            "category": categorie,
            "title": titre,
            "normalized": slugify(titre).replace("-", " "),
            "summary": (
                f"{titre}. Cet article de démonstration alimente le flux Actus "
                f"Mobilité du jeu de test. Il reprend un sujet représentatif de "
                f"ceux agrégés en production depuis GNews et NewsData, sans "
                f"reprendre de contenu réel."
            ),
            "source": random.choice(SOURCES),
            "date": d.strftime("%Y-%m-%d"),
            "image": None,
            "url": f"https://exemple.coshift.test/actus/{i:03d}-{slugify(titre)[:60]}",
            "created": dt(d, 6, 30),
        })

    return orgs, users, members, vehicles, trips, bookings, articles


# ─────────────────────────────────────────────────────────────────────────────
#  Vérification des invariants — le jeu doit être cohérent, pas seulement gros
# ─────────────────────────────────────────────────────────────────────────────

def check(orgs, users, members, vehicles, trips, bookings, articles):
    errs = []
    tri = {t["id"]: t for t in trips}

    # ── Clés étrangères ──
    # Contrôle ajouté après un incident réel : le décalage des identifiants
    # avait été appliqué partout sauf au rattachement des membres, qui
    # pointait alors vers des utilisateurs inexistants. MySQL l'aurait
    # refusé, et Flyway aurait marqué la migration en échec.
    ids_users = {u["id"] for u in users}
    ids_orgs = {o["id"] for o in orgs}
    ids_veh = {v["id"] for v in vehicles}
    ids_trips = {t["id"] for t in trips}

    for u, o in members:
        if u not in ids_users:
            errs.append(f"organization_members : utilisateur {u} inexistant")
        if o not in ids_orgs:
            errs.append(f"organization_members : organisation {o} inexistante")
    for v in vehicles:
        if v["owner_id"] not in ids_users:
            errs.append(f"vehicule {v['id']} : proprietaire {v['owner_id']} inexistant")
    for t in trips:
        if t["driver_id"] not in ids_users:
            errs.append(f"trajet {t['id']} : conducteur inexistant")
        if t["vehicule_id"] not in ids_veh:
            errs.append(f"trajet {t['id']} : vehicule inexistant")
        if t["org_id"] is not None and t["org_id"] not in ids_orgs:
            errs.append(f"trajet {t['id']} : organisation inexistante")
    for b in bookings:
        if b["trip_id"] not in ids_trips:
            errs.append(f"reservation {b['id']} : trajet inexistant")
        if b["passenger_id"] not in ids_users:
            errs.append(f"reservation {b['id']} : passager inexistant")

    # Tous les identifiants doivent rester au-dessus du décalage, sans quoi
    # ils entrent en collision avec les lignes déjà présentes en base.
    for nom, jeu in (("users", ids_users), ("organizations", ids_orgs),
                     ("vehicules", ids_veh), ("trips", ids_trips),
                     ("bookings", {b["id"] for b in bookings})):
        if jeu and min(jeu) <= ID_OFFSET:
            errs.append(f"{nom} : identifiant {min(jeu)} sous le decalage {ID_OFFSET}")

    for b in bookings:
        t = tri[b["trip_id"]]
        if b["passenger_id"] == t["driver_id"]:
            errs.append(f"réservation {b['id']} : le conducteur réserve son propre trajet")
        if abs(b["total"] - b["seats"] * t["price"]) > 0.001:
            errs.append(f"réservation {b['id']} : prix total incohérent")
        if b["seats"] < 1:
            errs.append(f"réservation {b['id']} : moins d'une place")

    for t in trips:
        pris = sum(b["seats"] for b in bookings
                   if b["trip_id"] == t["id"] and b["status"] in ("CONFIRMED", "COMPLETED"))
        if pris > t["seats"]:
            errs.append(f"trajet {t['id']} : {pris} places retenues pour {t['seats']} offertes")

    veh = {v["id"]: v for v in vehicles}
    for t in trips:
        if t["seats"] > veh[t["vehicule_id"]]["seats"] - 1:
            errs.append(f"trajet {t['id']} : plus de places offertes que le véhicule n'en a")
        if veh[t["vehicule_id"]]["owner_id"] != t["driver_id"]:
            errs.append(f"trajet {t['id']} : le véhicule n'appartient pas au conducteur")

    for u in users:
        if u["trips_count"] == 0 and u["rating"] > 0:
            errs.append(f"utilisateur {u['id']} : noté sans avoir voyagé")

    if len({u["email"] for u in users}) != len(users):
        errs.append("adresses e-mail en double")
    if len({v["plate"] for v in vehicles}) != len(vehicles):
        errs.append("plaques en double")
    if len({a["url"] for a in articles}) != len(articles):
        errs.append("URL d'articles en double")

    hors = {a["category"] for a in articles} - CATEGORIES_VALIDES
    if hors:
        errs.append(f"catégories inconnues du filtre de la page Actus : {sorted(hors)}")
    manquantes = CATEGORIES_VALIDES - {a["category"] for a in articles}
    if manquantes:
        errs.append(f"catégories sans aucun article : {sorted(manquantes)}")

    return errs


# ─────────────────────────────────────────────────────────────────────────────
#  Rendu SQL
# ─────────────────────────────────────────────────────────────────────────────

def render(orgs, users, members, vehicles, trips, bookings, articles):
    L = []
    w = L.append

    w("-- =============================================================================")
    w("--  CoShift — jeu de données de test")
    w("-- =============================================================================")
    w("--  Généré par scripts/generate_seed.py (tirage déterministe, graine "
      f"{SEED}).")
    w("--  Ne pas modifier à la main : relancer le script et remplacer ce fichier.")
    w("--")
    w(f"--  Volumétrie : {len(orgs)} organisations, {len(users)} utilisateurs, "
      f"{len(members)} rattachements,")
    w(f"--               {len(vehicles)} véhicules, {len(trips)} trajets, "
      f"{len(bookings)} réservations,")
    w(f"--               {len(articles)} articles.")
    w("--")
    w("--  `organizations` est volontairement en deçà de 100 lignes : une")
    w("--  plateforme B2B compte quelques dizaines de clients, pas des centaines.")
    w("--  Table naturellement limitée.")
    w("--")
    w("--  Les organisations sont fictives. Nommer de vraies entreprises")
    w("--  laisserait croire à un partenariat qui n'existe pas. Les villes, les")
    w("--  marques et les modèles de voitures sont en revanche réels, pour que")
    w("--  distances, motorisations et nombres de places restent crédibles.")
    w("--")
    w("--  Les articles sont des fixtures : intitulés rédigés pour ce jeu,")
    w("--  organes de presse fictifs, URL sur un domaine de test. Aucun contenu")
    w("--  réel n'est reproduit et aucun texte n'est attribué à un vrai média.")
    w("--")
    w(f"--  Tous les comptes partagent le mot de passe de test « {PASSWORD_PLAIN} ».")
    w("--  Empreinte BCrypt force 10. Réservé au développement et à la")
    w("--  démonstration : ce jeu n'a pas vocation à être joué en production.")
    w("-- =============================================================================")
    w("")

    # organizations
    w("-- ── Organisations clientes ──")
    w("INSERT INTO organizations (id, uuid, name, slug, logo_url, active, created_at, updated_at) VALUES")
    rows = [f"({o['id']}, {sql_str(o['uuid'])}, {sql_str(o['name'])}, {sql_str(o['slug'])}, "
            f"NULL, b'1', {sql_str(o['created'])}, {sql_str(o['created'])})" for o in orgs]
    w(",\n".join(rows) + ";")
    w("")

    # users
    w("-- ── Utilisateurs ──")
    w("--  Deux comptes ADMIN en tête de liste, le reste en USER.")
    w("INSERT INTO users (id, uuid, email, password, firstname, lastname, picture_url,")
    w("                   phone_number, role, email_verified, verification_code,")
    w("                   verification_code_expiry, average_rating, trips_count,")
    w("                   created_at, updated_at) VALUES")
    rows = []
    for u in users:
        rows.append(
            f"({u['id']}, {sql_str(u['uuid'])}, {sql_str(u['email'])}, {sql_str(PASSWORD_HASH)}, "
            f"{sql_str(u['firstname'])}, {sql_str(u['lastname'])}, NULL, {sql_str(u['phone'])}, "
            f"{sql_str(u['role'])}, b'{1 if u['verified'] else 0}', NULL, NULL, "
            f"{u['rating']}, {u['trips_count']}, {sql_str(u['created'])}, {sql_str(u['created'])})"
        )
    w(",\n".join(rows) + ";")
    w("")

    # organization_members
    w("-- ── Rattachement des utilisateurs à leurs organisations ──")
    w("INSERT INTO organization_members (user_id, organization_id) VALUES")
    w(",\n".join(f"({u}, {o})" for u, o in members) + ";")
    w("")

    # vehicules
    w("-- ── Véhicules ──")
    w("INSERT INTO vehicules (id, uuid, brand, model, license_plate, seats, energy,")
    w("                       photo_url, owner_id, created_at, updated_at) VALUES")
    rows = [
        f"({v['id']}, {sql_str(v['uuid'])}, {sql_str(v['brand'])}, {sql_str(v['model'])}, "
        f"{sql_str(v['plate'])}, {v['seats']}, {sql_str(v['energy'])}, NULL, {v['owner_id']}, "
        f"{sql_str(v['created'])}, {sql_str(v['created'])})" for v in vehicles
    ]
    w(",\n".join(rows) + ";")
    w("")

    # trips
    w("-- ── Trajets ──")
    w("--  Répartis de part et d'autre du 19 août 2026 : les trajets passés")
    w("--  alimentent l'historique, les trajets à venir la recherche.")
    w("INSERT INTO trips (id, uuid, departure_city, departure_address, arrival_city,")
    w("                   arrival_address, departure_time, available_seats, price_per_seat,")
    w("                   description, accepts_luggage, accepts_pets, music_allowed,")
    w("                   talking_allowed, status, driver_id, vehicule_id, organization_id,")
    w("                   created_at, updated_at) VALUES")
    rows = []
    for t in trips:
        rows.append(
            f"({t['id']}, {sql_str(t['uuid'])}, {sql_str(t['dep'])}, {sql_str(t['dep_addr'])}, "
            f"{sql_str(t['arr'])}, {sql_str(t['arr_addr'])}, {sql_str(t['time'])}, "
            f"{t['seats']}, {t['price']:.2f}, {sql_str(t['desc'])}, "
            f"b'{1 if t['luggage'] else 0}', b'{1 if t['pets'] else 0}', "
            f"b'{1 if t['music'] else 0}', b'{1 if t['talking'] else 0}', "
            f"{sql_str(t['status'])}, {t['driver_id']}, {t['vehicule_id']}, "
            f"{t['org_id'] if t['org_id'] else 'NULL'}, "
            f"{sql_str(t['created'])}, {sql_str(t['created'])})"
        )
    w(",\n".join(rows) + ";")
    w("")

    # bookings
    w("-- ── Réservations ──")
    w("--  Le prix total découle du prix au siège, et le cumul des réservations")
    w("--  confirmées ne dépasse jamais le nombre de places offertes.")
    w("INSERT INTO bookings (id, uuid, trip_id, passenger_id, seats_booked, total_price,")
    w("                      status, status_reason, created_at, updated_at) VALUES")
    rows = [
        f"({b['id']}, {sql_str(b['uuid'])}, {b['trip_id']}, {b['passenger_id']}, "
        f"{b['seats']}, {b['total']:.2f}, {sql_str(b['status'])}, {sql_str(b['reason'])}, "
        f"{sql_str(b['created'])}, {sql_str(b['created'])})" for b in bookings
    ]
    w(",\n".join(rows) + ";")
    w("")

    # articles
    w("-- ── Articles du flux Actus Mobilité ──")
    w("INSERT INTO articles (id, category, title, normalized_title, summary, source,")
    w("                      date, image_url, url, created_at) VALUES")
    rows = [
        f"({sql_str(a['id'])}, {sql_str(a['category'])}, {sql_str(a['title'])}, "
        f"{sql_str(a['normalized'])}, {sql_str(a['summary'])}, {sql_str(a['source'])}, "
        f"{sql_str(a['date'])}, NULL, {sql_str(a['url'])}, {sql_str(a['created'])})"
        for a in articles
    ]
    w(",\n".join(rows) + ";")
    w("")

    return "\n".join(L)


def main():
    data = build()
    errs = check(*data)
    if errs:
        print(f"{len(errs)} incohérence(s) détectée(s) :")
        for e in errs[:20]:
            print("  -", e)
        raise SystemExit(1)

    out = Path(__file__).resolve().parents[1] / "src/main/resources/db/migration/V3__Seed_test_data.sql"
    out.write_text(render(*data), encoding="utf-8")

    orgs, users, members, vehicles, trips, bookings, articles = data
    print(f"Écrit {out.relative_to(Path(__file__).resolve().parents[1])}")
    print(f"  organizations        {len(orgs):>4}  (table naturellement limitée)")
    print(f"  users                {len(users):>4}")
    print(f"  organization_members {len(members):>4}")
    print(f"  vehicules            {len(vehicles):>4}")
    print(f"  trips                {len(trips):>4}")
    print(f"  bookings             {len(bookings):>4}")
    print(f"  articles             {len(articles):>4}")
    print("Invariants vérifiés : aucune incohérence.")


if __name__ == "__main__":
    main()
