# Hytale Discord Link Plugin

Plugin Hytale permettant de lier les comptes Discord aux joueurs du serveur avec système de vérification automatique, console Discord et statut en temps réel.

## 🌟 Fonctionnalités

- **🔐 Liaison Discord obligatoire** : Force les joueurs à lier leur compte Discord avant de rejoindre
- **🤖 Liaison automatique par MP** : Les joueurs reçoivent un code unique à envoyer au bot Discord
- **📊 Console Discord** : Tous les logs du serveur dans un salon Discord
- **⚡ Commandes Discord** : Exécutez des commandes serveur depuis Discord
- **📈 Statut du serveur** : Mise à jour automatique avec nombre de joueurs et liste
- **🟢 Présence du bot** : Affiche "Watching X joueurs" en temps réel
- **🔄 Synchronisation des rôles** : Synchronisation unidirectionnelle des rôles Discord vers Hytale
- **🛠️ Commandes Hytale** : Gestion des liaisons directement en jeu (/discord)
- **💾 Stockage JSON** : Sauvegarde automatique des comptes liés
- **🔄 Configuration dynamique** : Ajout automatique des nouveaux paramètres

## 📋 Prérequis

- **Java 25** (JDK-25)
- **Gradle 8.0+**
- **Serveur Hytale** avec HytaleServer.jar
- **Bot Discord** avec token

## 🚀 Installation

### 1. Compiler le projet

```powershell
.\gradlew shadowJar
```

Le JAR sera généré dans `build/libs/Hytale-Plugin-1.0.0.jar`.

### 2. Installation sur le serveur

1. Copier `build/libs/Hytale-Plugin-1.0.0.jar` dans le dossier `mods/` du serveur
2. Démarrer le serveur
3. Arrêter le serveur (génération de la config)
4. Configurer `mods/LukienLive_DiscordLink/config.json`
5. Redémarrer le serveur

## ⚙️ Configuration

Fichier : `mods/LukienLive_DiscordLink/config.json`

```json
{
  "Discord_token": "VOTRE_TOKEN_BOT_DISCORD",
  "Guild_id": "VOTRE_ID_SERVEUR_DISCORD",
  "Require_discord_link": false,
  "Kick_message": "Vous devez lier votre compte Discord pour rejoindre ce serveur. Utilisez !link sur notre Discord.",
  "Console_channel_id": "VOTRE_ID_SALON_CONSOLE",
  "Status_channel_id": "VOTRE_ID_SALON_STATUT",
  "Status_message_id": "",
  "Enable_console_logs": true,
  "Enable_bot_status": true,
  "Enable_status_message": true,
  "Role_Group_Mapping": "ROLE_ID_1:GROUP_NAME_1,ROLE_ID_2:GROUP_NAME_2"
}
```

### Paramètres

| Paramètre | Description | Valeur par défaut |
|-----------|-------------|-------------------|
| `Discord_token` | Token du bot Discord | `DISCORD_BOT_TOKEN` |
| `Guild_id` | ID du serveur Discord | `YOUR_GUILD_ID_HERE` |
| `Require_discord_link` | Obliger la liaison Discord | `false` |
| `Kick_message` | Message affiché aux joueurs non liés | Message par défaut |
| `Console_channel_id` | ID du salon pour les logs | `YOUR_CHANNEL_ID_HERE` |
| `Status_channel_id` | ID du salon pour le statut | `YOUR_STATUS_CHANNEL_ID_HERE` |
| `Status_message_id` | ID du message de statut (auto) | `""` |
| `Enable_console_logs` | Activer les logs Discord | `true` |
| `Enable_bot_status` | Activer "Watching X joueurs" | `false` |
| `Enable_status_message` | Activer le message de statut | `false` |
| `Role_Group_Mapping` | Correspondance Rôles <-> Groupes | `""` |

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
- **Mise à jour** : Toutes les 5 minutes
- **Timestamp** : Dernière mise à jour visible

## 🛠️ Commandes en jeu

Le plugin ajoute la commande `/discord` pour gérer la synchronisation.

| Commande | Permission | Description |
|----------|------------|-------------|
| `/discord sync <player>` | `discord.sync` | Force la synchronisation des rôles/groupes pour un joueur |
| `/discord unlink <player>` | `discord.unlink` | Dissocie manuellement le compte Discord d'un joueur |
| `/discord status <player>` | `discord.status` | Affiche le statut de liaison et l'ID Discord associé |
| `/discord reload` | `discord.reload` | Recharge la configuration (config.json) sans redémarrer |

## 📁 Structure du projet

```
Hytale-Plugin/
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
│   │   ├── DiscordLogHandler.java        # Handler pour les logs Java
│   │   ├── DiscordCommandSender.java     # Exécution des commandes
│   │   └── RoleSyncService.java          # Synchronisation des rôles
│   ├── hytale/
│   │   ├── ConnectionListener.java       # Événements de connexion
│   │   ├── HytaleConfig.java            # Codec de configuration
│   │   └── commands/                     # Commandes Hytale (/discord)
│   ├── inject/
│   │   ├── HytaleInjector.java          # Configuration Guice
│   │   └── annotation/
├── build.gradle                          # Configuration Gradle
└── README.md                             # Documentation
```

## 🛠️ Développement

### Compilation

```powershell
.\gradlew clean shadowJar
```

### Dépendances

- **JDA 6.3.0** : Librairie Discord
- **slf4j-nop 2.0.17** : Logger pour JDA
- **Gson 2.13.2** : Sérialisation JSON
- **Guice 7.0.0** : Injection de dépendances
- **Lombok 1.18.42** : Réduction du boilerplate

## 📝 Licence

MIT License - Voir le fichier LICENSE pour plus de détails.

## 👤 Auteur

**Luki**
- Discord: [Votre serveur Discord]
- GitHub: [@lukienlive](https://github.com/lukienlive)

## 🤝 Contribution

Les contributions sont les bienvenues ! N'hésitez pas à ouvrir une issue ou une pull request.
