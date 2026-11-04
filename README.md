# FloatX 0.3 — cadre arrondi avec Shizuku direct

Cette version remet les vraies applications dans le cadre FloatX : coins arrondis, poignée de déplacement, coin de redimensionnement, agrandissement et réduction en bulle. Le moteur 0.1 qui attendait un `UserService` Shizuku est remplacé par des **appels Binder directs** via `ShizukuBinderWrapper`.

## Installer et essayer

1. Télécharger **FloatX-APK** dans le dernier lancement réussi de **Actions → APK Android**.
2. Décompresser, désinstaller l’ancien FloatX et installer `FloatX-0.3.0-debug.apk` (les APK debug n’ont pas encore de signature stable).
3. Dans FloatX, autoriser l’affichage flottant, démarrer Shizuku et autoriser FloatX.
4. Utiliser **Ouvrir une appli dans le cadre FloatX**, puis choisir **Calculatrice**.
5. Vérifier que le contenu s’affiche dans le cadre arrondi, que les touches répondent, puis essayer déplacement, redimensionnement et réduction/restauration en bulle.

**Pas encore vérifié sur le téléphone de l’utilisateur.** Une compilation réussie n’atteste pas la compatibilité avec son firmware XOS. En cas de blocage, le code S01–S04 situe l’étape concernée ; envoyer une capture de ce message. L’attente est limitée à 25 secondes côté interface.

Le mode cadre direct nécessite Android 14 ou supérieur et une autorisation Shizuku active. Aucune modification de Shizuku, du firmware, du lanceur ou des permissions système persistantes n’est effectuée par FloatX.

## Pourquoi un autre chemin de connexion

Des utilisateurs signalent que `LoadedApk.makeApplication()` empêche le démarrage des UserServices sur certains firmwares MediaTek, y compris sous Android 16 : https://github.com/RikkaApps/Shizuku/issues/1198 . Ce problème est une cause plausible du précédent diagnostic « affichage prêt, moteur non connecté » ; il n’a pas été confirmé par les logs du téléphone de l’utilisateur.

Le nouveau code n’appelle ni `bindUserService`, ni `newProcess`, ni `LoadedApk.makeApplication`. Il crée des interfaces système Binder déléguées à Shizuku dans le processus FloatX :

- `IDisplayManager` : création d’un écran virtuel public, sans recopie de l’écran principal ni contenu sécurisé.
- Un `DisplayManagerGlobal` séparé : le singleton Android du processus reste intact.
- Attribution du paquet `com.android.shell`, correspondant à l’identité déléguée lorsque Shizuku est démarré en ADB.
- `IActivityTaskManager` : lancement de l’activité sélectionnée sur cet écran, en plein écran à l’intérieur de notre cadre. La disposition des arguments est contrôlée avant l’appel.
- `IInputManager` : interaction tactile dans l’écran virtuel de cette session.

Le composant à lancer provient du choix explicite de l’utilisateur ; aucune commande shell libre n’est exécutée. Les écrans créés sont libérés à la fermeture, avec suppression de leur contenu. Une seule fenêtre FloatX est gérée à la fois. Le mode Shizuku ADB (UID shell) est celui visé ; le mode root n’est pas nécessaire ni validé.

## Apparence, bulles et limites

Le cadre est dessiné par FloatX. Son contenu est une TextureView avec accélération graphique explicite. Réduire en bulle masque la fenêtre sans détacher sa surface ; restaurer la bulle réaffiche la même session, sans relancer l’application. XOS peut néanmoins interrompre des processus en arrière-plan.

Le clavier natif dans l’écran virtuel reste expérimental. **⌨** ouvre la saisie de test : sélectionner un champ dans l’appli, saisir dans FloatX puis envoyer avec **➤**. Certains caractères, accents ou emojis peuvent être refusés par la table de clavier Android. Le bouton **‹** envoie Retour. Ce n’est pas un clavier complètement intégré.

Les contenus protégés peuvent rester noirs. Certaines applis refusent les écrans secondaires. Le bouton d’agrandissement remplit l’espace disponible avec le cadre FloatX ; il ne transfère pas l’activité vers l’écran principal. Le panneau latéral et les badges de notification de la maquette ne sont pas implémentés.

## Modes conservés

- **Mode fenêtres système — secours** : lancement natif de la série 0.2, sans Shizuku, avec les décorations système. L’ouverture de Calculatrice a été confirmée sur le téléphone de l’utilisateur ; la conservation de son calcul après réduction/réouverture n’a pas été validée.
- **Essayer les gestes — démo** : conversation fictive locale, indépendante de Shizuku, dont les gestes ont été validés par l’utilisateur.

## Compilation et validation

JDK 17, Gradle 8.9, Android SDK 35, AGP 8.7.3 et Kotlin 2.0.21. `gradle --no-daemon testDebugUnitTest lintDebug assembleDebug`.

Les tests couvrent les limites de fenêtre, les barres/découpes et la disposition des arguments de lancement Binder. Les signatures inconnues sont rejetées plutôt que devinées. La CI ne remplace pas les essais de rendu, tactile, clavier et maintien en arrière-plan sur XOS.

## Sources et licences

Code original FloatX : MIT (`LICENSE`). Dépendances : AndroidHiddenApiBypass 6.1 (LSPosed, Apache-2.0), Shizuku API/Provider 13.1.5 (RikkaApps, MIT). Copies des licences et notices dans `app/src/main/assets/licenses/`.

Le lancement natif de secours s’inspire du mécanisme de Taskbar (Braden Farmer, Apache-2.0). Aucun fichier source Taskbar ou composant propriétaire XOS n’est intégré. Projet non affilié à Infinix.
