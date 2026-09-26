# Dyskale

Plugin Hytale pour connecter Discord, les joueurs et les outils serveur avec vérification automatique, console Discord et statut en temps réel.

## 🌟 Fonctionnalités

- **🔐 Liaison Discord obligatoire** : Force les joueurs à lier leur compte Discord avant de rejoindre
- **🤖 Liaison automatique par MP** : Les joueurs reçoivent un code unique à envoyer au bot Discord
- **📊 Console Discord complète** : Capture TOUS les logs du serveur et les envoie vers Discord en temps réel
  - Batching intelligent pour éviter le rate limiting
  - Formatage ANSI avec couleurs
  - Filtrage par niveau de log (SEVERE, WARNING, INFO, DEBUG, TRACE)
  - Support complet des stack traces
  - Architecture Clean Architecture testable et extensible
- **⚡ Commandes Discord** : Exécutez des commandes serveur depuis Discord
- **📈 Statut du serveur** : Mise à jour automatique avec nombre de joueurs et liste
- **🟢 Présence du bot** : Affiche "Watching X joueurs" en temps réel
- **🔄 Synchronisation des rôles** : Synchronisation unidirectionnelle des rôles Discord vers Hytale
- **🛠️ Commandes Hytale** : Gestion des liaisons directement en jeu (/dyskale)
- **💾 Stockage JSON** : Sauvegarde automatique des comptes liés
- **🔄 Configuration dynamique** : Ajout automatique des nouveaux paramètres

## 📋 Prérequis

- **Java 25** (JDK-25)
- **Gradle Wrapper inclus** (aucune installation Gradle nécessaire)
- **Serveur Hytale** avec HytaleServer.jar
- **Bot Discord** avec token

## 🚀 Installation

### 1. Compiler le projet

```powershell
.\gradlew shadowJar
```

Le build compare automatiquement `libs/HytaleServer.jar` avec la version Hytale installée localement et la met à jour si son contenu est différent. La version utilisée est inscrite dans le manifest du jar Dyskale.

Pour afficher les versions détectées :

```powershell
.\gradlew printHytaleServerInfo
```

### 2. Installation sur le serveur

1. Copier `build/libs/Dyskale-1.0.0.jar` dans le dossier `mods/` du serveur
2. Démarrer le serveur
3. Arrêter le serveur (génération de la config)
4. Configurer `mods/LukiEnLive_Dyskale/config.json`
5. Redémarrer le serveur

## ⚙️ Configuration

Fichier : `mods/LukiEnLive_Dyskale/config.json`

```json
{
  "Discord_token": "VOTRE_TOKEN_BOT_DISCORD",
  "Guild_id": "VOTRE_ID_SERVEUR_DISCORD",
  "Discord_invite_link": "https://discord.gg/VOTRE_INVITE",
  "Required_role_ids": "ROLE_ID_1,ROLE_ID_2",
  "Require_discord_link": false,
  "Require_guild_membership": true,
  "Require_role": false,
  "Link_message": "Compte Discord requis!\n\nVotre code de liaison: {code}",
  "Console_channel_id": "VOTRE_ID_SALON_CONSOLE",
  "Status_channel_id": "VOTRE_ID_SALON_STATUT",
  "Status_message_id": "",
  "Enable_console_logs": true,
  "Enable_bot_status": true,
  "Enable_status_message": true,
  "Role_Group_Mapping": "ROLE_ID_1:GROUP_NAME_1,ROLE_ID_2:GROUP_NAME_2",
  "Enable_update_check": true,
  "Update_repository": "Luki-Industry/Dyskale",
  "Update_check_interval_hours": 24
}
```

### Paramètres

