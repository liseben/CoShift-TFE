# Charte graphique CoShift

> Extraite le 18 août 2026 des **15 fichiers CSS / 4 224 lignes** du frontend, par analyse
> automatisée des valeurs réellement écrites — et non d'une intention déclarée.
> Les contrastes sont calculés selon la formule de luminance relative WCAG 2.1.

**À l'attention d'un agent de design** : la section 2 décrit ce qui existe aujourd'hui,
la section 5 ce qui cloche, la section 6 le système cible à appliquer. En cas de conflit
entre une valeur observée et le système cible, **le système cible fait foi**.

---

## 1. Identité visuelle

CoShift est une plateforme de covoiturage **B2B** destinée aux organisations — entreprises,
universités, festivals, salons. L'interface doit inspirer la **fiabilité** d'un outil
professionnel tout en restant chaleureuse : on y confie ses trajets quotidiens à des collègues.

Le parti pris est un **dark glassmorphism** : fond sombre profond à dominante bleu-vert,
surfaces translucides floutées superposées, accent bleu lumineux. Il sert un objectif
concret — la page d'accueil affiche une carte Mapbox animée en fond, et seules des surfaces
translucides permettent de la laisser transparaître sans nuire à la lisibilité.

**Registre** : sobre, dense en information, animations discrètes. Jamais ludique, jamais
criard. Les cartes de trajet et les tableaux de bord priment sur les effets.

---

## 2. Fondations observées

### 2.1 Couleurs

Palette retenue, telle qu'employée dans le thème dominant.

#### Fonds

| Rôle | Valeur | Emploi |
|---|---|---|
| Fond applicatif | `#0f1a1c` | Fond de toutes les pages |
| Fond alternatif | `#0f172a` | Emails HTML, quelques surfaces |
| Surface niveau 1 | `rgba(255,255,255,0.04)` | Cartes, widgets |
| Surface niveau 2 | `rgba(255,255,255,0.05)` | Champs, boutons discrets |
| Surface survolée | `rgba(255,255,255,0.07)` | État `:hover` |
| Surface opaque | `#131f22` | Modales |
| Contraste sur accent | `#0b1416` | Texte posé sur un aplat bleu |

#### Bordures

| Rôle | Valeur |
|---|---|
| Bordure standard | `rgba(255,255,255,0.08)` à `rgba(255,255,255,0.12)` |
| Bordure discrète | `rgba(255,255,255,0.03)` |
| Séparateur | `rgba(255,255,255,0.07)` |

#### Texte

| Rôle | Valeur | Contraste sur `#0f1a1c` |
|---|---|---|
| Principal | `#f1f5f9` | 16,18:1 — AAA |
| Courant | `#e2e8f0` | 14,38:1 — AAA |
| Secondaire | `#cbd5e1` | 11,94:1 — AAA |
| Atténué | `#94a3b8` | 6,91:1 — AA |
| Discret | `#64748b` | 3,72:1 — **texte ≥ 24 px uniquement** |

#### Accent et couleurs sémantiques

| Rôle | Valeur | Contraste | Emploi |
|---|---|---|---|
| Accent | `#60a5fa` | 6,97:1 — AA | Liens, focus, prix, boutons principaux |
| Accent soutenu | `#3b82f6` | — | Dégradés, états actifs |
| Accent clair | `#93c5fd` | — | Texte sur pastille bleue |
| Succès | `#34d399` | 9,22:1 — AAA | Confirmations, départ d'itinéraire |
| Succès clair | `#6ee7b7` | — | Texte sur pastille verte |
| Avertissement | `#fbbf24` | 10,62:1 — AAA | En attente, étoiles de notation |
| Avertissement clair | `#fcd34d` | — | Texte sur pastille ambre |
| Erreur | `#f87171` | — | Bordures et icônes d'erreur |
| Erreur claire | `#fca5a5` | 9,34:1 — AAA | Texte d'erreur |
| Énergie électrique | `#34d399` | — | Badge véhicule |
| Énergie GPL | `#a78bfa` | — | Badge véhicule |

**Principe des pastilles sémantiques** : fond à 9–14 % d'opacité de la teinte, bordure à
25–35 %, texte dans la variante claire. Exemple : `background: rgba(52,211,153,0.13)`,
`border: 1px solid rgba(52,211,153,0.3)`, `color: #6ee7b7`.

