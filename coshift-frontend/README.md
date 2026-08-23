# CoShift — interface

Client React de la plateforme CoShift. Il ne fonctionne pas seul : il consomme l'API du module `coshift-backend`.

**La procédure d'installation complète se trouve dans le [README à la racine](../README.md).**

## Commandes

| Commande | Effet |
|---|---|
| `npm run dev` | Serveur de développement sur `http://localhost:5173` |
| `npm run build` | Vérification des types puis construction dans `dist/` |
| `npm run preview` | Sert le résultat de `build` |
| `npm run lint` | Analyse statique |

## Configuration

Copier `.env.example` en `.env`, puis renseigner :

- `VITE_API_URL` — URL du backend, doit suivre son `SERVER_PORT`
- `VITE_MAPBOX_TOKEN` — jeton public Mapbox pour le fond cartographique

Vite n'expose au navigateur que les variables préfixées `VITE_`. Elles finissent en clair dans le paquet livré : n'y placez jamais de secret.

## Organisation

```
src/
  components/   Composants réutilisables, dont la bibliothèque d'interface (ui/)
  context/      Authentification, langue, thème, consentement
  hooks/        Crochets partagés
  i18n/         Catalogues français et anglais
  layouts/      Coquille de page
  pages/        Un dossier par écran
  styles/       Jetons de conception, base, composants
```

La planche de composants est servie sur `/styleguide` : elle sert de référence visuelle et de support au rapport.
