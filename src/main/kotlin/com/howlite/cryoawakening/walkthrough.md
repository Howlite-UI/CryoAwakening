# Walkthrough : Synchronisation en Temps Réel de la Jauge du HUD AR

La jauge d'énergie du **Monocle de Bourrasque** s'actualise désormais à la perfection sur le **Gale Pipe Exhaust** en temps réel.

---

## 🛠️ Correctifs Appliqués

1. **Synchronisation Serveur -> Client Réactive ([GalePipeExhaustBlockEntity.kt](file:///c:/Users/deadd/Documents/CurseForge_ModJam\CryoAwakening\src\main\kotlin\com\howlite\cryoawakening\block\entity\GalePipeExhaustBlockEntity.kt))** :
   - Détection des variations de vent (`windStorage.wind`) à chaque tick serveur lors de l'aspiration et de la propulsion d'air.
   - Envoi immédiat des paquets de mise à jour au client (`sendBlockUpdated`), garantissant que le client dispose toujours du volume exact de vent disponible.

2. **Échantillonnage UV de la Jauge Verticale ([MonocleDataHudElement.kt](file:///c:/Users/deadd/Documents/CurseForge_ModJam\CryoAwakening\src\client\kotlin\com\howlite\cryoawakening\client\render\gui\MonocleDataHudElement.kt))** :
   - Correction de l'offset vertical `vOffset = GAUGE_HEIGHT - fillHeight` lors du rendu texturé de la jauge (`info_arm_bar_gauge.png`), assurant que la texture se remplit fluidement de bas en haut sans artefacts graphiques.

3. **Indicateur de Débit Dédié sur l'Échappement** :
   - Si la vanne est active : `§e0 §7/ §f500 §c(-10/t)`
   - Si la vanne est fermée : `§e0 §7/ §f500 §7(Closed)`

---

## 🧪 Validation
- `./gradlew build` : **BUILD SUCCESSFUL**