### 2.2 Typographie

**Famille déclarée** : `'Inter', 'Segoe UI', system-ui, sans-serif`.
⚠️ Voir §5.2 — Inter n'est jamais chargée ; le rendu réel est celui du système.

**Graisses employées** : 500, 600, 620, 650, 700, 750, 800, 900.

**Tailles employées** : 20 valeurs distinctes, de `0.72rem` à `3rem`, sans progression
régulière (0.72 / 0.75 / 0.78 / 0.79 / 0.8 / 0.85 / 0.86 / 0.87 / 0.875 / 0.9 / 0.92 /
0.95 / 1 / 1.05 / 1.1 / 1.2 / 1.3 / 1.4 / 2 / 3 rem).

**Interlettrage** : `-0.02em` sur les grands titres (9 occurrences), `+0.05em` à `+0.11em`
sur les libellés en capitales.

**Conventions de casse** : les libellés de section sont en capitales, `0.72rem`, graisse 600,
interlettrage `0.11em`, couleur `#64748b`.

### 2.3 Espacements

Grille de base **4 px**, respectée dans l'ensemble sauf trois valeurs isolées (5, 7, 15 px).

| Palier | Valeur | Emploi principal |
|---|---|---|
| 3xs | 4 px | Écart icône / texte |
| 2xs | 6 px | Écart intra-badge |
| xs | 8 px | Écart entre puces |
| sm | 12 px | Écart entre champs |
| md | 16 px | Écart entre cartes |
| lg | 20–22 px | Padding interne de carte |
| xl | 26 px | Padding de grande carte |
| 2xl | 40 px | Padding de modale |
| Gouttière de page | 100 px haut / 20 px côtés / 60 px bas | Conteneurs de page |

### 2.4 Rayons de bordure

20 valeurs distinctes employées, de 4 px à 90 px, plus `50%`.
Les plus fréquentes : **10 px** (20×), **8 px** (15×), **12 px** (13×), **20 px** (10×),
`50%` (16×, pour les avatars).

### 2.5 Élévation

Pas de système d'élévation. ~15 ombres uniques, dont les récurrentes :

| Emploi | Valeur |
|---|---|
| Bouton principal au survol | `0 9px 22px -10px rgba(96,165,250,0.7)` |
| Bouton accent | `0 4px 12px rgba(59,130,246,0.3)` |
| Anneau de focus | `0 0 0 4px rgba(96,165,250,0.08)` |
| Halo de pastille d'itinéraire | `0 0 0 4px rgba(52,211,153,0.15)` |

### 2.6 Glassmorphism

Trois ingrédients toujours combinés :

```css
background: rgba(255, 255, 255, 0.04);
border: 1px solid rgba(255, 255, 255, 0.09);
backdrop-filter: blur(12px);
```

Le flou varie de 4 px à 24 px selon les fichiers ; **20 px** est la valeur la plus fréquente
(en-tête), **12 px** la plus courante pour les cartes.

### 2.7 Mouvement

**Durées** : 0,15 s à 0,3 s. La plus fréquente est `all 0.3s ease` (7×), puis
`0.2s` et `0.18s ease`.

**Animations récurrentes** (18 `@keyframes` au total) :

| Nom | Effet |
|---|---|
| `spin` | Rotation continue du spinner |
| `fadeSlideUp` | Apparition avec translation de 8 px vers le haut |
| `shimmer` | Balayage de dégradé sur les squelettes de chargement |
| `pulse` | Pulsation de l'icône email |
| `modalIn` / `overlayIn` | Ouverture de modale |

**Cascade** : les widgets s'affichent avec un `animation-delay` décalé (0 / 50 / 100 ms).

### 2.8 Points de rupture

9 valeurs employées : 480, 560, 620, 640, 768, 880, 900, 1437 px.

---

## 3. Motifs de composants

### Bouton principal
Dégradé `linear-gradient(135deg, #60a5fa, #3b82f6)`, texte `#0b1416`, rayon 11 px,
padding `13px`, graisse 700. Au survol : `translateY(-2px)` + ombre portée bleue.

### Bouton fantôme
Fond `rgba(255,255,255,0.05)`, bordure `rgba(255,255,255,0.12)`, texte `#cbd5e1`.

### Carte
Fond `rgba(255,255,255,0.04)`, bordure `rgba(255,255,255,0.09)`, rayon 14–16 px,
padding 20–26 px, `backdrop-filter: blur(12px)`.

