# Walkthrough : Ajout du Bloc Ecosystem Bench (Établi d'Écosystème)

Le nouveau bloc **Ecosystem Bench** a été intégré au mod avec l'ensemble de ses textures, orientations et fichiers de données.

---

## 🛠️ Composants Créés & Enregistrés

1. **Logique du Bloc** :
   - [EcosystemBenchBlock.kt](file:///c:/Users/deadd/Documents/CurseForge_ModJam\CryoAwakening\src\main\kotlin\com\howlite\cryoawakening\block\EcosystemBenchBlock.kt) : Bloc directionnel horizontal (`HORIZONTAL_FACING`) qui se place face au joueur.
   - [ModBlocks.kt](file:///c:/Users/deadd/Documents/CurseForge_ModJam\CryoAwakening\src\main\kotlin\com\howlite\cryoawakening\ModBlocks.kt) : Enregistrement de `ECOSYSTEM_BENCH` et `ECOSYSTEM_BENCH_ITEM`, ajout à l'onglet créatif du mod.

2. **Modèles & Blockstates** :
   - [blockstates/ecosystem_bench.json](file:///c:/Users/deadd/Documents/CurseForge_ModJam\CryoAwakening\src\main\resources\assets\cryo-awakening\blockstates\ecosystem_bench.json) : Gestion des rotations (North, South, East, West).
   - [models/block/ecosystem_bench.json](file:///c:/Users/deadd/Documents/CurseForge_ModJam\CryoAwakening\src\main\resources\assets\cryo-awakening\models\block\ecosystem_bench.json) : Modèle cube texturé avec les 5 textures (`front`, `back`, `side`, `top`, `bottom`).
   - [items/ecosystem_bench.json](file:///c:/Users/deadd/Documents/CurseForge_ModJam\CryoAwakening\src\main\resources\assets\cryo-awakening\items\ecosystem_bench.json) & [models/item/ecosystem_bench.json](file:///c:/Users/deadd/Documents/CurseForge_ModJam\CryoAwakening\src\main\resources\assets\cryo-awakening\models\item\ecosystem_bench.json) : Modèles d'item pour l'inventaire.

3. **Données & Traductions** :
   - [loot_table/blocks/ecosystem_bench.json](file:///c:/Users/deadd/Documents/CurseForge_ModJam\CryoAwakening\src\main\resources\data\cryo-awakening\loot_table\blocks\ecosystem_bench.json) : Table de loot (drop le bloc à la casse).
   - [tags/block/mineable/axe.json](file:///c:/Users/deadd/Documents/CurseForge_ModJam\CryoAwakening\src\main\resources\data\minecraft\tags\block\mineable\axe.json) : Minable à la hache.
   - [en_us.json](file:///c:/Users/deadd/Documents/CurseForge_ModJam\CryoAwakening\src\main\resources\assets\cryo-awakening\lang\en_us.json) : *"Ecosystem Bench"*
   - [fr_fr.json](file:///c:/Users/deadd/Documents/CurseForge_ModJam\CryoAwakening\src\main\resources\assets\cryo-awakening\lang\fr_fr.json) : *"Établi d'Écosystème"*

---

## 🧪 Validation
- `./gradlew build` : **BUILD SUCCESSFUL**
