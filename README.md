# FloatX 0.4.2 — commandes ⋯ et premier plan

La barre supérieure est supprimée. Un bouton ⋯ est superposé en haut, au centre du contenu : appuyer ouvre les commandes (réduire, agrandir/restaurer, retour, fermer), le faire glisser déplace la fenêtre. Les coins restent arrondis à 12 dp.

Toucher une fenêtre la place devant les autres une fois le geste terminé, en conservant son écran virtuel et sa SurfaceTexture. Une bulle ou un raccourci restaure aussi la fenêtre au premier plan. Le changement d’ordre est effectué entre fenêtres WindowManager, sans relancer l’application. À valider sur XOS : alternance de deux fenêtres, continuité d’un défilement et clavier externe après changement de priorité.

# FloatX 0.4.1 — cadre discret

La barre inférieure FLOATX est supprimée. Le contenu occupe tout le bas du cadre, avec des coins de 12 dp. La barre supérieure passe de 48 à 36 dp et le bouton retour rejoint les commandes du haut. Le redimensionnement reste disponible en glissant le coin inférieur droit, sans triangle ni bande inférieure. Les fenêtres multiples, raccourcis et réglages du clavier de 0.4.0 sont conservés.

# FloatX 0.4.0

Cette version ajoute plusieurs fenêtres indépendantes et une barre flottante de raccourcis.

- Utiliser « Choisir les icônes de ma barre » pour épingler les applis ; cocher puis enregistrer.
- Une pression sur une icône ouvre l’appli dans FloatX ou restaure sa fenêtre existante.
- Appui long sur une icône : retirer le raccourci. Le bouton + ouvre la sélection.
- Chaque fenêtre dispose de sa fermeture, de son redimensionnement et de sa bulle.
- La densité du contenu est adaptée aux petites dimensions pour conserver tout le contenu dans la surface. Le cadre garde un minimum de 180 × 200 dp pour ses commandes.
- L’écran virtuel demande le mode plein écran interne, sans décorations système, afin d’enlever la seconde barre de titre et la barre de navigation internes.
- Le clavier Android est demandé sur l’écran principal via IME_POLICY_FALLBACK_DISPLAY, au lieu d’être contenu dans la fenêtre. Aucun champ de saisie de test n’est ajouté.

Les réglages concernent les écrans virtuels de FloatX, sans changement global de XOS. Chaque refus système apparaît dans le diagnostic. Les profils utilisés sont 1290 (privé, TRUSTED sans décorations), puis 266 si la permission est refusée. L’effet des commandes de fenêtre et du clavier dépend du système XOS et doit être vérifié sur le téléphone. La compatibilité universelle n’est pas garantie.

Les tests de géométrie, de densité et de signature Binder, lint et la compilation APK sont exécutés par GitHub Actions. Le mode système de secours et la démo restent disponibles.

## Historique technique 0.3.1

# FloatX 0.3.1 — cadre arrondi avec Shizuku direct

Cette version remet les vraies applications dans le cadre FloatX : coins arrondis, poignée de déplacement, coin de redimensionnement, agrandissement et réduction en bulle. Le moteur 0.1 qui attendait un `UserService` Shizuku est remplacé par des **appels Binder directs** via `ShizukuBinderWrapper`.

## Installer et essayer

1. Télécharger **FloatX-APK** dans le dernier lancement réussi de **Actions → APK Android**.
2. Décompresser, désinstaller l’ancien FloatX et installer `FloatX-0.3.1-debug.apk` (les APK debug n’ont pas encore de signature stable).
3. Dans FloatX, autoriser l’affichage flottant, démarrer Shizuku et autoriser FloatX.
4. Utiliser **Ouvrir une appli dans le cadre FloatX**, puis choisir **Calculatrice**.
5. Vérifier que le contenu s’affiche dans le cadre arrondi, que les touches répondent, puis essayer déplacement, redimensionnement et réduction/restauration en bulle.