### Carte à liseré de statut
Carte standard + `border-left: 3px solid` de la couleur sémantique. Le statut est **toujours
encodé deux fois** — liseré coloré et badge textuel — pour rester lisible sans la couleur.

### Champ de saisie
Fond `rgba(255,255,255,0.05)`, bordure `rgba(255,255,255,0.12)`, rayon 9–10 px,
padding `10px 13px`. Au focus : `border-color: #60a5fa`.

### Modale
Voile `rgba(0,0,0,0.6)` + `backdrop-filter: blur(4px)`, contenu `#131f22`,
bordure `rgba(255,255,255,0.1)`, rayon 20 px.

### Avatar
Cercle 35–52 px. Avec photo : `object-fit: cover` + bordure accent 2 px.
Sans photo : dégradé `135deg, #60a5fa, #3b82f6` et initiale en `#0f1a1c`, graisse 800.

---

## 4. Accessibilité — contrastes mesurés

Sur le fond `#0f1a1c`. Seuils WCAG AA : **4,5:1** en texte normal, **3:1** au-delà de 24 px.

| Couleur | Ratio | Verdict |
|---|---|---|
| `#f1f5f9` | 16,18:1 | AAA |
| `#e2e8f0` | 14,38:1 | AAA |
| `#cbd5e1` | 11,94:1 | AAA |
| `#fbbf24` | 10,62:1 | AAA |
| `#fca5a5` | 9,34:1 | AAA |
| `#34d399` | 9,22:1 | AAA |
| `#60a5fa` | 6,97:1 | AA |
| `#94a3b8` | 6,91:1 | AA |
| `rgba(255,255,255,0.5)` | 5,26:1 | AA |
| `#64748b` | 3,72:1 | ⚠️ grands textes seulement |
| `rgba(255,255,255,0.3)` | 2,71:1 | ❌ échec |
| `#475569` | 2,34:1 | ❌ échec |
| `#334155` | 1,71:1 | ❌ échec |

Texte `#0b1416` sur bouton `#60a5fa` : **7,34:1 — AAA**.

---

## 5. Points d'amélioration

Classés par gravité. Chiffres issus de l'analyse automatisée.

### 5.1 🔴 Quatre chartes concurrentes cohabitent

**166 valeurs de couleur distinctes** (55 hex + 111 rgba) pour une seule interface.
Le dark glassmorphism bleu est dominant, mais trois systèmes abandonnés subsistent :

| Vestige | Couleurs | Fichiers concernés |
|---|---|---|
| Vert « Karos » | `--primary-color: #00b87c`, `#009665` | `index.css`, `MainLayout.css`, `HomePage.css` |
| Orange | `--coshift-orange: #ff8c00`, `#ffaa00`, `#e67e22` | 6 fichiers, dont `MainLayout.tsx` |
| Thème clair | `--bg-color: #f4f6f8`, `--text-main: #2d3748` | `index.css`, `MainLayout.css`, `ActusPage.css` |
| Gabarit Vite | `#646cffaa`, `#61dafbaa` | `App.css` |

L'orange est encore **visible en production** : la bordure de l'avatar dans l'en-tête est
codée en dur en `#ffaa00` dans `MainLayout.tsx`, en contradiction avec l'accent bleu.

→ **Action** : supprimer les trois vestiges, ne conserver que le thème sombre bleu.

### 5.2 🔴 La police Inter n'est jamais chargée

`font-family: 'Inter', ...` est déclarée 5 fois, mais il n'existe **aucun `@font-face`,
aucun `<link>` vers Google Fonts, aucun paquet `@fontsource`**. Le navigateur retombe donc
sur `'Segoe UI'` sous Windows, `system-ui` ailleurs.

Conséquence directe : les graisses **620, 650 et 750** — qui n'existent que sur une fonte
variable — sont arrondies par le navigateur. Le rendu diffère d'une machine à l'autre, et
ne correspond à aucune des maquettes.

→ **Action** : installer `@fontsource-variable/inter` et l'importer, ou retirer Inter de la
déclaration et assumer la pile système.

### 5.3 🔴 Collision de variable CSS

`--text-muted` est défini **deux fois avec des valeurs incompatibles** :

- `index.css:6` → `#718096` (gris pour thème **clair**)
- `DashboardPage.css:11` → `rgba(255,255,255,0.3)` (blanc pour thème **sombre**)

