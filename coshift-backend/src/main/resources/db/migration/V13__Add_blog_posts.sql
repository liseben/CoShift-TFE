-- =============================================================================
--  V13 — Le blog passe en base, et devient rédigeable sans redéploiement
-- =============================================================================
--  `src/config/blog.ts` annonçait exactement ce moment : « le jour où le blog
--  est rédigé par plusieurs personnes et mis à jour sans redéploiement, il
--  faudra une table et un éditeur. Le composant qui affiche un billet n'aura
--  pas à changer : il reçoit déjà un titre, un chapeau et une suite de
--  paragraphes. » C'est ce qui se produit ici.
--
--  DEUX TABLES, PAS UNE
--  Un billet existe indépendamment de ses traductions : il a une date, une
--  rubrique, un auteur, un fragment d'URL. Ce sont ses traductions qui portent
--  le texte. Écrire `title_fr`, `title_en`, `lead_fr`… dans une seule table
--  obligerait à une migration de schéma le jour où le néerlandais arrive, et
--  laisserait des colonnes vides pour chaque langue absente.
--
--  Un billet peut donc n'exister qu'en français. C'est assumé : mieux vaut un
--  billet lisible dans une langue qu'un billet retenu jusqu'à sa traduction.
--  L'interface se rabat sur la langue disponible.
--
--  CE QUE CETTE MIGRATION FAIT PERDRE
--  Les textes vivaient dans le catalogue de traduction, où une clé absente
--  d'une langue est une erreur de compilation. En base, plus rien ne garantit
--  qu'un billet existe dans les deux langues. C'est le prix de la rédaction
--  sans redéploiement, et il est payé en connaissance de cause.
--
--  LES QUATRE BILLETS EXISTANTS
--  Repris tels quels depuis `fr.ts` et `en.ts`, avec leurs dates, rubriques et
--  durées de lecture d'origine. Leur auteur reste NULL : ils ont été écrits
--  avant qu'un éditeur existe, et leur attribuer rétroactivement un compte
--  serait une donnée inventée.
-- =============================================================================

