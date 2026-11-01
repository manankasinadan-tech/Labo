# BIBLE TECHNIQUE LABO & GUIDE DE DÉVELOPPEMENT D'EXTENSIONS LABX

> **Version** : 1.0.0 — Document de référence d'ingénierie système ROM & GSI Android
> **Auteur** : Équipe d'Ingénierie LABO

---

## Sommaire

1. [Architecture Fondamentale de LABO](#1-architecture-fondamentale-de-labo)
2. [Les 11 Moteurs Autonomes](#2-les-11-moteurs-autonomes)
3. [L'Écosystème LABX : Philosophie & Conception](#3-lécosystème-labx--philosophie--conception)
4. [Structure Normalisée d'un Paquet d'Extension .labx](#4-structure-normalisée-dun-paquet-dextension-labx)
5. [Spécification Complète du Manifeste TOML](#5-spécification-complète-du-manifeste-toml)
6. [Guide Pas-à-Pas : Créer votre Première Extension LABX](#6-guide-pas-à-pas--créer-votre-première-extension-labx)
7. [Interopérabilité avec les Moteurs LABO](#7-interopérabilité-avec-les-moteurs-labo)
8. [Ingénierie GSI & Rétablissement du Boot](#8-ingénierie-gsi--rétablissement-du-boot)
9. [Directives de Sécurité, Permissions & Bonnes Pratiques](#9-directives-de-sécurité-permissions--bonnes-pratiques)

---

## 1. Architecture Fondamentale de LABO

LABO repose sur une séparation stricte entre :
- **L'Interface Utilisateur (UI Layer)** : Conçue en Jetpack Compose (Material 3), elle écoute les états émis par les moteurs via des flux réactifs (`StateFlow`) sans exécuter elle-même de logique métier.
- **Le Cœur de Coordination (Core Engine)** : Supervise le cycle de vie, la file de tâches asynchrones, le journal d'événements et le bus de messages.
- **Les Moteurs Autonomes (Engine Layer)** : Composants métier indépendants capables de s'exécuter en arrière-plan sans dépendre du contexte graphique.
- **L'Écosystème LABX (Extensibility Layer)** : Permet à des tiers d'injecter de nouveaux moteurs, fonctionnalités et panneaux d'administration.

### Isolation Physique des Données
- **Espace de Travail Isolé** : `context.filesDir/workspace/projects/{projectId}/`
  - Empêche tout échec d'écriture ou corruption causée par les restrictions de stockage scoped storage d'Android.
  - Structure dédiée : `extracted/` (arborescence du système de fichiers), `config/` (métadonnées brutes de reconstruction), `database/` (base SQLite et cartographie).
- **Espace d'Exportation Externe** :
  - `/sdcard/LABO/REPACKED/` : Destination des images GSI reconstruites par FORGE.
  - `/sdcard/LABO/ROMSIGN/` : Destination des images entièrement re-signées par SIGNALAB.

---

## 2. Les 11 Moteurs Autonomes

### 1. CORE ENGINE
- **Rôle** : Tour de contrôle de LABO.
- **Responsabilités** :
  - Enregistre et supervise les moteurs disponibles (internes et LABX).
  - Gère les files d'attente prioritaires des tâches asynchrones.
  - Diffuse les événements via le bus réactif (`CoreEventBus`).
  - Route les flux de logs vers le Terminal Universel.

### 2. FILE ENGINE
- **Rôle** : Gestionnaire de flux, d'arborescences et d'attributs de fichiers.
- **Responsabilités** :
  - Lecture et écriture séquentielles évitant la saturation de la RAM lors du traitement d'images multi-gigaoctets.
  - Calcul cryptographique d'empreintes (SHA-256, SHA-1, MD5).
  - Conservation des liens symboliques (symlinks) et des attributs étendus (xattrs).

### 3. PROJECT ENGINE
- **Rôle** : Cycle de vie des projets de déballage et de modification.
- **Responsabilités** :
  - Création automatique d'un projet identifié par UUID lors de chaque opération Unpack.
  - Historique et reprise automatique d'un projet interrompu.
  - Suivi des statuts de fichiers (`UNMODIFIED`, `MODIFIED`, `ADDED`, `DELETED`).

### 4. ANALYST ENGINE
- **Rôle** : Analyse technique non destructive.
- **Responsabilités** :
  - Identification des nombres magiques : Android Sparse Image (`0xED26FF3A`), ext4 (`0xEF53`), EROFS (`0xE0F5E1E2`), bootimg (`ANDROID!`).
  - Parsing des propriétés système `build.prop` (`ro.build.version.release`, `ro.product.model`, `ro.system.build.fingerprint`).
  - Cartographie des partitions présentes.

### 5. FORGE IMG ENGINE
- **Rôle** : Déballage (Unpack) et Reconstitution (Repack) de partitions Android.
- **Responsabilités** :
  - Unpack : Décompression des blocs, extraction dans l'espace de travail et génération automatique de `config/fs_config`, `config/file_contexts`, `config/sparse_info.json`.
  - Repack : Détection différentielle des fichiers modifiés, mise à jour dynamique des permissions, réalignement des blocs de données, recalcul de la taille du système de fichiers et écriture de la nouvelle image dans `/sdcard/LABO/REPACKED/`.

### 6. FILESYSTEM ENGINE
- **Rôle** : Gestion avancée des systèmes de fichiers ext4 et EROFS.
- **Responsabilités** :
  - Analyse des inodes, superblocs et structures de blocs.
  - Lecture et synthèse des tables de permissions POSIX (`fs_config`) avec UID, GID et masques octaux.
  - Traitement des expressions régulières de labellisation SELinux (`file_contexts`).

### 7. PARTITION ENGINE
- **Rôle** : Gestion de la structure partitionnelle.
- **Responsabilités** :
  - Lecture des tables de partitionnement GPT (GUID Partition Table).
  - Détection des conteneurs dynamiques Android (`super.img`, sous-partitions logical dynamic partitions).
  - Validation des alignements et limites de blocs pour éviter tout chevauchement.

### 8. ARCHIVE ENGINE
- **Rôle** : Compression et conteneurs d'archives.
- **Responsabilités** :
  - Prise en charge des formats ZIP, TAR, GZ, XZ.
  - Décompression avec conservation de l'arborescence et extraction sélective pour l'ingestion d'extensions LABX.

### 9. SIGNER ENGINE
- **Rôle** : Forensique de signature, cryptographie X.509 et chaînes de confiance AVB.
- **Responsabilités** :
  - Analyse approfondie des schémas de signature APK :
    - V1 : JAR signing (META-INF/MANIFEST.MF, CERT.SF, CERT.RSA).
    - V2 : APK Signature Scheme v2 (bloc ID `0x7109871a`).
    - V3 : APK Signature Scheme v3 (bloc ID `0xf3436ccd`, rotation de clés).
    - V4 : Signature externe streaming (`.idsig`).
  - Extraction complète des certificats X.509 : Distinguished Names (CN, OU, O, L, ST, C), numéros de série, dates d'expiration, empreintes SHA-256.
  - **Keymaker** : Création de paires de clés (RSA 2048/4096, EC P-256) et génération de certificats autosignés.
    - Mode **CloneSign** : Reproduction exacte de la structure, des DN, de l'algorithme et de la taille de clé d'une cible donnée.
    - Mode **Custom** : Configuration sur mesure des paramètres d'identité.
  - **ROMsignchain** : Cartographie globale de tous les APKs d'une ROM déballée, regroupement croisé par sharedUserId, certificats partagés (platform, shared, media, testkey) et export structuré en JSON.
  - **ROM Signer** : Pipeline global automatisé de re-signature de tous les composants avec recalcul des hash AVB hashtree.

### 10. ARTIFACT ENGINE
- **Rôle** : Gestion des artefacts Android Runtime (ART) et intégrité fs-verity.
- **Responsabilités** :
  - Détection et parsing des fichiers `.odex`, `.vdex` et `.art`.
  - Repérage des artefacts obsolètes lors de la modification ou du remplacement d'un APK.
  - Préparation au deodexing (réinsertion du fichier `classes.dex` à l'intérieur de l'APK pour restaurer la portabilité).

### 11. MAPPING ENGINE
- **Rôle** : Cartographie relationnelle et analyse d'impact.
- **Responsabilités** :
  - Construction du graphe de dépendances (applications <-> bibliothèques dynamiques `.so` <-> services système).
  - Détection préventive des ruptures de boot lors de la suppression d'un composant système critique.

---

## 3. L'Écosystème LABX : Philosophie & Conception

Une extension **LABX** n'est pas un simple script d'automatisation. C'est un sous-système autonome capable de :
1. Déclarer ses propres métadonnées techniques et exigences de version.
2. Embarquer son propre moteur spécialisé capable d'interagir avec les 11 moteurs de LABO.
3. Définir une interface graphique dédiée intégrée dans l'Accueil de LABO.
4. Consulter et modifier de façon sécurisée les données des projets déballés via des contrats d'interface stricts.

---

## 4. Structure Normalisée d'un Paquet d'Extension .labx

Un paquet `.labx` est une archive compressée dont l'extension est `.labx` (ou `.zip`), organisée comme suit :

```
mon_extension.labx/
├── manifest.toml         # [OBLIGATOIRE] Manifeste d'identité et de déclaration
├── icon.png              # [OPTIONNEL] Icône carrée au format PNG (192x192)
├── config/               # [OPTIONNEL] Fichiers de configuration par défaut
│   └── default_rules.json
├── bin/                  # [OPTIONNEL] Binaires natifs ou scripts exécutables
│   └── patch_tool
└── docs/                 # [OPTIONNEL] Documentation embarquée au format Markdown
    └── README.md
```

---

## 5. Spécification Complète du Manifeste TOML

Le fichier `manifest.toml` doit obligatoirement être présent à la racine du paquet.

```toml
[extension]
id = "org.labo.labx.sample"
name = "Sample GSI Optimizer"
version = "1.0.0"
author = "Android ROM Lab"
description = "Optimise les performances et ajuste les permissions d'une partition système déballée."
min_labo_api = 1

[engines]
provided = ["SampleOptimizerEngine"]
dependencies = ["CoreEngine", "ForgeImgEngine", "FilesystemEngine", "ProjectEngine"]

[permissions]
read_projects = true
modify_projects = true
run_terminal_commands = false
require_root = false

[ui]
category = "Optimization"
accent_color = "#3B82F6"
entry_point = "main_panel"
```

### Détail des Champs :
- `id` : Identifiant unique inversé (ex. `com.dev.nomoutil`).
- `name` : Nom affiché sur la tuile de l'écran d'accueil.
- `version` : Chaîne sémantique `MAJOR.MINOR.PATCH`.
- `min_labo_api` : Version minimale de l'API LABO requise (actuellement `1`).
- `provided` : Liste des moteurs que cette extension met à disposition de l'écosystème.
- `dependencies` : Moteurs LABO requis pour le bon fonctionnement de l'extension.
- `permissions` : Déclaration des accès requis aux projets et aux privilèges.

---

## 6. Guide Pas-à-Pas : Créer votre Première Extension LABX

### Étape 1 : Préparer l'arborescence de travail
Créez un dossier local nommé `deodexer_extension` :
```bash
mkdir deodexer_extension
cd deodexer_extension
mkdir config docs
```

### Étape 2 : Rédiger le fichier `manifest.toml`
Créez `manifest.toml` :
```toml
[extension]
id = "org.labo.labx.deodexer"
name = "Deodexer & APK Optimizer"
version = "1.0.0"
author = "LABO Core Team"
description = "Fusionne les fichiers vdex/odex dans les APKs pour rendre la ROM hautement portable."
min_labo_api = 1

[engines]
provided = ["DeodexEngine"]
dependencies = ["CoreEngine", "ProjectEngine", "FileEngine", "ArtifactEngine"]

[permissions]
read_projects = true
modify_projects = true
run_terminal_commands = false
require_root = false

[ui]
category = "Maintenance"
accent_color = "#10B981"
entry_point = "deodex_dashboard"
```

### Étape 3 : Ajouter la documentation de l'extension
Créez `docs/README.md` décrivant les fonctionnalités et paramètres.

### Étape 4 : Archiver au format `.labx`
```bash
zip -r ../deodexer.labx .
```

### Étape 5 : Installer dans LABO
Ouvrez l'application LABO -> Naviguez dans **Paramètres** -> Cliquez sur **Installer une extension LABX** -> Sélectionnez le fichier `deodexer.labx`. Le moteur `LABX Engine Installer` valide la conformité du manifeste, extrait l'extension et l'ajoute immédiatement à l'écran d'Accueil !

---

## 7. Interopérabilité avec les Moteurs LABO

Chaque extension accède à l'API LABO via le pont `LabxContext` fourni lors de son initialisation :
- `labxContext.getProject(projectId)` : Récupère la structure complète du projet déballé.
- `labxContext.log(message)` : Diffuse un message formaté directement dans le Terminal Universel.
- `labxContext.invokeEngine("ForgeImgEngine", action, params)` : Déclenche une opération sur un moteur natif.

---

## 8. Ingénierie GSI & Rétablissement du Boot

Lors de la reconstitution d'une GSI modifiée, trois règles d'or garantissent que l'image reste bootable :

1. **Cohérence `fs_config`** : Tout fichier ajouté dans `/system/bin` ou `/system/xbin` doit se voir assigner un UID `0` (root), un GID `2000` (shell), et des droits `0755`. Les fichiers dans `/system/app` doivent avoir des droits `0644`.
2. **Contextes SELinux (`file_contexts`)** : Aucun fichier ne doit hériter d'un contexte vide ou `unlabeled`. Un script exécutable ajouté doit se conformer à `u:object_r:system_file:s0` ou au type de domaine approprié.
3. **Android Verified Boot (AVB)** : Si des partitions sont modifiées, la signature d'intégrité racine (VBMeta) doit comporter le flag `flags=2` (`AVB_VBMETA_IMAGE_FLAGS_HASHTREE_DISABLED`) pour éviter un rejet du bootloader au démarrage.

---

## 9. Directives de Sécurité, Permissions & Bonnes Pratiques

- **Jamais d'écritures directes hors projet** : Les extensions ne doivent manipuler que les répertoires sous `workspace/projects/{projectId}/`.
- **Validation des signatures** : Avant d'injecter un APK modifié dans le système, vérifiez toujours qu'il a été re-signé conformément au `ROMsignchain` de la ROM cible.
- **Journalisation exhaustive** : Émettez toujours des logs précis lors de chaque étape pour permettre au technicien de suivre l'avancement dans le Terminal Universel.