Selon l'ordre de chargement, l'un écrase l'autre. Le second échoue par ailleurs au contraste
(2,71:1).

→ **Action** : un seul fichier de tokens, une seule définition par nom.

### 5.4 🟠 Trois contrastes sous le seuil WCAG AA

| Couleur | Ratio | Où | Correction proposée |
|---|---|---|---|
| `#334155` | 1,71:1 | Icônes d'état vide | `#5b6b7a` → 3,23:1 (décoratif, ≥ 24 px) |
| `#475569` | 2,34:1 | Pied d'email, texte discret | `#7d8b9c` → 5,10:1 (AA) |
| `rgba(255,255,255,0.3)` | 2,71:1 | `--text-muted` du dashboard | `rgba(255,255,255,0.55)` → 6,07:1 (AA) |

`#64748b` (3,72:1) reste conforme **uniquement** au-delà de 24 px, or il est employé à
`0.77rem` dans `.td-hint` et `.bk-empty p`.

→ **Action** : relever le plancher des gris à `#94a3b8` pour tout texte sous 24 px.
N4 et N5 du cahier des charges rendent l'accessibilité opposable.

### 5.5 🟠 Tokens définis mais non appliqués

Les variables CSS n'existent que dans **3 fichiers sur 15** (`index.css`,
`MainLayout.css`, `DashboardPage.css`). Partout ailleurs les valeurs sont écrites en dur :
`#60a5fa` apparaît **33 fois**, `#f1f5f9` 20 fois, `rgba(255,255,255,0.1)` 24 fois.

Changer l'accent de la marque demande aujourd'hui 33 modifications réparties dans 12 fichiers.

→ **Action** : un `tokens.css` unique importé par `index.css`, et substitution des valeurs
en dur par les `var(--*)`.

### 5.6 🟠 Duplication de composants CSS

| Classe | Fichiers |
|---|---|
| `.spinner` | 4 |
| `.input-group` | 4 |
| `.input-label` | 3 |

Pire, le **même spinner** existe sous trois noms d'animation différents : `spin`,
`td-spin`, `bk-spin`. J'ai moi-même redéclaré `.spinner` en créant `TripDetailPage.css` et
`BookingsPage.css`, faute de socle partagé — le problème se reproduit à chaque nouvelle page.

→ **Action** : extraire des composants React réutilisables (`Spinner`, `Input`, `Button`,
`Modal`, `Card`, `Badge`) et supprimer les copies.

### 5.7 🟡 Aucune échelle systématique

| Dimension | Valeurs distinctes | Problème |
|---|---|---|
| Tailles de police | 20 | Pas de progression modulaire ; `0.85` / `0.86` / `0.87` / `0.875` coexistent sans distinction perceptible |
| Rayons | 20 | De 4 px à 90 px, sans logique |
| Ombres | ~15 | Aucune échelle d'élévation |
| Flou | 7 | 4, 8, 10, 12, 16, 20, 24 px pour un même effet |
| Graisses | 9 | Dont 620, 650, 750 non standard |
| Points de rupture | 9 | Dont `1437px`, manifestement issu d'un écran particulier |

→ **Action** : ramener à 7 tailles, 4 rayons, 3 élévations, 2 flous, 4 graisses,
3 points de rupture. Voir §6.

### 5.8 🟡 Une ombre probablement accidentelle

`box-shadow: 0 25px 50px -12px rgba(158, 46, 46, 0.5)` — un **rouge-brun** dans une charte
bleue, présent 2 fois. Vraisemblablement un `rgba` modifié par erreur.

### 5.9 🟡 Mouvement non maîtrisé pour l'accessibilité

**18 animations** `@keyframes`, mais **une seule** occurrence de
`@media (prefers-reduced-motion)`. Les utilisateurs sensibles au mouvement subissent donc
la quasi-totalité des animations.

→ **Action** : une règle globale neutralisant `animation` et `transition` sous
`prefers-reduced-motion: reduce`.

### 5.10 🟡 Métadonnées du document par défaut

`index.html` est resté le gabarit Vite : `lang="en"` sur un site francophone,
`<title>coshift-frontend</title>`, favicon `vite.svg`. Aucune balise `theme-color`,
`description` ni Open Graph.

### 5.11 🟡 Pas d'états de focus visibles homogènes