| Paramètre | Description | Valeur par défaut |
|-----------|-------------|-------------------|
| `Discord_token` | Token du bot Discord | `DISCORD_BOT_TOKEN` |
| `Guild_id` | ID du serveur Discord | `YOUR_GUILD_ID_HERE` |
| `Discord_invite_link` | Lien d'invitation Discord | `https://discord.gg/VOTRE_INVITE` |
| `Required_role_ids` | IDs des rôles Discord requis | `ROLE_ID_1,ROLE_ID_2` |
| `Require_discord_link` | Obliger la liaison Discord | `false` |
| `Require_guild_membership` | Exiger que le joueur soit membre du serveur Discord | `true` |
| `Require_role` | Exiger un rôle Discord | `false` |
| `Link_message` | Message affiché aux joueurs non liés | Message par défaut |
| `Console_channel_id` | ID du salon pour les logs | `YOUR_CHANNEL_ID_HERE` |
| `Status_channel_id` | ID du salon pour le statut | `YOUR_STATUS_CHANNEL_ID_HERE` |
| `Status_message_id` | ID du message de statut (auto) | `""` |
| `Enable_console_logs` | Activer les logs Discord | `true` |
| `Console_minimum_log_level` | Niveau minimum des logs (SEVERE, WARNING, INFO, DEBUG, TRACE) | `INFO` |
| `Enable_bot_status` | Activer "Watching X joueurs" | `false` |
| `Enable_status_message` | Activer le message de statut | `false` |
| `Role_Group_Mapping` | Correspondance Rôles <-> Groupes | `""` |
| `Enable_update_check` | Vérifier les nouvelles releases GitHub | `true` |
| `Update_repository` | Dépôt GitHub au format `organisation/projet` | `Luki-Industry/Dyskale` |
| `Update_check_interval_hours` | Intervalle minimal entre deux vérifications | `24` |

Au démarrage, Dyskale affiche la version du serveur Hytale utilisé. La compatibilité réelle dépend de cette version et des API présentes dans le jar installé.

## 🔄 Synchronisation des rôles

Le système de synchronisation permet de lier des rôles Discord à des groupes LuckPerms.

