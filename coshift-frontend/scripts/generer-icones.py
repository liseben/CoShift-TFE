# -*- coding: utf-8 -*-
"""Produit les icones PNG de la PWA a partir du logo CoShift.

POURQUOI UN SCRIPT PLUTOT QUE DES FICHIERS DESSINES A LA MAIN
Le logo vit dans `src/components/Logo/Logo.tsx` et dans `public/favicon.svg`.
Des icones dessinees separement s'en detacheraient a la premiere retouche, et
personne ne s'en apercevrait : une icone d'ecran d'accueil ne se regarde pas
deux fois. Ce script rejoue les memes traces, avec les memes points de
controle, si bien qu'une modification du logo se repercute en une commande.

POURQUOI PAS UN RENDU DU SVG
Rasteriser un SVG demanderait cairosvg ou un navigateur sans affichage — une
dependance de plus dans un projet dont l'inventaire des licences est publie.
Les traces sont assez simples pour etre rejoues directement.

COMMENT LE TRAIT EST OBTENU
PIL n'anticrenelle pas les lignes, et `ImageDraw.line(joint="curve")` laisse
des stries la ou les segments se recouvrent. Chaque trace est donc dessine en
tamponnant un disque le long du chemin, dans un masque en niveaux de gris ; le
masque sert ensuite a poser la couleur. L'opacite est appliquee au masque
entier plutot qu'a chaque tampon : sinon les recouvrements s'assombriraient.

USAGE
    python scripts/generer-icones.py

Sortie : public/pwa-192.png, pwa-512.png, pwa-maskable-512.png,
         apple-touch-icon.png
"""
import os
from PIL import Image, ImageDraw

# Le logo est dessine dans un carre de 48 unites, comme le SVG.
BOITE = 48.0

# Rendu a haute resolution puis reduction : la reduction Lanczos depuis quatre
# fois la taille finale donne des bords aussi nets qu'un rendu vectoriel.
MAITRE = 2048

BRAND = (29, 78, 216)      # --brand, theme clair
ECO = (15, 122, 61)        # --eco, theme clair
FOND = (255, 255, 255)

TRAIT = 4.5                # stroke-width du SVG
RAYON_POINT = 3.2
ESTOMPE = 140              # opacite du second trajet (0.55 dans le SVG)


def bezier(p0, p1, p2, p3, pas=400):
    """Points d'une courbe cubique, echantillonnee."""
    points = []
    for i in range(pas + 1):
        t = i / pas
        u = 1 - t
        x = u * u * u * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t * t * t * p3[0]
        y = u * u * u * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t * t * t * p3[1]
        points.append((x, y))
    return points


def segment(a, b, pas=400):
    return [(a[0] + (b[0] - a[0]) * i / pas, a[1] + (b[1] - a[1]) * i / pas)
            for i in range(pas + 1)]


def dessiner(taille, marge_relative=0.0):
    """Rend le logo sur un fond opaque.

    `marge_relative` reserve une bordure vide autour du trace. Les icones
    « maskable » sont recadrees par le systeme, qui peut mordre jusqu'a 20 % de
    chaque bord : sans marge, la fleche et les points seraient rognes.
    """
    img = Image.new("RGB", (MAITRE, MAITRE), FOND)

    dessin = MAITRE * (1 - 2 * marge_relative)
    echelle = dessin / BOITE
    decalage = MAITRE * marge_relative

    def P(x, y):
        return (decalage + x * echelle, decalage + y * echelle)

    rayon_trait = TRAIT * echelle / 2

    def poser(chemins, couleur, opacite=255):
        """Tamponne un disque le long des chemins, puis pose la couleur.

        `chemins` est une liste de couples (points, rayon). Tout ce qui partage
        une meme opacite doit passer dans un seul appel : deux appels estompes
        qui se recouvrent composeraient leurs alphas et laisseraient une auréole
        la ou le point rejoint sa courbe.
        """
        masque = Image.new("L", (MAITRE, MAITRE), 0)
        d = ImageDraw.Draw(masque)
        for points, r in chemins:
            for x, y in points:
                cx, cy = P(x, y)
                d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=255)
        if opacite != 255:
            masque = masque.point(lambda v: v * opacite // 255)
        img.paste(couleur, (0, 0), masque)

    rayon_point = RAYON_POINT * echelle

    # Premier trajet, plein, avec son point de depart.
    poser([(bezier((8, 11), (19, 11), (21, 24), (29, 24)), rayon_trait),
           ([(8, 11)], rayon_point)], BRAND)

    # Second trajet, estompe. Courbe et point dans le meme masque.
    poser([(bezier((8, 37), (19, 37), (21, 24), (29, 24)), rayon_trait),
           ([(8, 37)], rayon_point)], BRAND, opacite=ESTOMPE)

    # Le tronc commun et sa fleche, en vert : c'est lui qui porte le gain.
    poser([(segment((29, 24), (41, 24)), rayon_trait),
           (segment((35.5, 18.5), (41, 24)), rayon_trait),
           (segment((41, 24), (35.5, 29.5)), rayon_trait)], ECO)

    return img.resize((taille, taille), Image.LANCZOS)


if __name__ == "__main__":
    racine = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "public")
    sorties = [
        ("pwa-192.png", 192, 0.10),
        ("pwa-512.png", 512, 0.10),
        ("pwa-maskable-512.png", 512, 0.22),
        # iOS applique son propre arrondi et ignore la transparence.
        ("apple-touch-icon.png", 180, 0.12),
    ]
    for nom, taille, marge in sorties:
        chemin = os.path.join(racine, nom)
        dessiner(taille, marge).save(chemin, "PNG", optimize=True)
        print("%-24s %4d px  %6d octets" % (nom, taille, os.path.getsize(chemin)))
