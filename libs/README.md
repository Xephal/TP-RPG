# Générateur de Personnages RPG - TP & TP2

Projet Java implementant des design patterns pour un générateur de personnages de jeu de rôle.

## Patterns Implémentés

### TP1 (Base)
- **Builder** : Construction de personnages avec validation
- **Decorator** : Capacités spéciales (Invisibilité, Résistance au feu, Télépathie)
- **Singleton** : Configuration globale du jeu (GameSettings)
- **DAO** : Persistance générique des personnages
- **Collections** : Gestion des groupes (Party)

### TP2 (Extensions)
- **Chain of Responsibility** : Validation avancée des personnages
- **Composite** : Hiérarchie Army/Party pour groupes de personnages
- **Command** : Actions de jeu (attaque, défense, pouvoirs) + historique
- **Observer** : Notifications automatiques pour combat et changements
- **MVC** : Architecture Model-View-Controller avec EventBus

## Compilation

```powershell
# Compiler tous les fichiers Java
$files = Get-ChildItem -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
javac -d out $files
```

## Exécution

### Mode Console (TP1 compatible)
```powershell
java -cp out rpg.main.Main
```

### Mode Console TP2
```powershell
java -cp out rpg.main.MainTP2
```

### Interface Graphique (TP2) - NOUVEAU
```powershell
java -cp out rpg.main.MainGUI gui
```

### Mode Console de MainGUI
```powershell
java -cp out rpg.main.MainGUI
```

## Interface Graphique - Fonctionnalités

L'interface graphique Swing offre un accès complet à toutes les fonctionnalités via des onglets :

### 📋 Onglet "Characters"
- **Création de personnages** : Formulaire avec nom, force, agilité, intelligence
- **Validation automatique** : Chain of Responsibility avec règles configurables
- **Application de décorateurs** : Boutons pour ajouter Invisibilité, Résistance au feu, Télépathie
- **Liste des personnages** : Affichage en temps réel avec stats et power level

### ⚔️ Onglet "Combat"
- **Sélection des combattants** : ComboBox avec tous les personnages disponibles
- **Simulation de combat** : Utilise Command pattern pour les actions
- **Log en temps réel** : Affichage détaillé des actions via Observer
- **Historique intégré** : Toutes les commandes sont enregistrées

### ⚙️ Onglet "Settings"
- **Configuration GameSettings** : Modification du Singleton en temps réel
- **Max Stat Points** : Limite de la somme des statistiques
- **Max Characters Per Group** : Limite pour les Party
- **Max Groups Per Army** : Limite pour les Army
- **Application immédiate** : Les changements affectent immédiatement les validations

### 🏛️ Onglet "Armies"
- **Création d'armées** : Pattern Composite pour hiérarchie
- **Gestion des groupes** : Organisation Army > Party > Characters
- **Calcul de puissance** : Total power calculé automatiquement

### 📜 Onglet "History"
- **Historique des commandes** : Toutes les actions Command enregistrées
- **Replay** : Re-exécution de l'historique complet
- **Rafraîchissement** : Mise à jour en temps réel
- **Nettoyage** : Clear de l'historique

## Architecture Technique

### Observer Pattern
- **EventBus** : Hub central pour toutes les notifications
- **Mise à jour automatique** : Les vues se synchronisent automatiquement
- **Events supportés** : CHARACTER_CREATED, COMBAT_ACTION, SETTINGS_CHANGED, etc.

### MVC Pattern
- **GameController** : Coordination entre Model et View
- **SwingView** : Interface graphique complète
- **ConsoleView** : Interface console (compatibilité)
- **EventBus** : Communication inter-couches

### Command Pattern
- **AttackCommand, DefendCommand, UsePowerCommand** : Actions atomiques
- **CommandHistory** : Historique avec replay
- **CombatEngine** : Utilise les commandes pour les combats
