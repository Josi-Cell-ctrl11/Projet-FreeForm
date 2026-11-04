# FloatX 0.2 — fenêtres Android sans Shizuku

Prototype Kotlin de lancement d’applications dans les **fenêtres libres natives d’Android**, sur le principe de Taskbar. Cette version remplace le moteur d’écran virtuel de la série 0.1 : **aucun Shizuku, root, ADB ou processus shell n’est utilisé**.

Le lanceur XOS reste en place. Les options développeur de fenêtres libres déjà utilisées avec Taskbar doivent rester activées ; FloatX ne les modifie pas.

## Installer et tester

1. Dans **Actions → APK Android**, ouvrir le dernier lancement réussi.
2. Télécharger l’artefact **FloatX-APK**, décompresser et installer `FloatX-0.2.0-native-debug.apk`.
3. Si Android refuse la mise à jour, désinstaller l’ancien FloatX puis installer ce nouvel APK : les compilations debug n’ont pas encore de clé stable.
4. Arrêter Shizuku pour vérifier que FloatX fonctionne indépendamment de lui. Aucun accès Shizuku n’est à accorder.
5. Ouvrir FloatX, garder **Compatibilité de lancement type Taskbar** activée, puis **Choisir une vraie application → Calculatrice**.
6. Vérifier visuellement si Calculatrice s’ouvre dans une fenêtre, et si les touches répondent.
7. Si elle s’ouvre en plein écran, revenir dans FloatX, désactiver la compatibilité et réessayer. Le bouton **Tester une fenêtre FloatX** lance aussi une activité interne capable de signaler son propre état multi-fenêtre.

Le test décisif reste à réaliser sur le téléphone : compilation et tests unitaires ne garantissent pas que XOS acceptera les options de lancement.

## Ce qui change

- `ActivityOptions.setLaunchBounds` demande la taille initiale.
- `ActivityOptions.setLaunchWindowingMode(5)` est invoqué via AndroidHiddenApiBypass 6.1 pour demander le mode libre, comme le mécanisme utilisé dans Taskbar. Cette bibliothèque est embarquée ; aucune application externe n’est nécessaire.
- Avec la compatibilité activée, une activité dédiée prépare d’abord un espace multi-fenêtre, puis lance l’activité sélectionnée après une courte transition.
- Le mode direct n’utilise pas cette activité intermédiaire.
- Si l’accès au sélecteur de mode est refusé, une demande de taille via l’API publique reste tentée et le diagnostic **N01** est conservé dans FloatX. Il ne constitue pas une confirmation de fenêtre libre.
- Si le lancement est refusé, le diagnostic **N02** est affiché et conservé.

La dernière tentative est enregistrée localement, sans serveur ni télémétrie. Le test interne sait seulement détecter son **propre** état multi-fenêtre ; FloatX ne prétend pas inspecter celui des autres applis.

## Apparence et limites de cette étape

Les **vraies applications utilisent les barres et commandes du système Android/XOS**, y compris la barre grise éventuelle. Leur clavier et leurs interactions sont gérés directement par Android : plus de transfert d’image ou de tactile dans un écran virtuel.

L’objectif de cette version est de valider l’ouverture réelle sans moteur externe. La poignée et la bulle personnalisées de la maquette sont toujours testables dans **Essayer les gestes — démo**, mais ne contrôlent pas encore les vraies fenêtres. L’intégration du style de la maquette constitue une étape distincte dont la faisabilité reste à vérifier.

La compatibilité peut varier selon le firmware et l’application. Une activité déjà ouverte peut reprendre sa tâche existante, et Android peut ignorer les limites demandées. Certaines activités ne sont pas redimensionnables. Une petite fenêtre de préparation peut apparaître brièvement selon le comportement de XOS.

Le bouton **Arrêter la démo et l’espace de compatibilité** ferme seulement les tâches appartenant à FloatX. Fermer les autres applis avec leurs commandes système. FloatX ne remplace ni l’accueil XOS ni le gestionnaire système de fenêtres.

## Démo validée

Conversation fictive locale avec Alex : déplacement, redimensionnement, agrandissement, réduction et déplacement de bulle. L’autorisation **Afficher par-dessus les autres applications** et le service de notification concernent cette démo ; aucun message n’est envoyé.

## Compilation et vérifications

JDK 17, Gradle 8.9, Android SDK 35, Android Gradle Plugin 8.7.3 et Kotlin 2.0.21. Android 11 minimum.

```
gradle --no-daemon testDebugUnitTest lintDebug assembleDebug
```

GitHub Actions installe ces outils, exécute les tests, l’analyse Android et produit l’APK. Les tests vérifient les limites de la démo et les tailles natives demandées avec les barres système, les découpes, les petits écrans et les changements d’orientation.

## Architecture

- `MainActivity` : choix de l’application, modes direct/compatibilité, diagnostic local, accès à la démo.
- `NativeFreeformLauncher` : demande de lancement en mode libre et taille initiale ; aucun accès aux données privées des autres applis.
- `FreeformWorkspaceActivity` : tâche auxiliaire redimensionnable, distincte du lanceur et exclue des applications récentes.
- `FreeformProbeActivity` : test interne de l’état multi-fenêtre.
- `NativeWindowBounds` : calcul des limites demandées en coordonnées écran.
- `FloatingService` / `WindowGeometry` : démo locale et ses gestes.

L’ancien `WindowHost`, son interface AIDL et la dépendance Shizuku sont supprimés de l’APK. Leur code reste dans l’historique Git des versions 0.1.

## Sources et licences

Code original FloatX : MIT (`LICENSE`). Principe de lancement étudié dans [Taskbar](https://github.com/farmerbb/Taskbar), Braden Farmer, Apache-2.0 ; aucun fichier source de Taskbar n’est copié.

Dépendance embarquée : [Android Hidden API Bypass 6.1](https://github.com/LSPosed/AndroidHiddenApiBypass), Copyright 2021-2025 LSPosed, Apache-2.0. Copie de la licence et notice dans `app/src/main/assets/licenses/`.

Aucun composant ni ressource propriétaire XOS n’est intégré. Projet indépendant, non affilié à Infinix. L’appel à une API non publique sert ici à la compatibilité de lancement Android et ne donne pas de permissions système supplémentaires.
