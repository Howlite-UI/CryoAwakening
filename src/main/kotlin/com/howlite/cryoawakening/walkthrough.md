# Walkthrough : Véritable Spirale Hélicoïdale 3D dans le Gale Tank

Les particules `STYLIZED_WIND` dans le **Gale Tank** suivent désormais une trajectoire hélicoïdale (spirale 3D) enroulée autour du cylindre intérieur.

---

## 🌀 Forme de la Spirale Hélicoïdale 3D

1. **Enroulement Hélicoïdal (2 à 3 boucles complètes) :**
   - L'angle de rotation s'incrémente de `0.42` à `0.85` radians par tick au fur et à mesure que la particule s'élève.
   - La traînée forme ainsi 2 à 3 boucles spiralées nettes et visibles du bas vers le haut du réservoir.
2. **Évasement Progressif en Entonnoir :**
   - Rayon de départ serré au centre (`0.12` bloc) s'élargissant progressivement jusqu'à `0.30` bloc au sommet (bien contenu dans le verre).
3. **Accélération avec le Niveau de Vent :**
   - Plus le tank se remplit, plus la vitesse d'ascension et d'enroulement angulaire s'accélère.

---

## 🧪 Validation
- `./gradlew build` : **BUILD SUCCESSFUL in 12s**
