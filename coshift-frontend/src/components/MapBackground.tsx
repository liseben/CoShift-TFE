import { useEffect, useRef, useMemo } from "react";
// 1. On importe MapRef
import Map, { Source, Layer } from "react-map-gl";
import type { MapRef } from "react-map-gl";
import "mapbox-gl/dist/mapbox-gl.css";

// @ts-ignore
import * as turf from "@turf/turf";

const SIBELGA_TEAL = "#00B4C5";
const SIBELGA_NAVY = "#003354";
/* Fond du bloc de carte. Repris du jeton applicatif : la charte est
   claire, un bleu nuit ferait tache autour de la carte. */
const MAP_SURROUND = "var(--surface-sunk)";

const routeCoordinates = [
  [4.3517, 50.8503],
  [4.36, 50.84],
  [4.37, 50.83],
  [4.3815, 50.8118],
];

const routeGeoJSON = turf.lineString(routeCoordinates);
const routeLength = turf.length(routeGeoJSON, { units: "kilometers" });

export default function MapBackground() {
  // 2. On utilise une référence (qui ne déclenche AUCUN re-rendu React)
  const mapRef = useRef<MapRef>(null);

  useEffect(() => {
    let start: number;
    let animationFrameId: number;
    const duration = 5000;
    let currentBearing = 0; // On stocke la rotation ici, hors de React

    const animate = (timestamp: number) => {
      if (!start) start = timestamp;
      const progress = (timestamp - start) / duration;

      // 3. On récupère le moteur Mapbox brut
      const map = mapRef.current?.getMap();

      // On s'assure que la carte a fini de charger avant d'animer
      if (map && map.isStyleLoaded()) {
        // A. On tourne la caméra directement dans Mapbox
        currentBearing += 0.15;
        map.setBearing(currentBearing);

        // B. On fait avancer la voiture directement dans Mapbox
        if (progress < 1) {
          const distance = progress * routeLength;
          const newPoint = turf.along(routeGeoJSON, distance, {
            units: "kilometers",
          });

          // On injecte les nouvelles coordonnées à la volée (casté en any pour éviter les soucis TypeScript)
          const source = map.getSource("car") as any;
          if (source) {
            source.setData(newPoint);
          }
        } else {
          start = timestamp;
        }
      }

      animationFrameId = requestAnimationFrame(animate);
    };

    animationFrameId = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(animationFrameId);
  }, []);

  const lineLayer = useMemo(
    () => ({
      id: "route-line",
      type: "line" as const,
      paint: {
        "line-color": SIBELGA_TEAL,
        "line-width": 6,
        "line-opacity": 0.9,
      },
    }),
    [],
  );

  const carLayer = useMemo(
    () => ({
      id: "car-point",
      type: "circle" as const,
      paint: {
        "circle-radius": 15,
        "circle-color": "#ffffff",
        "circle-stroke-width": 4,
        "circle-stroke-color": SIBELGA_TEAL,
        "circle-pitch-alignment": "map" as const,
      },
    }),
    [],
  );

  const building3dLayer = useMemo(
    () => ({
      id: "3d-buildings",
      source: "composite",
      "source-layer": "building",
      filter: ["==", "extrude", "true"],
      type: "fill-extrusion" as const,
      minzoom: 12,
      paint: {
        "fill-extrusion-color": SIBELGA_NAVY,
        "fill-extrusion-height": ["get", "height"],
        "fill-extrusion-base": ["get", "min_height"],
        "fill-extrusion-opacity": 0.4,
      },
    }),
    [],
  );

  return (
    <div
      style={{
        position: "absolute",
        top: 0,
        left: 0,
        width: "100%",
        height: "100%",
        backgroundColor: MAP_SURROUND,
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        zIndex: 0,
        overflow: "hidden",
      }}
    >
      <div
        style={{
          width: "85%",
          height: "75%",
          borderRadius: "var(--r-xl)",
          overflow: "hidden",
          boxShadow: "var(--elev-3)",
          border: "1px solid var(--border)",
          position: "relative",
        }}
      >
        <Map
          ref={mapRef} // 👈 On connecte notre référence ici
          initialViewState={{
            // 👈 On utilise initialViewState au lieu des states
            longitude: 4.365,
            latitude: 50.83,
            zoom: 16,
            pitch: 70,
            bearing: 0,
          }}
          mapStyle="mapbox://styles/mapbox/light-v11"
          mapboxAccessToken={import.meta.env.VITE_MAPBOX_TOKEN}
          interactive={false}
          // On désactive les rendus dupliqués du monde pour alléger la carte graphique
          renderWorldCopies={false}
        >
          <Layer {...(building3dLayer as any)} />

          <Source id="route" type="geojson" data={routeGeoJSON}>
            <Layer {...lineLayer} />
          </Source>

          {/* On fixe le point de départ en dur, l'animation s'occupe du reste */}
          <Source
            id="car"
            type="geojson"
            data={turf.point(routeCoordinates[0])}
          >
            <Layer {...carLayer} />
          </Source>
        </Map>
      </div>
    </div>
  );
}