- **Discord → Hytale** : Si un joueur reçoit un rôle sur Discord, il est ajouté au groupe LuckPerms correspondant.
- **Hytale → Discord** : Désactivé (les changements sur Hytale n'affectent pas Discord)
- **Suppression** : La suppression du rôle Discord entraîne le retrait du groupe LuckPerms.

### Configuration du mapping

Format : `ID_ROLE_DISCORD:NOM_GROUPE_LUCKPERMS`
Séparer les multiples mappings par une virgule.

**Exemple :**
Pour lier le rôle Discord `1122334455` au groupe `vip` et le rôle `9988776655` au groupe `moderator` :

```json
"Role_Group_Mapping": "1122334455:vip,9988776655:moderator"
```



### Obtenir les IDs Discord

1. Activer le mode développeur : `Paramètres Discord` → `Avancés` → `Mode développeur`
2. Clic droit sur le serveur/salon → `Copier l'identifiant`

## 🎮 Utilisation

### Liaison de compte

1. **Joueur tente de rejoindre** le serveur sans compte lié
2. **Serveur refuse** la connexion et affiche un code (ex: `AB12CD`)
3. **Joueur envoie le code en MP** au bot Discord
4. **Bot confirme** la liaison et autorise l'accès

### Console Discord

Dans le salon console configuré :
- **Voir les logs** : Tous les événements du serveur en temps réel
- **Exécuter des commandes** : Tapez n'importe quelle commande serveur
- **Réactions** : ⏳ en cours → ✅ succès / ❌ erreur

### Statut du serveur

Le bot affiche automatiquement :
- **🟢 Serveur EN LIGNE** : Avec nombre de joueurs et liste
- **🔴 Serveur HORS LIGNE** : Quand le serveur s'arrête
- **Mise à jour** : Toutes les 30 secondes
- **Timestamp** : Dernière mise à jour visible

## 🛠️ Commandes en jeu

Le plugin ajoute la commande `/dyskale` pour gérer la synchronisation. `/discord` reste disponible comme alias de compatibilité.

| Commande | Permission | Description |
|----------|------------|-------------|
| `/dyskale sync <player>` | `dyskale.sync` | Force la synchronisation des rôles/groupes pour un joueur |
| `/dyskale unlink <player>` | `dyskale.unlink` | Dissocie manuellement le compte Discord d'un joueur |
| `/dyskale status <player>` | `dyskale.status` | Affiche le statut de liaison et l'ID Discord associé |
| `/dyskale reload` | `dyskale.reload` | Recharge la configuration (config.json) sans redémarrer |

## 📁 Structure du projet

```
Dyskale/
├── src/main/java/com/lukienlive/hytale/
│   ├── Main.java                          # Point d'entrée du plugin
│   ├── application/
│   │   └── service/
│   │       └── LinkService.java          # Service de gestion des liaisons
│   ├── domain/
│   │   ├── PendingLink.java             # Modèle pour les codes de liaison
│   │   └── repository/
│   │       └── LinkRepository.java      # Interface de stockage
│   ├── infrastructure/
│   │   ├── discord/
│   │   │   ├── ConsoleLogService.java   # Gestion des logs console
│   │   │   └── StatusUpdateService.java # Gestion du statut bot/serveur
│   │   └── persistence/
│   │       └── JsonLinkRepository.java  # Implémentation stockage JSON
│   ├── discord/
│   │   ├── DiscordBot.java               # Gestion du bot Discord & Events
│   │   ├── DiscordLogger.java            # Orchestrateur des logs Discord
│   │   ├── DiscordCommandSender.java     # Exécution des commandes
│   │   └── RoleSyncService.java          # Synchronisation des rôles
│   ├── hytale/
│   │   ├── ConnectionListener.java       # Événements de connexion
│   │   ├── HytaleConfig.java            # Codec de configuration
│   │   └── commands/                     # Commandes Hytale (/dyskale)
│   ├── inject/
│   │   ├── HytaleInjector.java          # Configuration Guice
│   │   └── annotation/
├── build.gradle                          # Configuration Gradle
└── README.md                             # Documentation
```

## Console Discord avancée

Le plugin dispose d'un **système de capture de logs complet** qui envoie TOUS les logs du serveur vers Discord.

### Caractéristiques

- ✅ **Capture complète** : Tous les logs Java sont interceptés automatiquement
- ✅ **Filtrage intelligent** : Configurez le niveau minimum (INFO, WARNING, SEVERE, etc.)
- ✅ **Formatage élégant** : Messages avec icônes, couleurs ANSI et timestamps
- ✅ **Performance optimale** : Batching des messages pour éviter le spam
- ✅ **Stack traces** : Support complet des exceptions avec formatage
- ✅ **Architecture Clean** : Code testable et extensible

### Configuration des niveaux de log

```json
{
  "Console_minimum_log_level": "INFO"
}
```

**Niveaux disponibles** (du plus strict au plus verbeux) :
- `SEVERE` : Uniquement les erreurs critiques
- `WARNING` : Avertissements et erreurs
- `INFO` : Informations générales (recommandé)
- `DEBUG` : Messages de débogage
- `TRACE` : Tous les messages (très verbeux)

### Exemple de sortie Discord

```ansi
[14:30:45] ℹ️ [INFO] [Dyskale] 🚀 Plugin Dyskale démarré
[14:30:46] 🟢 **Steve** a rejoint le serveur
[14:31:02] 💬 **Steve**: Hello!
[14:32:15] ⚠️ [WARNING] [WorldManager] Faible performance
[14:35:00] 🔴 [SEVERE] [Database] Erreur de connexion
java.sql.SQLException: Connection refused
    at Database.connect(Database.java:42)
```

### Messages personnalisés

Vous pouvez envoyer des messages custom depuis votre code :

```java
@Inject
private DiscordLogger discordLogger;

discordLogger.info("Message info");
discordLogger.warning("Message warning");
discordLogger.error("Message erreur");
discordLogger.playerJoin("PlayerName");
discordLogger.playerLeave("PlayerName");
```

## 🛠️ Développement

### Compilation

```powershell
.\gradlew clean shadowJar
```

### CI et releases

Chaque push sur `main` et chaque pull request déclenche le workflow CI. Il compile Dyskale avec l'API Hytale publiée sur Maven et conserve le jar comme artefact.

Pour publier une version, mettre à jour la version Gradle et `manifest.json`, puis pousser un tag correspondant, par exemple `v1.0.0`. Le workflow vérifie la cohérence des versions, compile le jar et crée automatiquement la release GitHub.

### Architecture

Ce plugin suit les principes de **Clean Architecture** :
- **Domain Layer** : Logique métier pure (pas de dépendances externes)
- **Application Layer** : Services et use cases
- **Infrastructure Layer** : Implémentations concrètes (Hytale, Discord)

Voir [.github/copilot-instructions.md](.github/copilot-instructions.md) pour les guidelines de développement.

### Dépendances

- **JDA 6.3.0** : Librairie Discord
- **SLF4J 2.0.17** : Logger relocalisé pour JDA afin d'éviter les conflits avec le serveur
- **Gson 2.13.2** : Sérialisation JSON
- **Guice 7.0.0** : Injection de dépendances
- **Lombok 1.18.42** : Réduction du boilerplate

## 📝 Licence

MIT License - Voir le fichier LICENSE pour plus de détails.

## 👤 Auteurs

**LukiEnLive** et **SoraxDubbing** — co-auteurs du projet

- GitHub: [@LukiEnLive](https://github.com/LukiEnLive)
- GitHub: [@Sorax5](https://github.com/Sorax5)

## 🤝 Contribution

Les contributions sont les bienvenues ! N'hésitez pas à ouvrir une issue ou une pull request.
