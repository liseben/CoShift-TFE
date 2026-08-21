/**
 * Photographies du site.
 *
 * Les fichiers sources pesaient de 254 Ko à 2,4 Mo. Ils sont convertis en
 * WebP et redimensionnés à la taille réellement affichée : 6,4 Mo au total
 * sont devenus 430 Ko, sans différence visible à l'écran.
 *
 * Chaque image porte un texte alternatif écrit ici, une bonne fois : il
 * décrit ce que montre la photo, pas le nom du fichier.
 *
 * PROVENANCE — à vérifier avant toute mise en ligne publique :
 *   · covoitureurs-habitacle, depart-covoiturage : générées par IA
 *   · passagere-rendez-vous : photographie de banque d'images, licence à
 *     confirmer
 *   · application-en-main : voir la note en fin de fichier
 */

import covoitureursHabitacle from "./covoitureurs-habitacle.webp";
import departCovoiturage from "./depart-covoiturage.webp";
import passagereRendezVous from "./passagere-rendez-vous.webp";
import applicationEnMain from "./application-en-main.webp";

export type Photo = {
  src: string;
  /**
   * Clé de traduction du texte alternatif.
   *
   * <p>Une chaîne écrite ici serait figée en français : ce module est évalué
   * au chargement, avant que la langue soit connue. Un texte alternatif est
   * pourtant le seul contenu dont dispose qui ne voit pas l'image — le laisser
   * dans une langue qui n'est pas celle de la page le rend inutilisable.</p>
   */
  alt: string;
  /** Rapport largeur/hauteur, à réserver en CSS pour éviter le saut de page. */
  ratio: string;
};

export const PHOTOS = {
  habitacle: {
    src: covoitureursHabitacle,
    alt: "photos.habitacle",
    ratio: "2 / 3",
  },
  depart: {
    src: departCovoiturage,
    alt: "photos.depart",
    ratio: "2 / 3",
  },
  rendezVous: {
    src: passagereRendezVous,
    alt: "photos.rendezVous",
    ratio: "3 / 2",
  },
  application: {
    src: applicationEnMain,
    alt: "photos.application",
    ratio: "3 / 2",
  },
} satisfies Record<string, Photo>;

/*
 * NOTE SUR « application-en-main »
 *
 * Cette image est conservée dans le dossier mais n'est placée sur aucune
 * page, pour deux raisons :
 *
 * 1. Le texte affiché sur l'écran du téléphone est fautif — « Aŕriede »
 *    pour « Arrivée », « Rejonnée le covoitrage » pour « Rejoindre le
 *    covoiturage », « La Défénce » pour « La Défense ». Sur un livrable
 *    évalué, ces fautes se voient au premier coup d'œil.
 *
 * 2. Elle dérive d'une photographie de presse montrant à l'origine
 *    l'application d'un concurrent, dont seul le logo a été remplacé. La
 *    photo d'origine reste protégée par le droit d'auteur.
 *
 * Une capture réelle de l'application, insérée dans un cadre de téléphone,
 * résoudrait les deux problèmes d'un coup — et montrerait le vrai produit.
 */
