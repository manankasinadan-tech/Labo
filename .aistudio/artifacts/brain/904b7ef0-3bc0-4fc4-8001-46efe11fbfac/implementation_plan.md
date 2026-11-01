# LABO — Plateforme d'Ingénierie & Reconstruction de ROM & GSI Android

LABO est un environnement d'ingénierie système Android conçu pour déballer, analyser, modifier, re-signer et reconstruire des images de partitions (GSI / images raw, sparse, ext4, EROFS) tout en garantissant la cohérence structurelle, les contextes SELinux, les métadonnées de permissions et la capacité de boot post-modification. L'application intègre une architecture modulaire de 11 moteurs autonomes, un écosystème d'extensions LABX extensible, une suite forensique de signature (SIGNALAB) et une console terminal universelle.

---

### Synthèse des Décisions Utilisateur

> [!IMPORTANT]
> Les choix d'architecture suivants ont été confirmés lors de la phase de cadrage et guident l'implémentation :
> - **Moteur de déballage / remballage** : Parser de flux en pur Kotlin avec chaîne d'outils binaire Android intégrée (ext4, EROFS, sparse, extraction/régénération de `fs_config` et `file_contexts`).
> - **Identité visuelle Material 3** : Style résolument **minimaliste, épuré et simple**, privilégiant la lisibilité technique, des surfaces neutres et raffinées (ardoise / graphite / titane) avec personnalisation dynamique du thème (clair, sombre, système).
> - **Extension LABX de référence préinstallée** : Extension complète **Deodexer & APK Optimizer** (module LABX complet avec manifeste TOML, moteur interne d'optimisation d'artefacts ART/DEX, interface dédiée et intégration au CORE ENGINE).
> - **Structure des espaces de travail** : Séparation stricte entre l'espace de données applicatif interne (`workspace/projects/{id}`) pour zéro risque d'altération et l'espace de sortie utilisateur sur `/sdcard/LABO/REPACKED` et `/sdcard/LABO/ROMSIGN`.
> - **Prêt pour GitHub Actions** : Ajout du workflow CI automatisé (`.github/workflows/build.yml`) pour la compilation d'APK et génération de la documentation technique intégrale ("Bible LABX").

---

## 1. Vue d'Ensemble & Spécifications Métier

