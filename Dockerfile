# =============================================================================
#  CoShift — image de production à une seule URL
# =============================================================================
#  Trois étapes : construire l'interface, construire le serveur en y embarquant
#  l'interface, puis ne garder que l'exécutable. L'image finale ne contient ni
#  Node, ni Maven, ni les sources — uniquement un JRE et le .jar.
#
#  Le frontend est construit avec VITE_API_URL vide : servi par le backend,
#  il appelle l'API en chemin relatif (/api/…), même origine, donc pas de CORS.
# =============================================================================

# ─── 1. Interface ─────────────────────────────────────────────────────────────
FROM node:22-alpine AS interface
WORKDIR /interface
COPY coshift-frontend/package.json coshift-frontend/package-lock.json ./
RUN npm ci
COPY coshift-frontend/ ./
# L'URL de l'API est vide : même origine que la page. Le .env local, s'il est
# présent, est écarté par .dockerignore — il pointerait vers localhost:8081.
ENV VITE_API_URL=
RUN npm run build

# ─── 2. Serveur ───────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk AS serveur
WORKDIR /app
COPY coshift-backend/ ./
# L'interface construite devient une ressource statique du serveur.
COPY --from=interface /interface/dist/ src/main/resources/static/
# Le dépôt vient parfois d'un poste Windows : les fins de ligne CRLF font
# échouer le script mvnw sous /bin/sh avant même que Maven ne démarre.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
# Les tests ont leur place en intégration continue, pas dans la construction
# d'une image : ici ils rallongeraient chaque déploiement de plusieurs minutes.
RUN ./mvnw -q package -DskipTests

# ─── 3. Exécution ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=serveur /app/target/*.jar app.jar
# Railway impose son port par la variable PORT ; à défaut, 8080.
# MaxRAMPercentage borne la JVM à la mémoire du conteneur au lieu de celle
# de la machine hôte — sans quoi elle se croit riche et se fait tuer.
# preferIPv6Addresses : le réseau privé de Railway (…railway.internal) ne
# publie que des adresses IPv6 ; sans cette préférence, la JVM peut tenter
# l'IPv4 et échouer en « Communications link failure » vers la base.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Djava.net.preferIPv6Addresses=true"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]
