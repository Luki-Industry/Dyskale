# Hytale Discord Link Plugin

Plugin Hytale permettant de lier les comptes Discord aux joueurs du serveur avec système de vérification automatique et console Discord.

## 🌟 Fonctionnalités

- **🔐 Liaison Discord obligatoire** : Force les joueurs à lier leur compte Discord avant de rejoindre
- **🤖 Liaison automatique par MP** : Les joueurs reçoivent un code unique à envoyer au bot Discord
- **📊 Console Discord** : Tous les logs du serveur dans un salon Discord
- **⚡ Commandes Discord** : Exécutez des commandes serveur depuis Discord
- **💾 Stockage JSON** : Sauvegarde automatique des comptes liés
- **🔄 Configuration dynamique** : Ajout automatique des nouveaux paramètres

## 📋 Prérequis

- **Java 25** (JDK-25)
- **Maven 3.9+** (wrapper inclus)
- **Serveur Hytale** avec HytaleServer.jar
- **Bot Discord** avec token

## 🚀 Installation

### 1. Installer l'API Hytale

```powershell
# Placer HytaleServer.jar à la racine du projet
.\setup-hytale-api.ps1
```

### 2. Compiler le plugin

```powershell
.\mvnw.cmd clean package
```

Le JAR sera généré dans `target/Hytale-Plugin-1.0.0.jar` (~13 MB).

### 3. Installation sur le serveur

1. Copier `target/Hytale-Plugin-1.0.0.jar` dans le dossier `mods/` du serveur
2. Démarrer le serveur
3. Arrêter le serveur (génération de la config)
4. Configurer `mods/LukienLive_DiscordLink/config.json`

## ⚙️ Configuration

Fichier : `mods/LukienLive_DiscordLink/config.json`

```json
{
  "discord_token": "VOTRE_TOKEN_BOT_DISCORD",
  "guild_id": "VOTRE_ID_SERVEUR_DISCORD",
  "require_discord_link": false,
  "kick_message": "Vous devez lier votre compte Discord...",
  "console_channel_id": "VOTRE_ID_SALON_CONSOLE",
  "enable_console_logs": true
}
```

### Paramètres

| Paramètre | Description | Valeur par défaut |
|-----------|-------------|-------------------|
| `discord_token` | Token du bot Discord | `VOTRE_TOKEN_DISCORD_ICI` |
| `guild_id` | ID du serveur Discord | `VOTRE_GUILD_ID_ICI` |
| `require_discord_link` | Obliger la liaison Discord | `false` |
| `kick_message` | Message affiché aux joueurs non liés | Message par défaut |
| `console_channel_id` | ID du salon pour les logs | `VOTRE_CHANNEL_ID_ICI` |
| `enable_console_logs` | Activer les logs Discord | `true` |

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

## 📁 Structure du projet

```
Hytale-Plugin/
├── src/main/java/com/lukienlive/hytale/
│   ├── Main.java                    # Point d'entrée du plugin
│   ├── DiscordBot.java              # Gestion du bot Discord
│   ├── EventsListener.java          # Événements de connexion
│   ├── LinkedStorage.java           # Stockage des liaisons
│   ├── DiscordCommandSender.java    # Exécution de commandes
│   └── DiscordLogHandler.java       # Logs vers Discord
├── src/main/resources/
│   ├── config.json                  # Config par défaut (template)
│   └── manifest.json                # Métadonnées du plugin
├── HytaleServer.jar                 # API Hytale (requis)
├── pom.xml                          # Configuration Maven
├── mvnw.cmd                         # Wrapper Maven
└── setup-hytale-api.ps1            # Script d'installation API

Généré au runtime :
├── mods/LukienLive_DiscordLink/
│   ├── config.json                  # Configuration active
│   └── linked_players.json          # Base de données
```

## 🔧 Développement

### Compiler en mode rapide

```powershell
.\mvnw.cmd package -q
```

### Dépendances

- **JDA 5.0.0-alpha.19** : API Discord
- **Gson 2.10.1** : Sérialisation JSON
- **SLF4J-NOP 1.7.36** : Suppression logs JDA
- **Hytale Server API** : API serveur Hytale

### API Hytale utilisée

- `JavaPlugin` : Classe de base
- `PlayerSetupConnectEvent` : Détection connexion joueur
- `EventRegistry` : Système d'événements
- `CommandManager` : Exécution de commandes

## 📝 Changelog

### Version 1.0.0 (2026-01-14)

- ✅ Liaison Discord par code MP
- ✅ Vérification automatique à la connexion
- ✅ Console Discord avec logs temps réel
- ✅ Exécution de commandes depuis Discord
- ✅ Configuration dynamique avec fusion automatique
- ✅ Stockage JSON avec nettoyage des codes expirés

## 🤝 Contribution

Les contributions sont les bienvenues ! N'hésitez pas à ouvrir une issue ou une pull request.

## 📄 Licence

Ce projet est sous licence MIT.

## 👤 Auteur

**LukienLive**

## 🙏 Remerciements

- Hypixel Studios pour Hytale
- JDA (Java Discord API)
- Nitrado pour leur exemple de plugin WebServer
