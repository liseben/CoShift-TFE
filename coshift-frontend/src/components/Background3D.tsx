import { useRef } from "react";
import { Canvas, useFrame } from "@react-three/fiber";
import { Grid } from "@react-three/drei";
import * as THREE from "three";

// Composant qui gère les "voitures" abstraites en mouvement
function MovingElements() {
  const groupRef = useRef<THREE.Group>(null);

  // useFrame s'exécute à 60 FPS. C'est ici qu'on crée l'animation.
  useFrame((_state, delta) => {
    if (groupRef.current) {
      groupRef.current.children.forEach((mesh) => {
        // Déplace l'élément vers la caméra
        mesh.position.z += delta * 15; // Vitesse

        // Si l'élément dépasse la caméra, on le remet au loin
        if (mesh.position.z > 5) {
          mesh.position.z = -40;
          // Optionnel : changer sa position X de manière aléatoire pour varier
          mesh.position.x = (Math.random() - 0.5) * 20;
        }
      });
    }
  });

  return (
    <group ref={groupRef}>
      {/* On génère 8 formes abstraites */}
      {[...Array(8)].map((_, i) => (
        <mesh
          key={i}
          position={[(Math.random() - 0.5) * 20, 0.5, -Math.random() * 40]}
        >
          {/* Forme allongée pour simuler la vitesse */}
          <boxGeometry args={[0.8, 0.2, 3]} />
          <meshStandardMaterial
            color="#00b87c"
            emissive="#00b87c"
            emissiveIntensity={1.5}
            toneMapped={false}
          />
        </mesh>
      ))}
    </group>
  );
}

export default function Background3D() {
  return (
    <div
      className="canvas-container"
      style={{
        position: "absolute",
        top: 0,
        left: 0,
        width: "100%",
        height: "100%",
        zIndex: 0,
        pointerEvents: "none",
      }}
    >
      <Canvas camera={{ position: [0, 3, 10], fov: 50 }}>
        {/* Couleur de fond identique à ton CSS pour une belle transition */}
        <color attach="background" args={["#f4f6f8"]} />

        {/* Brouillard pour cacher l'horizon et donner un effet de profondeur */}
        <fog attach="fog" args={["#f4f6f8", 15, 40]} />

        <ambientLight intensity={0.5} />
        <directionalLight position={[10, 10, 5]} intensity={1} />

        {/* Grille infinie simulant la route / le réseau */}
        <Grid
          position={[0, -0.5, 0]}
          args={[50, 50]}
          cellSize={1}
          cellThickness={1}
          cellColor="#cbd5e1"
          sectionSize={5}
          sectionThickness={1.5}
          sectionColor="#94a3b8"
          fadeDistance={40}
          fadeStrength={1}
        />

        <MovingElements />
      </Canvas>
    </div>
  );
}