LABO apporte aux développeurs Android, mainteneurs de ROMs Custom et spécialistes GSI un atelier complet sur appareil pour :
1. **Unpack & Repack GSI (FORGE)** : Décompresser une image partition système (`system.img`, `product.img`, etc.), cartographier son arborescence et générer automatiquement le dossier de configuration (`fs_config`, `file_contexts`, UID/GID, SELinux, xattrs, capacités Linux). Reconstruire l'image avec adaptation dynamique aux fichiers modifiés/ajoutés/supprimés.
2. **Gestionnaire de Projets & Fichiers Avancé** : Navigation hiérarchique avec diff visuel en temps réel (fichiers modifiés surlignés, fichiers ajoutés étiquetés, fichiers supprimés signalés par une croix rouge pour la cartographie) et explorateur de stockage `/sdcard` avec sélecteur modal intégré.
3. **Suite Cryptographique SIGNALAB** :
   - *VerifyAPP* : Audit forensique exhaustif (signatures V1/V2/V3/V4, chaînes X.509, DN, condensats SHA256/SHA1, idsig).
   - *Keymaker* : Génération de paires de clés / certificats avec modes **CloneSign** (réplication à l'identique de la structure et de la logique cible) et **Custom**.
   - *SignAPP* : Signature unitaire d'APK avec préservation d'intégrité et alignement zip.
   - *ROMsignchain* : Analyse systémique de tous les APKs d'une image déballée, extraction des graphes de confiance, classification des clés de signature partagées (shared UID, platform, media, releasekey), et génération de cartographies JSON croisées.
   - *ROM Signer* : Pipeline de bout en bout (Unpack -> Analyse de chaîne -> Re-signature globale -> Adaptation des permissions et `system/etc/permissions/` -> Régénération des métadonnées `fs-verity` et artefacts ART -> Repack complet).
4. **Console Terminal Universelle** : Composant transversal superposable (docké, demi-hauteur, plein écran), avec logs en direct de tous les moteurs, historique d'exécution, diagnostic système et support root interactif.
5. **Écosystème LABX** : Framework d'extensions autonome avec moteur d'installation (vérification d'intégrité, manifestes TOML), bus d'événements, registre de capacités et documentation complète ("Bible LABX").

---

## 2. Expérience Utilisateur & Design d'Interface

```
┌────────────────────────────────────────────────────────────────────────┐
│  LABO SYSTEM WORKBENCH                                         [●] [⚙]  │
├────┬───────────────────────────────────────────────────────────────────┤
│ ⌂  │  Accueil                                                          │
│    │  ┌─────────────────────────────────────────────────────────────┐  │
│ ⚡ │  │ ⚒ FORGE — GSI Partition Unpack & Repack Engine             │  │
│    │  ├─────────────────────────────────────────────────────────────┤  │
│ 📁 │  │ 🗂 GESTIONNAIRE — Exploration Projets & Diff Cartographique │  │
│    │  ├─────────────────────────────────────────────────────────────┤  │
│ 🔏 │  │ 🛡 SIGNALAB — Forensique Cryptographique & ROM Signer       │  │
│    │  ├─────────────────────────────────────────────────────────────┤  │
│ 🧩 │  │ ⚡ EXTENSION LABX : Deodexer & APK Optimizer                │  │
│    │  └─────────────────────────────────────────────────────────────┘  │
│ ⚙  │                                                                   │
├────┴───────────────────────────────────────────────────────────────────┤
│ [>_] TERMINAL : CoreEngine initialized. Ready for GSI operations.      │
└────────────────────────────────────────────────────────────────────────┘
```

### Navigation Globale & Ergonomie
- **Navigation Rail Latéral & Adaptatif** :
  - **Accueil** : Liste verticale des modules natifs et extensions LABX installées.
  - **Action** : Accès direct à l'opération active en arrière-plan (progression temps réel, annulation, inspection des étapes).
  - **Paramètres** : Gestion de l'apparence (thème clair/sombre/système, couleur d'accentuation), permissions stockage/root, installateur LABX et accès direct à la documentation technique.
  - **Terminal** : Déclencheur du panneau console glissant (dockable, semi-extensible ou plein écran).

### Identité Visuelle Épurée & Minimaliste
- **Palette & Contrastes** : Fond ardoise sombre velouté (`#121316`) et surfaces élevées (`#1C1E24`), contrastes nets en titane clair (`#EDEDEF`) et bordures subtiles (`#2C3038`).
- **Typographie Technique & Rigoureuse** : En-têtes nets, libellés de données en police à espacement fixe (Monospace) pour les empreintes SHA, chemins de fichiers, contextes SELinux et codes d'état.
- **Micro-interactions & Rétroactions** : Indicateurs de progression précis (décompte d'octets, nombre de fichiers traités, arbre d'analyse en cours), transitions fluides et animations de dépliement discrètes.

---

## 3. Architecture Technique des 11 Moteurs

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              CORE ENGINE                                │
│   (Orchestrateur central, Task Manager, Event Bus, Lifecycle Manager)   │
└───────┬──────────────┬──────────────┬──────────────┬──────────────┬─────┘
        │              │              │              │              │
┌───────▼──────┐┌──────▼──────┐┌──────▼──────┐┌──────▼──────┐┌──────▼─────┐
│ FILE ENGINE  ││PROJECT ENGINE││FORGE ENGINE ││FILESYSTEM  ││ PARTITION  │
│ Flux, SHA256,││ Projets, DB,││Unpack/Repack││ ext4, EROFS,││ GPT, Sparse│
│ xattrs, arbo ││ Reprise/État││  GSI/Images ││ fs_config   ││ Offsets    │
└──────────────┘└─────────────┘└─────────────┘└─────────────┘└────────────┘
        │              │              │              │              │
┌───────▼──────┐┌──────▼──────┐┌──────▼──────┐┌──────▼──────┐┌──────▼─────┐
│ANALYST ENGINE││SIGNER ENGINE││MAPPING ENGINE││ARTIFACT ENG ││ARCHIVE ENG │
│ Partitions,  ││ V1-V4, AVB, ││ Graphe déps,││ ART, odex,  ││ ZIP, TAR,  │
│ inspecteur   ││ vbmeta, key ││ impact diff ││ dex2oat     ││ GZ, XZ     │
└──────────────┘└─────────────┘└─────────────┘└─────────────┘└────────────┘
        │
┌───────▼─────────────────────────────────────────────────────────────────┐
│                    LABX FRAMEWORK & INSTALLER                           │
│ (Plugin Loader, Découverte d'Extensions, Interopérabilité Moteur-à-Moteur)│
└─────────────────────────────────────────────────────────────────────────┘
```

### Spécification des Moteurs
1. **CoreEngine** : Bus d'événements réactif Kotlin Flow, gestionnaire de files de tâches (`LaboTask`), dispatch asynchrone sécurisé, gestion centralisée des erreurs et annulations.
2. **FileEngine** : Lecture par flux des gros fichiers d'images sans engorgement mémoire, calcul de hachages cryptographiques (SHA-256, SHA-1, MD5), gestion des symlinks, des droits POSIX et des attributs étendus.
3. **ProjectEngine** : Cycle de vie des espaces de travail. Chaque Unpack initialise un dossier structuré :
   - `workspace/projects/{projectId}/extracted/` : Arborescence système extraite.
   - `workspace/projects/{projectId}/config/` : `fs_config`, `file_contexts`, `metadata.json`, `sparse_info.json`.
   - `workspace/projects/{projectId}/database/` : Base SQLite/Room du projet répertoriant chaque fichier, son hash d'origine, son hash courant, son état diff (`UNMODIFIED`, `MODIFIED`, `ADDED`, `DELETED`).
4. **AnalystEngine** : Analyse d'en-têtes magiques (Android sparse `0xED26FF3A`, ext4 superblock `0xEF53`, EROFS `0xE0F5E1E2`), détection des partitions système, tables de propriétés `build.prop`.
5. **ForgeImgEngine** : Déballage (Unpack) complet avec extraction des structures et préservation de la configuration de reconstitution ; Reconstitution (Repack) intelligente intégrant les modifications et générant l'image finale dans `/sdcard/LABO/REPACKED/`.
6. **FilesystemEngine** : Analyse et reconstruction des contextes Linux (`file_contexts`), règles de permissions (`fs_config`), calcul de taille de blocs et alignement.
7. **PartitionEngine** : Interprétation des métadonnées de partitionnement, tables dynamiques et géométrie des blocs.
8. **ArchiveEngine** : Gestion des conteneurs ZIP, TAR, GZ pour l'ingestion d'extensions LABX et de modules complémentaires.
9. **SignerEngine** :
   - Moteur forensique pour APK (détection et décodage V1 JAR signing, V2 APK Signature Scheme, V3 et V4 avec fichier `.idsig`).
   - Analyseur X.509 : extraction ASN.1 des certificats, DN émetteur/sujet, validité, clés publiques RSA/EC.
   - Générateur de clés & certificats autosignés avec support **CloneSign** (reproduction des attributs publics exacts du certificat cible) et profils personnalisés.
   - Analyseur et générateur AVB / vbmeta (descripteurs de hashtree, flags `flags=2` pour disable-verity/disable-verification).
10. **ArtifactEngine** : Analyse des fichiers compilés Android Runtime (`.odex`, `.vdex`, `.art`), détection d'obsolescence post-modification d'APK et réalignement.
11. **MappingEngine** : Construction de graphes de dépendances entre packages, bibliothèques partagées, shared user IDs et permissions système ; calcul d'impact avant recompilation.

---

## 4. Écosystème LABX & Extension Préinstallée

### Architecture LABX
- **Structure d'un paquet d'extension (`.labx`)** :
  - `manifest.toml` : Métadonnées (id, nom, version, auteur, compatibilité API LABO, permissions requises, moteurs déclarés, points d'entrée UI).
  - `config/` : Configurations par défaut.
  - `bin/` ou `scripts/` : Outils exécutables ou scripts de traitement.
  - `docs/` : Documentation intégrée de l'extension.
- **LABX Engine Installer** : Validation cryptographique du paquet, contrôle du schéma TOML, enregistrement dans le catalogue des extensions, instanciation dynamique.
- **Extension de Démonstration Incluse** :
  - Nom : **Deodexer & APK Optimizer** (ID: `org.labo.labx.deodexer`)
  - Rôle : Analyse les répertoires `/system/framework` et `/system/app`, identifie les APK odexés avec fichiers `.vdex`/`.odex` associés, et prépare la fusion dex pour faciliter la portabilité GSI.

---

## 5. Documentation Technique Intégrée ("Bible LABX")

Accessible directement depuis les **Paramètres > Documentation & Bible LABX** :
- **Architecture de LABO** : Définition des 11 moteurs, diagrammes de communication, contrats d'interface, flux de données.
- **Guide du Développeur d'Extensions LABX** :
  - Guide pas-à-pas de création d'une extension de A à Z.
  - Spécification complète du format de manifeste `manifest.toml`.
  - Utilisation des APIs d'interopérabilité pour appeler `CoreEngine`, `SignerEngine` et `ProjectEngine`.
  - Cycle de vie, gestion des erreurs et affichage d'interfaces personnalisées.
- **Spécifications GSI & Clés de Boot** : Directives pour préserver la séquence de démarrage Android (SELinux contexts, AVB flags, permissions de privilèges).

---

## 6. Automatisation CI/CD (GitHub Actions)

Création du fichier `.github/workflows/build.yml` :
- Déclenchement sur push et pull requests.
- Configuration Java 17 + Android SDK Setup.
- Compilation de l'APK de débogage (`gradle assembleDebug`).
- Exécution des tests unitaires (`gradle testDebugUnitTest`).
- Archivage des artefacts APK prêts au déploiement.

---

## Plan d'Exécution par Étapes

1. **Initialisation & Identité** :
   - Mise à jour de `metadata.json` ("LABO - GSI ROM Workbench") et `strings.xml`.
   - Configuration d'un identifiant applicatif unique (`com.aistudio.labo.workbench`).
   - Configuration du thème Material 3 minimaliste monochrome/ardoise dans `Theme.kt`, `Color.kt` et `Type.kt`.
2. **Couche Données & Modèles** :
   - Entités du Projet, Tâches (`LaboTask`), Modèles de Signature, Diff Files, Extensions LABX.
   - Base de données Room / SQLite locale pour la persistance des projets et de leurs cartographies de fichiers.
3. **Implémentation des 11 Moteurs** :
   - Architecture modulaire dans `com.example.labo.core` et `com.example.labo.engine.*`.
   - Création de `CoreEngine`, `FileEngine`, `ProjectEngine`, `ForgeImgEngine`, `FilesystemEngine`, `SignerEngine`, `AnalystEngine`, `MappingEngine`, `ArtifactEngine`, `PartitionEngine`, `ArchiveEngine`.
4. **Implémentation du Framework LABX** :
   - Moteur d'installation `LabxInstaller` et runtime `LabxFramework`.
   - Extension intégrée de démonstration *Deodexer & APK Optimizer*.
5. **Composants d'Interface Jetpack Compose** :
   - Navigation globale par Navigation Rail / Drawer adaptatif.
   - Écran Accueil avec tuiles de modules dynamiques (Forge, Signalab, Gestionnaire, Extensions).
   - Écran FORGE (Unpack & Repack avec sélecteur de projets et barre d'état).
   - Écran GESTIONNAIRE DE FICHIERS (Mode Projet avec diff markers et Mode Stockage avec popup modal réutilisable).
   - Écran SIGNALAB (VerifyAPP, Keymaker, SignAPP, ROMsignchain, ROM Signer pipeline complet).
   - Console TERMINAL universelle (collapsible, live logs, diagnostic interactif).
   - Écran Paramètres avec documentation interactive "Bible LABX" et sélecteur de thèmes.
6. **Workflow GitHub Actions & Documentation Markdown** :
   - Fichier `.github/workflows/build.yml` pour build continu.
   - Fichier de documentation technique complet embarqué dans l'application.
7. **Vérification & Compilation** :
   - Test de compilation avec `compile_applet`.
   - Vérification de la conformité du code et validation de l'exécution.