Le focus clavier n'est traité que ponctuellement (`border-color` sur les champs). Aucune
règle `:focus-visible` globale — bloquant pour la navigation au clavier (N4).

---

## 6. Système cible proposé

Jeu de tokens à écrire dans `src/styles/tokens.css`, importé par `index.css`.

```css
:root {
  /* ── Fonds ── */
  --bg:            #0f1a1c;
  --surface-1:     rgba(255, 255, 255, 0.04);
  --surface-2:     rgba(255, 255, 255, 0.06);
  --surface-hover: rgba(255, 255, 255, 0.09);
  --surface-solid: #131f22;
  --on-accent:     #0b1416;

  /* ── Bordures ── */
  --border:        rgba(255, 255, 255, 0.09);
  --border-strong: rgba(255, 255, 255, 0.14);
  --divider:       rgba(255, 255, 255, 0.07);

  /* ── Texte — plancher AA respecté ── */
  --text:          #f1f5f9;   /* 16,18:1 */
  --text-soft:     #cbd5e1;   /* 11,94:1 */
  --text-muted:    #94a3b8;   /*  6,91:1 — plancher pour tout texte < 24px */
  --text-faint:    #7d8b9c;   /*  5,10:1 — remplace #64748b, désormais conforme AA */
  --text-deco:     #5b6b7a;   /*  3,23:1 — décoratif uniquement (icônes, ≥ 24px) */

  /* ── Accent ── */
  --accent:        #60a5fa;
  --accent-strong: #3b82f6;
  --accent-soft:   #93c5fd;
  --accent-wash:   rgba(96, 165, 250, 0.12);

  /* ── Sémantique ── */
  --ok:    #34d399;  --ok-soft:    #6ee7b7;  --ok-wash:    rgba(52, 211, 153, 0.13);
  --warn:  #fbbf24;  --warn-soft:  #fcd34d;  --warn-wash:  rgba(251, 191, 36, 0.13);
  --error: #f87171;  --error-soft: #fca5a5;  --error-wash: rgba(248, 113, 113, 0.13);

  /* ── Espacements — grille de 4 ── */
  --sp-1: 4px;   --sp-2: 8px;   --sp-3: 12px;  --sp-4: 16px;
  --sp-5: 20px;  --sp-6: 24px;  --sp-8: 32px;  --sp-10: 40px;

  /* ── Typographie — ratio 1.125 ── */
  --font: 'Inter Variable', 'Inter', 'Segoe UI', system-ui, sans-serif;
  --fs-xs:  0.75rem;   --fs-sm:  0.85rem;   --fs-base: 0.95rem;
  --fs-md:  1.05rem;   --fs-lg:  1.25rem;   --fs-xl:   1.6rem;   --fs-2xl: 2.2rem;
  --fw-normal: 400;  --fw-medium: 500;  --fw-semibold: 600;  --fw-bold: 700;

  /* ── Rayons ── */
  --r-sm: 8px;  --r-md: 12px;  --r-lg: 16px;  --r-xl: 20px;  --r-full: 999px;

  /* ── Élévation ── */
  --elev-1: 0 1px 3px rgba(0, 0, 0, 0.3);
  --elev-2: 0 8px 24px -12px rgba(0, 0, 0, 0.5);
  --elev-accent: 0 9px 22px -10px rgba(96, 165, 250, 0.6);
  --focus-ring: 0 0 0 3px rgba(96, 165, 250, 0.45);

  /* ── Verre ── */
  --blur-card: 12px;
  --blur-nav:  20px;

  /* ── Mouvement ── */
  --ease: cubic-bezier(0.4, 0, 0.2, 1);
  --dur-fast: 0.18s;  --dur-base: 0.25s;
}

/* Points de rupture retenus : 640px (mobile), 900px (tablette), 1200px (large) */

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

:focus-visible {
  outline: none;
  box-shadow: var(--focus-ring);
  border-radius: var(--r-sm);
}
```

### Composants à extraire

`Button` (principal / fantôme / danger / accepter) · `Input` · `Select` · `Textarea` ·
`Modal` · `Card` · `StatusBadge` · `Spinner` · `Avatar` · `EmptyState` · `Alert`.

Ces onze composants couvrent l'intégralité des motifs répétés dans les 4 224 lignes de CSS
actuelles, et permettraient d'en supprimer une part substantielle.
