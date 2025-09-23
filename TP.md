# Projet Java - Générateur de Personnages
pour un Jeu de Rôle (3–4h)

## Objectif :

Créer une application Java permettant de générer des personnages personnalisés pour un jeu de rôle. Les
personnages possèdent des caractéristiques de base et peuvent se voir attribuer dynamiquement des capacités
spéciales. Ils sont enregistrés dans un système de persistance, et certaines règles du jeu sont accessibles
globalement.

## Contraintes techniques (obligatoires) :

Vous devez obligatoirement utiliser les éléments suivants :

• Design pattern Builder pour construire les personnages.
• Design pattern Decorator pour ajouter des capacités spéciales dynamiquement.
• Design pattern Singleton pour la configuration globale du jeu.
• Pattern DAO pour stocker et récupérer les personnages.
• Collections pour gérer les groupes de personnages.
• Généricité pour concevoir un DAO générique.

## Étapes à réaliser

### 1. Création de la classe de base Character

• Attributs obligatoires :

o name (String)
o strength, agility, intelligence (int)

• Méthodes attendues :

o getPowerLevel() : retourne un score calculé à partir des caractéristiques.
o getDescription() : retourne une description textuelle du personnage.

### 2. Implémentation du pattern Builder

• Créez une classe dédiée qui permet de construire un Character étape par étape.
• Prévoir des méthodes setName(), setStrength(), etc.
• Méthode finale attendue : build()

### 3. Ajout des capacités spéciales via Decorator

• Créez une interface ou une classe abstraite pour un "Personnage amélioré".
• Implémentez au moins 3 capacités décoratrices, par exemple :

o Invisibilité
o Résistance au feu
o Télépathie

• Chaque capacité doit modifier ou compléter la méthode getDescription() et éventuellement
getPowerLevel().

### 4. Gestion des règles du jeu avec un Singleton

• Créez une classe GameSettings contenant :

o Une valeur maxStatPoints (valeur max que peut atteindre la somme des stats).
o Une méthode isValid(Character c) qui vérifie que le personnage respecte les règles.

• Cette classe doit être un singleton, accessible globalement.

### 5. Implémentation du pattern DAO pour les personnages

• Créez une interface DAO<T> avec les méthodes :

o save(T item)
o findByName(String name)
o findAll()

• Implémentez un CharacterDAO pour gérer les personnages.

### 6. Utilisation des collections

• Créez une classe Party (équipe de personnages) qui contient une List<Character>.
• Ajoutez des méthodes pour :

o Ajouter un personnage à l’équipe
o Supprimer un personnage
o Calculer la puissance totale de l’équipe

### 7. Intégration et démonstration

• Dans une classe principale Main, créez plusieurs personnages avec des capacités spéciales.
• Stockez-les via le DAO.
• Affichez leurs descriptions et niveaux de puissance.
• Testez la validation avec les règles du GameSettings.

### 8. En plus …

• Implémentez un système de tri des personnages par puissance ou par nom.
• Ajoutez une gestion des erreurs si les règles du jeu ne sont pas respectées.
• Simulez un combat simple entre deux personnages.

## Livraison sur git

Votre rendu se fera sous la forme suivante

Arborescence des packages

```
src/
└── main/
	└── java/
		└── rpg/
			├── builder/
			│   ├── CharacterBuilder.java
			├── core/
			│   ├── Character.java
			│   ├── Party.java
			├── decorator/
			│   ├── CharacterDecorator.java
			│   ├── Invisibility.java
			│   ├── FireResistance.java
			│   ├── Telepathy.java
			├── dao/
			│   ├── DAO.java
			│   ├── CharacterDAO.java
			├── settings/
			│   ├── GameSettings.java
			└── main/
				└── Main.java
```

## Détail des packages

rpg.core

Contient les classes principales du modèle :

• Character : entité de base
• Party : collection de personnages

rpg.builder

Contient la classe :

• CharacterBuilder : permet de construire un personnage en plusieurs étapes.

rpg.decorator

Contient :

• Une classe abstraite ou une interface CharacterDecorator
• Les différentes capacités (Invisibility, FireResistance, etc.), chacune étendant le décorateur

rpg.dao

• DAO<T> : interface générique pour l’accès aux données
• CharacterDAO : implémentation de DAO pour les personnages

rpg.settings

• GameSettings : singleton contenant les règles globales du jeu

rpg.main

• Main : point d’entrée de l’application, avec des appels de démo