CREATE TABLE blog_posts (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    uuid            VARCHAR(255) NOT NULL,
    -- Fragment d'URL. Unique et stable : il est indexé et partagé.
    slug            VARCHAR(120) NOT NULL,
    category        ENUM('PRODUIT','CONFIDENTIALITE','OUVERTURE','CONCEPTION') NOT NULL,
    -- NULL = brouillon. Une date de publication répond à « depuis quand »,
    -- ce qu'un booléen `publie` ne saurait pas dire.
    published_at    DATETIME(6)  DEFAULT NULL,
    reading_minutes INT          NOT NULL DEFAULT 3,
    -- NULL pour les billets antérieurs à l'éditeur. La contrainte n'est pas
    -- NOT NULL : un auteur peut aussi voir son compte anonymisé.
    author_id       BIGINT       DEFAULT NULL,
    created_at      DATETIME(6)  DEFAULT NULL,
    updated_at      DATETIME(6)  DEFAULT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_posts_uuid (uuid),
    UNIQUE KEY uk_blog_posts_slug (slug),
    -- La liste publique filtre sur la publication et trie sur cette date.
    KEY idx_blog_posts_published (published_at),
    CONSTRAINT fk_blog_posts_author
        FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE blog_post_translations (
    id      BIGINT       NOT NULL AUTO_INCREMENT,
    post_id BIGINT       NOT NULL,
    locale  VARCHAR(5)   NOT NULL,
    title   VARCHAR(200) NOT NULL,
    lead    VARCHAR(500) NOT NULL,
    -- Paragraphes séparés par une ligne vide. Pas de HTML : le texte saisi est
    -- rendu comme du texte, ce qui interdit d'injecter un script par l'éditeur.
    body    MEDIUMTEXT   NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_translation (post_id, locale),
    CONSTRAINT fk_blog_translation_post
        FOREIGN KEY (post_id) REFERENCES blog_posts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ─────────────────────────────────────────────────────────────────────────────
--  Les quatre billets déjà écrits
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO blog_posts (id, uuid, slug, category, published_at, reading_minutes,
                        author_id, created_at, updated_at) VALUES
(3001, '0e5-3001-0000-4000-8000-000000003001', 'confirmer-un-trajet', 'CONCEPTION', '2026-08-23 09:00:00', 3, NULL,
 '2026-08-23 09:00:00', '2026-08-23 09:00:00'),
(3002, '0e5-3002-0000-4000-8000-000000003002', 'vos-donnees-en-clair', 'CONFIDENTIALITE', '2026-08-19 09:00:00', 4, NULL,
 '2026-08-19 09:00:00', '2026-08-19 09:00:00'),
(3003, '0e5-3003-0000-4000-8000-000000003003', 'donnees-ouvertes', 'OUVERTURE', '2026-08-14 09:00:00', 3, NULL,
 '2026-08-14 09:00:00', '2026-08-14 09:00:00'),
(3004, '0e5-3004-0000-4000-8000-000000003004', 'domicile-travail', 'PRODUIT', '2026-08-08 09:00:00', 4, NULL,
 '2026-08-08 09:00:00', '2026-08-08 09:00:00');

INSERT INTO blog_post_translations (id, post_id, locale, title, lead, body) VALUES
(3001, 3001, 'fr', 'Pourquoi c''est au passager de confirmer le trajet', 'Une petite décision de conception qui dit beaucoup sur la façon dont on fabrique une information fiable.', 'Quand un covoiturage a eu lieu, quelqu''un doit le déclarer. Ce geste compte : il incrémente le nombre de trajets des deux participants, il ouvre le droit de laisser un avis, et il alimentera demain le partage des frais. La question de savoir qui appuie sur le bouton n''est donc pas un détail d''interface.

Le réflexe serait de confier la confirmation au conducteur. C''est lui qui organise, lui qui conduit, lui qui a la vue d''ensemble. Sauf que c''est précisément le problème : il a un intérêt à déclarer la course effectuée. Elle alimente son compteur, elle nourrit sa réputation, et le jour où l''argent circulera, elle le paiera.

Le passager, lui, n''a rien à y gagner. Confirmer ne lui rapporte pas de trajet supplémentaire, ne le fait pas mieux noter, ne lui rembourse rien. Il constate, et c''est tout. C''est exactement ce qui rend sa confirmation crédible.

La règle générale tient en une phrase : quand une déclaration doit être fiable, confiez-la à la partie qui n''en tire aucun bénéfice. Ce n''est pas une question de confiance dans les personnes, c''est une question de conception. Un système qui repose sur la bonne volonté de celui qui y gagne finit toujours par récompenser ceux qui en abusent.

Le serveur ajoute deux garde-fous. La réservation doit avoir été acceptée — une demande restée en attente n''a transporté personne. Et le départ doit être passé, sans quoi il suffirait de réserver pour gonfler son compteur. L''opération n''est pas rejouable : une seconde confirmation est refusée, et le compteur ne bouge pas.

Le même raisonnement se retrouve ailleurs dans le projet. Le freinage des tentatives de connexion compte par couple adresse IP et compte visé, précisément pour qu''un tiers ne puisse pas verrouiller le compte de quelqu''un d''autre en échouant volontairement. Dans les deux cas, on ne se protège pas de la maladresse mais de l''intérêt.'),
(3002, 3001, 'en', 'Why it is the passenger who confirms the trip', 'A small design decision that says a great deal about how reliable information is manufactured.', 'When a carpool has taken place, someone has to declare it. That act matters: it increments both participants'' trip counts, it opens the right to leave a review, and tomorrow it will drive cost sharing. Who presses the button is therefore not an interface detail.

The instinct would be to leave confirmation to the driver. They organise, they drive, they have the overview. Except that is exactly the problem: they have an interest in declaring the trip completed. It feeds their counter, it builds their reputation, and the day money changes hands, it pays them.

The passenger has nothing to gain. Confirming earns them no extra trip, no better rating, no refund. They simply observe. That is precisely what makes their confirmation credible.

The general rule fits in one sentence: when a declaration must be reliable, entrust it to the party that gains nothing from it. This is not about trusting people, it is about design. A system that relies on the goodwill of whoever benefits ends up rewarding those who abuse it.

The server adds two safeguards. The booking must have been accepted — a pending request carried nobody. And departure must be in the past, otherwise booking alone would inflate one''s counter. The operation cannot be replayed: a second confirmation is refused, and the counter does not move.

The same reasoning appears elsewhere in the project. Login attempt throttling counts per IP address and targeted account pair, precisely so that a third party cannot lock someone else''s account by failing on purpose. In both cases, we are guarding not against clumsiness but against interest.'),
(3003, 3002, 'fr', 'Ce que nous faisons de vos données, en clair', 'Trois droits que le règlement européen ouvre, et ce qu''il a fallu écrire pour qu''ils existent vraiment.', 'Une politique de confidentialité peut annoncer n''importe quoi. Elle ne coûte rien à rédiger, et rien dans le produit n''oblige à ce qu''elle soit vraie. Nous avons voulu que chaque phrase de la nôtre corresponde à du code qu''on peut lire.

Le droit d''accès et le droit à la portabilité — articles 15 et 20 — se traduisent par un bouton qui produit un fichier JSON contenant tout ce que nous détenons sur vous. Le format n''est pas anodin : le règlement exige quelque chose de « structuré, couramment utilisé et lisible par machine », trois qualités qu''un PDF n''a pas.

L''export contient vos données, pas celles des autres. Un trajet que vous avez réservé chez quelqu''un y figure avec son itinéraire et son horaire, mais sans le téléphone ni l''adresse du conducteur. Ce sont ses données à lui, et votre droit à la portabilité ne porte pas dessus. Un bloc du fichier énumère d''ailleurs ce qui en est volontairement absent, et pourquoi.

Le droit à l''effacement — article 17 — a demandé une décision moins évidente. Supprimer purement votre ligne détruirait les trajets auxquels d''autres personnes ont participé : effacer un conducteur priverait ses passagers de leur propre historique. Faire droit à votre demande en portant atteinte aux données d''un tiers n''est pas une option.

L''effacement procède donc par anonymisation sur place. Nom, adresse, téléphone, photographie, plaque d''immatriculation, empreinte du mot de passe : tout est écrasé, immédiatement, sans copie de sauvegarde. Ce qui subsiste — un trajet Namur-Bruxelles rattaché à un participant sans nom — ne se rapporte plus à une personne identifiable.

Avant d''anonymiser, l''opération annule vos trajets à venir et prévient leurs passagers. Un trajet futur dont le conducteur a disparu laisserait quelqu''un attendre à un point de rendez-vous. Supprimer un compte doit prévenir, pas seulement se taire.

Enfin, la limitation de la durée de conservation — article 5.1.e — tourne toutes les nuits. Une inscription jamais confirmée est supprimée au bout de trente jours : une adresse dont personne n''a prouvé qu''elle lui appartenait n''est pas un compte. Les trajets de plus de deux ans perdent leurs adresses précises et leurs descriptions. Restent les villes et les dates, qui ne désignent personne.'),
(3004, 3002, 'en', 'What we do with your data, plainly', 'Three rights the European regulation grants, and what had to be written for them to actually exist.', 'A privacy policy can claim anything. It costs nothing to write, and nothing in the product forces it to be true. We wanted every sentence of ours to match code you can read.

The right of access and the right to portability — articles 15 and 20 — become a button that produces a JSON file containing everything we hold about you. The format is not incidental: the regulation requires something structured, commonly used and machine-readable, three qualities a PDF does not have.

The export contains your data, not other people''s. A trip you booked with someone appears with its route and time, but without the driver''s phone number or address. Those are their data, and your right to portability does not extend to them. One block of the file lists what is deliberately absent, and why.

The right to erasure — article 17 — required a less obvious decision. Simply deleting your row would destroy trips other people took part in: erasing a driver would strip their passengers of their own history. Granting your request by harming a third party''s data is not an option.

Erasure therefore proceeds by anonymisation in place. Name, address, phone number, photograph, licence plate, password hash: all overwritten, immediately, with no backup copy. What remains — a Namur-Brussels trip attached to a nameless participant — no longer relates to an identifiable person.

Before anonymising, the operation cancels your upcoming trips and notifies their passengers. A future trip whose driver has vanished would leave someone waiting at a meeting point. Deleting an account must warn, not merely go quiet.

Finally, storage limitation — article 5.1.e — runs every night. A sign-up never confirmed is deleted after thirty days: an address nobody proved they own is not an account. Trips older than two years lose their precise addresses and descriptions. Cities and dates remain, and they identify nobody.'),
(3005, 3003, 'fr', 'Nos données sont ouvertes, et voici pourquoi', 'Publier ses chiffres quand on pourrait les garder, et le seuil qui sépare une statistique d''un déplacement individuel.', 'CoShift publie un jeu de données ouvertes, sous Licence Ouverte 2.0, accessible sans compte ni clé : volumes de trajets, places partagées, villes desservies, taux de remplissage, répartition par mois. En JSON pour les machines, en CSV pour les tableurs — parce que c''est par le tableur qu''une donnée ouverte est réellement réutilisée par quelqu''un qui n''écrit pas de code.

Nous le faisons d''abord par cohérence. Nous demandons aux organisations de mesurer leur mobilité plutôt que de la déclarer ; garder nos propres chiffres reviendrait à demander qu''on nous croie sur parole.

Publier des chiffres de mobilité pose une difficulté que les données personnelles ne posent pas : un agrégat suffisamment fin redevient nominatif. Une liaison entre deux villes empruntée deux fois dans l''année ne décrit pas un flux, elle décrit le déplacement de deux personnes qu''on retrouve sans peine.

D''où un seuil. Une ville n''apparaît dans le jeu qu''au-delà de cinq trajets. En dessous, elle est retirée — et le nombre de villes ainsi écartées est publié, pour qu''un réutilisateur sache que le jeu n''est pas exhaustif. Les couples départ-arrivée, eux, ne sont pas publiés du tout : au volume actuel, aucun n''atteint le seuil.

Le jeu exclut par construction l''identité des conducteurs et des passagers, les adresses précises, les horaires de départ, le nom des organisations et les montants payés individuellement. Cette liste figure dans la réponse elle-même : ce qui est absent est annoncé, plutôt que laissé à deviner.

Les trajets annulés sont comptés séparément des trajets publiés. Les mélanger gonflerait les volumes ; les taire relèverait de la malhonnêteté statistique. Publier ses chiffres n''a d''intérêt que si l''on publie aussi ceux qui arrangent moins.'),
(3006, 3003, 'en', 'Our data is open, and here is why', 'Publishing your figures when you could keep them, and the threshold that separates a statistic from an individual journey.', 'CoShift publishes an open data set, under Open Licence 2.0, accessible without an account or a key: trip volumes, shared seats, cities served, occupancy rate, monthly breakdown. In JSON for machines, in CSV for spreadsheets — because a spreadsheet is how open data is actually reused by someone who does not write code.

We do it first for consistency. We ask organisations to measure their mobility rather than declare it; keeping our own figures would amount to asking to be taken at our word.

Publishing mobility figures raises a difficulty personal data does not: a sufficiently fine aggregate becomes nominative again. A route between two cities travelled twice in a year does not describe a flow, it describes the journey of two people who are easily identified.

Hence a threshold. A city appears in the set only above five trips. Below that it is removed — and the number of cities dropped is published, so a reuser knows the set is not exhaustive. Departure-arrival pairs are not published at all: at current volumes, none reaches the threshold.

The set excludes by construction the identity of drivers and passengers, precise addresses, departure times, organisation names and individual amounts paid. That list appears in the response itself: what is missing is announced rather than left to guess.

Cancelled trips are counted separately from published ones. Merging them would inflate the volumes; hiding them would be statistical dishonesty. Publishing your figures is only worth anything if you also publish the less flattering ones.'),
(3007, 3004, 'fr', 'Le trajet que personne ne partage', 'Pourquoi le covoiturage quotidien résiste là où le covoiturage longue distance a réussi.', 'Le covoiturage longue distance fonctionne. Un trajet de trois cents kilomètres coûte assez cher pour qu''on accepte un détour, on le planifie des jours à l''avance, et les plateformes grand public l''ont parfaitement résolu.

Le trajet domicile-travail est l''exact opposé. Il est court, donc l''économie individuelle est faible. Il est quotidien, donc s''organiser à chaque fois est hors de question. Et il est rigide : arriver vingt minutes en retard n''est pas une option quand on est attendu.

C''est pourtant celui qui compte. Ce ne sont pas les grands départs qui saturent les routes un mardi matin, mais des milliers de voitures qui font le même chemin, aux mêmes heures, avec un siège occupé sur cinq.

Les plateformes ouvertes butent ici sur un obstacle qui n''est pas technique. Elles doivent fabriquer de la confiance entre inconnus : profils, avis, vérifications, assurances. Cet appareil se justifie pour un trajet exceptionnel. Pour dix minutes de voiture avec quelqu''un qu''on croisera de toute façon à la machine à café, il est disproportionné.

Dans une organisation, cette confiance existe déjà. On partage un employeur, un bâtiment, des horaires, souvent des connaissances communes. Le problème restant n''est pas de savoir à qui se fier, mais de savoir qui part au même moment vers le même endroit. C''est un problème d''information, et un problème d''information se résout avec un logiciel.

C''est le pari de CoShift : ne pas recréer ce qui existe déjà, et se concentrer sur ce qui manque vraiment. Le cercle fermé n''est pas une limitation du produit, c''est sa condition de fonctionnement.'),
(3008, 3004, 'en', 'The trip nobody shares', 'Why daily carpooling resists where long-distance carpooling succeeded.', 'Long-distance carpooling works. A three-hundred-kilometre trip costs enough to justify a detour, it is planned days ahead, and consumer platforms have solved it thoroughly.

The commute is the exact opposite. It is short, so the individual saving is small. It is daily, so organising it each time is out of the question. And it is rigid: arriving twenty minutes late is not an option when someone expects you.

Yet it is the one that counts. It is not the holiday getaways that clog the roads on a Tuesday morning, but thousands of cars driving the same route, at the same hours, with one seat out of five taken.

Open platforms hit an obstacle here that is not technical. They must manufacture trust between strangers: profiles, reviews, verifications, insurance. That apparatus is justified for an exceptional journey. For ten minutes in a car with someone you will run into at the coffee machine anyway, it is disproportionate.

Inside an organisation, that trust already exists. You share an employer, a building, working hours, often mutual acquaintances. The remaining problem is not knowing whom to trust, but knowing who leaves at the same time for the same place. That is an information problem, and information problems are solved with software.

That is CoShift''s bet: not to rebuild what already exists, and to focus on what is genuinely missing. The closed circle is not a limitation of the product, it is the condition of its working.');
