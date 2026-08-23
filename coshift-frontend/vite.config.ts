import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

/**
 * Installation sur l'écran d'accueil et mise en cache de l'application.
 *
 * ## Ce que la PWA fait, et ce qu'elle ne fait pas
 *
 * Elle rend CoShift installable et démarrable hors réseau. Elle ne rend pas le
 * service utilisable hors réseau : chercher un trajet, réserver, consulter ses
 * demandes exigent le serveur. C'est une distinction qui vaut d'être tenue —
 * une application qui s'ouvre puis affiche une erreur est plus honnête qu'une
 * application qui affiche des trajets d'hier sans le dire.
 *
 * ## `prompt` et non `autoUpdate`
 *
 * Une nouvelle version n'est jamais installée sous les doigts de quelqu'un :
 * elle est annoncée, et c'est lui qui recharge. `autoUpdate` remplacerait
 * l'application à la navigation suivante, y compris au milieu d'un formulaire
 * de publication à moitié rempli. C'est aussi la réponse au risque qui avait
 * motivé de garder ce chantier pour la fin : servir une version périmée. Ici,
 * une version périmée se signale au lieu de s'installer.
 *
 * ## Rien de l'API n'est mis en cache
 *
 * Le préchargement ne porte que sur la coquille : le JavaScript, les styles,
 * les polices et les icônes. Aucune réponse de `/api` n'est conservée, et
 * c'est délibéré — une liste de trajets servie depuis un cache montrerait des
 * places déjà prises et des trajets déjà annulés. Mieux vaut une erreur de
 * réseau, qui se comprend, qu'une donnée fausse, qui ne se voit pas.
 */
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'prompt',
      includeAssets: ['favicon.svg', 'robots.txt', 'apple-touch-icon.png'],

      manifest: {
        name: 'CoShift — Covoiturage d\'entreprise',
        short_name: 'CoShift',
        description:
          "Le covoiturage entre collègues et étudiants : publiez vos trajets, réservez ceux de votre organisation.",
        lang: 'fr-BE',
        start_url: '/',
        scope: '/',
        display: 'standalone',
        /* Les deux couleurs du thème clair. Le manifeste n'en accepte qu'une,
           alors que l'application a deux thèmes : c'est la balise
           `theme-color` d'index.html, tenue à jour par le contexte de thème,
           qui suit réellement le choix de la personne. Celle-ci ne sert qu'à
           l'écran de démarrage. */
        background_color: '#F7F9FC',
        theme_color: '#F7F9FC',
        categories: ['travel', 'productivity'],
        icons: [
          { src: '/pwa-192.png', sizes: '192x192', type: 'image/png' },
          { src: '/pwa-512.png', sizes: '512x512', type: 'image/png' },
          /* Android recadre les icônes dans la forme du système et peut mordre
             jusqu'à 20 % de chaque bord. Celle-ci porte la marge nécessaire ;
             sans elle, la flèche du logo serait tronquée. */
          {
            src: '/pwa-maskable-512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable',
          },
        ],
        /* Raccourcis de l'icône, sur appui long. Les deux gestes qui amènent
           quelqu'un à ouvrir l'application. */
        shortcuts: [
          { name: 'Rechercher un trajet', short_name: 'Rechercher', url: '/trips/search' },
          { name: 'Proposer un trajet', short_name: 'Proposer', url: '/trips/create' },
        ],
      },

      workbox: {
        /* Le prechargement pese environ 2 Mo, dont 988 Ko pour `mapbox-gl`.
           C'est beaucoup pour une installation, et c'est assume : la carte est
           importee statiquement par la page d'accueil, donc elle fait partie du
           graphe de modules charge au demarrage. L'ecarter du cache rendrait
           l'application impossible a ouvrir hors reseau — le contraire du but.

           La reduire suppose de charger la carte a la demande, avec une
           frontiere d'erreur pour le cas ou le morceau manque. C'est un
           chantier a part, identifie, pas un reglage de cette configuration. */
        /* Les .webp sont les photos de la page d'accueil. Precacher la
           coquille sans elles donnerait, hors reseau, une page correcte avec
           quatre images cassees — pire que pas d'application du tout. */
        globPatterns: ['**/*.{js,css,html,svg,png,webp,woff,woff2}'],

        /* Une application à page unique répond index.html à toute adresse
           inconnue — c'est ce qui fait vivre le routeur, et la page 404. Mais
           ces chemins-là ne sont pas des pages : les servir depuis le cache
           renverrait du HTML là où l'appelant attend du JSON ou un fichier.
           Le cas se pose dès que l'API partage l'origine du site, ce qui est
           la configuration de production visée. */
        navigateFallbackDenylist: [
          /^\/api\//,
          /^\/actuator\//,
          /^\/uploads\//,
          /^\/swagger-ui/,
          /^\/v3\/api-docs/,
          /^\/sitemap\.xml$/,
        ],

        /* Aucune règle de cache d'exécution : voir l'en-tête de ce fichier.
           Le silence est ici une décision, pas un oubli. */
        runtimeCaching: [],

        cleanupOutdatedCaches: true,
      },

      /* Le service worker reste éteint en développement. Allumé, il servirait
         une version mise en cache pendant qu'on modifie le code, et chaque
         « ça ne se met pas à jour » coûterait un quart d'heure. */
      devOptions: { enabled: false },
    }),
  ],
})