**Calculatrice et le clavier ont été validés par l’utilisateur sur son téléphone.** La compatibilité des autres applis et du nouveau profil TRUSTED reste à confirmer sur son firmware XOS. En cas de blocage, le code S01–S04 situe l’étape concernée ; envoyer une capture de ce message. L’attente est limitée à 25 secondes côté interface.

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

Le clavier Android est utilisé directement. À la demande de l’utilisateur, le bouton et le champ de saisie de secours ainsi que l’injection de texte correspondante ont été supprimés. Le bouton **‹** envoie Retour.

### Profils d’affichage et permissions

La création essaie dans cet ordre : **1803** (PUBLIC, PRESENTATION, OWN_CONTENT_ONLY, DESTROY_CONTENT_ON_REMOVAL, SHOULD_SHOW_SYSTEM_DECORATIONS, TRUSTED), puis **1291** (TRUSTED sans décorations) si Android refuse la première création, puis **267**, le profil standard qui fonctionnait pour Calculatrice. Seul un refus de sécurité lors de la création déclenche ces replis.

La permission `ADD_TRUSTED_DISPLAY` de l’identité Shizuku est contrôlée à titre de diagnostic ; elle n’est ni accordée ni modifiée par FloatX. Une tentative effective confirme le profil accepté. Le résultat et les refus sont enregistrés localement et affichés à la réouverture de FloatX. En repli standard, un message l’indique explicitement.

Si l’écran est TRUSTED, FloatX demande `IWindowManager.setDisplayImePolicy(displayId, LOCAL)` pour cette session. L’acceptation de cette demande ne garantit pas à elle seule que chaque clavier/fenêtre secondaire affichera son interface de la même manière sur tous les firmwares. Les décorations système ne remplacent pas cette politique IME. Les restrictions de taille et d’orientation sont ignorées sur les écrans TRUSTED quand l’API Android correspondante est disponible.

Les APIs d’entrée `injectInputEventToTarget` et `injectInputEvent` restent prises en charge dans cet ordre. Les sources AOSP consultées contiennent encore les deux ; leur disponibilité n’est pas déduite uniquement du numéro de version Android.

Les contenus protégés peuvent rester noirs. Certaines applis refusent les écrans secondaires. Le bouton d’agrandissement remplit l’espace disponible avec le cadre FloatX ; il ne transfère pas l’activité vers l’écran principal. Le panneau latéral et les badges de notification de la maquette ne sont pas implémentés.

## Modes conservés

- **Mode fenêtres système — secours** : lancement natif de la série 0.2, sans Shizuku, avec les décorations système. L’ouverture de Calculatrice a été confirmée sur le téléphone de l’utilisateur ; la conservation de son calcul après réduction/réouverture n’a pas été validée.
- **Essayer les gestes — démo** : conversation fictive locale, indépendante de Shizuku, dont les gestes ont été validés par l’utilisateur.

## Compilation et validation

JDK 17, Gradle 8.9, Android SDK 35, AGP 8.7.3 et Kotlin 2.0.21. `gradle --no-daemon testDebugUnitTest lintDebug assembleDebug`.

Les tests couvrent les limites de fenêtre, les barres/découpes, la disposition des arguments de lancement Binder et les profils de repli (sans décorations sur un écran non trusted). Les signatures inconnues sont rejetées plutôt que devinées. La CI ne remplace pas les essais de rendu, tactile, clavier et maintien en arrière-plan sur XOS.

## Sources et licences

Code original FloatX : MIT (`LICENSE`). Dépendances : AndroidHiddenApiBypass 6.1 (LSPosed, Apache-2.0), Shizuku API/Provider 13.1.5 (RikkaApps, MIT). Copies des licences et notices dans `app/src/main/assets/licenses/`.

Le lancement natif de secours s’inspire du mécanisme de Taskbar (Braden Farmer, Apache-2.0). Aucun fichier source Taskbar ou composant propriétaire XOS n’est intégré. Projet non affilié à Infinix.
