import { useEffect, useState, useMemo } from "react";
import Map, { Source, Layer } from "react-map-gl";
import "mapbox-gl/dist/mapbox-gl.css";
import * as turf from "@turf/turf";

// --- COULEURS SIBELGA ---
const SIBELGA_TEAL = "#00B4C5";
const SIBELGA_NAVY = "#003354";
const SIBELGA_BG = "#001b2e";

const routeCoordinates = [
  [4.3517, 50.8503],
  [4.36, 50.84],
  [4.37, 50.83],
  [4.3815, 50.8118],
];

const routeGeoJSON = turf.lineString(routeCoordinates);
const routeLength = turf.length(routeGeoJSON, { units: "kilometers" });

export default function MapBackground() {
  const [carPosition, setCarPosition] = useState<
    GeoJSON.Feature<GeoJSON.Point>
  >(turf.point(routeCoordinates[0]));

  const [viewState, setViewState] = useState({
    longitude: 4.365,
    latitude: 50.83,
    zoom: 16,
    pitch: 70,
    bearing: 0,
  });

  useEffect(() => {
    let start: number;
    let animationFrameId: number;
    const duration = 5000;

    const animate = (timestamp: number) => {
      if (!start) start = timestamp;
      const progress = (timestamp - start) / duration;

      // Rotation continue de la caméra
      setViewState((prev) => ({
        ...prev,
        bearing: prev.bearing + 0.15,
      }));

      // Avancée de la voiture
      if (progress < 1) {
        const distance = progress * routeLength;
        const newPoint = turf.along(routeGeoJSON, distance, {
          units: "kilometers",
        });
        setCarPosition(newPoint);
      } else {
        start = timestamp;
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
        "circle-pitch-alignment": "map",
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
        backgroundColor: SIBELGA_BG, // Le fond profond derrière la carte
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        zIndex: 0,
        overflow: "hidden",
      }}
    >
      {/* LE CONTENEUR DÉTACHÉ */}
      <div
        style={{
          width: "85%", // Largeur de la carte (ajustable)
          height: "75%", // Hauteur de la carte (ajustable)
          borderRadius: "24px", // Bords très arrondis pour l'effet moderne
          overflow: "hidden", // Pour que la map respecte l'arrondi
          boxShadow: "0 25px 50px -12px rgba(0, 0, 0, 0.5)", // Ombre profonde pour le relief
          border: `1px solid rgba(255, 255, 255, 0.05)`, // Fine bordure pour détacher du noir
          position: "relative",
        }}
      >
        <Map
          {...viewState}
          onMove={(evt) => setViewState(evt.viewState)}
          mapStyle="mapbox://styles/mapbox/dark-v11"
          mapboxAccessToken={import.meta.env.VITE_MAPBOX_TOKEN}
          interactive={false}
          projection="mercator"
          renderWorldCopies={false}
        >
          <Layer {...building3dLayer} />

          <Source id="route" type="geojson" data={routeGeoJSON}>
            <Layer {...lineLayer} />
          </Source>

          <Source id="car" type="geojson" data={carPosition}>
            <Layer {...carLayer} />
          </Source>
        </Map>
      </div>
    </div>
  );
}
