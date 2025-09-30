Product Backlog
Générateur de Personnages RPG Étendu
Épic 1 : Création et gestion des personnages
• US 1.1 : En tant qu’utilisateur, je veux créer un personnage en choisissant ses caractéristiques
(Builder) pour disposer d’une base jouable.
• US 1.2 : En tant qu’utilisateur, je veux ajouter ou retirer dynamiquement des capacités spéciales
(Decorator) pour personnaliser mon personnage.
• US 1.3 : En tant qu’utilisateur, je veux stocker mes personnages et les retrouver plus tard (DAO).
• US 1.4 : En tant qu’utilisateur, je veux pouvoir organiser mes personnages dans des groupes
hiérarchisés (Composite → ex. une armée composée de plusieurs parties).
Épic 2 : Gestion des règles du jeu et des actions
• US 2.1 : En tant qu’utilisateur, je veux que les règles globales du jeu (limite de points de stats, max
de personnages par groupe, etc.) soient centralisées et accessibles partout (Singleton).
• US 2.2 : En tant qu’utilisateur, je veux déclencher des actions de jeu via des commandes (Command
→ ex. attaquer, défendre, utiliser un pouvoir) pour simuler des tours de jeu.
• US 2.3 : En tant qu’utilisateur, je veux qu’un système de validation applique des règles dans un
enchaînement (Chain of Responsibility → ex. validation des points, validation du nom, validation
des capacités).
Épic 3 : Interaction et affichage (MVC)
• US 3.1 : En tant qu’utilisateur, je veux disposer d’une interface simple (console ou Swing) suivant le
pattern MVC pour gérer mes personnages.
• US 3.2 : En tant qu’utilisateur, je veux visualiser les personnages et leurs pouvoirs dans une liste
triable (MVC + Observer).
• US 3.3 : En tant qu’utilisateur, je veux être notifié automatiquement si une modification est faite
(Observer → ex. quand un personnage change, l’affichage de l’équipe se met à jour).
Épic 4 : Combat et simulation
• US 4.1 : En tant qu’utilisateur, je veux lancer un combat entre deux personnages pour comparer leurs
niveaux de puissance (Command + Strategy optionnelle si tu veux aller plus loin).
• US 4.2 : En tant qu’utilisateur, je veux observer l’évolution du combat (Observer → les spectateurs
ou le journal de combat reçoivent les infos).
• US 4.3 : En tant qu’utilisateur, je veux pouvoir sauvegarder et rejouer une séquence d’actions
(Command → historique/replay des actions).
Résultat attendu
• Une application Java qui va au-delà du simple générateur :
o Création et validation avancée de personnages (Builder, Singleton, Chain of Responsibility).
o Capacités ajoutées dynamiquement (Decorator).
o Actions et combats (Command, Observer).
o Organisation en armées/équipes hiérarchisées (Composite).
o Persistance avec DAO générique.
o Interface minimale (console ou Swing) en MVC.
Plan de Sprint – Projet Générateur RPG Étendu
Jour 1 – Mise en place du cœur du système (MVP)
• US 1.1 (Builder) : Création d’un personnage avec caractéristiques de base.
• US 2.1 (Singleton) : Centralisation des règles globales (GameSettings).
• US 2.3 (Chain of Responsibility) : Validation des personnages (nom unique, total des stats, etc.).
• US 1.2 (Decorator) : Ajout dynamique de capacités spéciales (invisibilité, télépathie, etc.).
• US 1.3 (DAO) : Persistance des personnages (sauvegarde / lecture).
• US 1.4 (Composite) : Organisation de personnages en équipes hiérarchisées (Party, puis armée).
Livrable fin Jour 1 :
Un système où l’on peut créer un personnage valide via un Builder, lui appliquer des capacités, le
sauvegarder/récupérer, et l’ajouter dans une équipe.
Jour 2 – Interaction, combats et interface
• US 2.2 (Command) : Implémenter des actions de jeu (attaquer, défendre, utiliser un pouvoir).
• US 4.1 (Combat simple) : Comparer deux personnages et afficher le vainqueur.
• US 4.3 (Command + historique) : Enregistrer et rejouer une séquence d’actions
• US 3.1 (MVC) : Mise en place d’une petite IHM (console ou Swing).
• US 3.2 (Observer) : Synchroniser l’affichage quand un personnage ou une équipe est modifié.
• US 4.2 (Observer) : Journal de combat qui s’actualise automatiquement.
Livrable fin Jour 2 :
Une application complète permettant de :
• créer des personnages avec pouvoirs spéciaux,
• gérer des équipes,
• lancer des combats et rejouer des actions,
• utiliser une IHM simple (MVC) avec mise à jour auto (Observer).