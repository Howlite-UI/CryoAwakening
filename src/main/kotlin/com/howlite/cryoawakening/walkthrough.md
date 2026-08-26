# Walkthrough : Implémentation de la Breeze Foundry (Fonderie de Bourrasque)

La **Breeze Foundry** ainsi que son interface graphique complète ont été implémentées pour permettre la création d'alliages de **Tellurobismuthite Ingot** à partir de **Raw Bismuth** et **Raw Tellurium** propulsés par l'énergie Vent.

---

## 🛠️ Composants Réalisés

### 1. Bloc & Modèle 3D
- [BreezeFoundryBlock.kt](file:///c:/Users/deadd/Documents/CurseForge_ModJam/CryoAwakening/src/main/kotlin/com/howlite/cryoawakening/block/BreezeFoundryBlock.kt) :
  - Bloc orientable (`FACING`) avec sons métalliques.
  - Clic droit pour ouvrir l'interface de fonderie.
  - Restitution automatique des objets au sol si le bloc est brisé (`playerWillDestroy`).
- [breeze_foundry.json (block)](file:///c:/Users/deadd/Documents/CurseForge_ModJam/CryoAwakening/src/main/resources/assets/cryo-awakening/models/block/breeze_foundry.json) & [blockstates/breeze_foundry.json](file:///c:/Users/deadd/Documents/CurseForge_ModJam/CryoAwakening/src/main/resources/assets/cryo-awakening/blockstates/breeze_foundry.json) :
  - Utilise les textures dédiées : `breeze_foundry_front.png`, `breeze_foundry_side.png`, `breeze_foundry_top.png`, `breeze_foundry_back.png`.

### 2. Logique & Fusion d'Alliage
- [BreezeFoundryBlockEntity.kt](file:///c:/Users/deadd/Documents/CurseForge_ModJam/CryoAwakening/src/main/kotlin/com/howlite/cryoawakening/block/entity/BreezeFoundryBlockEntity.kt) :
  - Stockage interne d'énergie vent : **10 000 V**.
  - **4 slots d'inventaire** :
    - Slot 0 : Entrée A (Raw Bismuth)
    - Slot 1 : Entrée B (Raw Tellurium)
    - Slot 2 : Carburant / Catalyseur
    - Slot 3 : Sortie (Lingot de Tellurobismuthite)
  - Fusion d'alliage : consomme 1 Raw Bismuth + 1 Raw Tellurium + ~50 V de Vent en 5 secondes (100 ticks) pour forger 1 Lingot de Tellurobismuthite.
  - Support de l'automatisation (Entonnoirs / Hoppers) : Entrée par le dessus/côtés, extraction du produit fini par le dessous.

### 3. Interface Graphique (GUI) & Screen
- [BreezeFoundryMenu.kt](file:///c:/Users/deadd/Documents/CurseForge_ModJam/CryoAwakening/src/main/kotlin/com/howlite/cryoawakening/screen/BreezeFoundryMenu.kt) :
  - Alignement précis des slots sur la texture `breeze_foundry_gui.png`.
  - Gestion fluide du shift-click (`quickMoveStack`).
- [BreezeFoundryScreen.kt](file:///c:/Users/deadd/Documents/CurseForge_ModJam/CryoAwakening/src/client/kotlin/com/howlite/cryoawakening/client/render/gui/BreezeFoundryScreen.kt) :
  - Rendu de la jauge de Vent avec `breeze_foundry_bar_gauge.png` qui monte verticalement avec le niveau d'énergie.
  - Flèche de progression de fusion cyan avec scintillement lumineux.
  - Flamme / turbine animée pulsante lorsque la machine est active.
  - Tooltip dynamique indiquant le niveau d'énergie Vent au survol de la jauge.

### 4. Réseau & Réalité Augmentée Monocle
- [GalePipeBlock.kt](file:///c:/Users/deadd/Documents/CurseForge_ModJam/CryoAwakening/src/main/kotlin/com/howlite/cryoawakening/block/GalePipeBlock.kt) : Se connecte directement à la fonderie pour lui alimenter du vent.
- [MonocleDataHudElement.kt](file:///c:/Users/deadd/Documents/CurseForge_ModJam/CryoAwakening/src/client/kotlin/com/howlite/cryoawakening/client/render/gui/MonocleDataHudElement.kt) : Affiche en temps réel le nom `breeze foundry` et la charge en Vent via le monocle AR.

---

## 🧪 Validation & Compilation
- `./gradlew build` : **BUILD SUCCESSFUL**
