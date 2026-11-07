# FloatX 1.0.1 — barre discrète et groupe de fenêtres

La poignée ⋯ est remplacée par une petite barre centrée en haut de chaque fenêtre (36 × 4 dp, zone tactile invisible plus large). Glisser déplace la fenêtre, toucher ouvre les commandes. Une bande de 18 dp réserve la place de la poignée pour ne pas recouvrir les boutons de l’application. « Retour dans l’application » est retiré du menu ; les dispositions, la réduction en bulle et les quatre coins de redimensionnement restent disponibles.

Les fenêtres ouvertes sont regroupées dans une seule tâche **FloatX · Fenêtres**, distincte de l’accueil de configuration. La touche Carré / Applications récentes permet de choisir une autre application. Les fenêtres, menus, bulles et la barre FloatX sont alors masqués et ne captent plus les touchers ni le clavier. Toucher la carte du groupe restaure les fenêtres visibles dans leurs positions et tailles actuelles ; une fenêtre réduite reste réduite. La carte dispose d’un aperçu du groupe, conservé uniquement en mémoire. Aucun aperçu n’est écrit sur disque par FloatX. Les tâches créées sur les écrans virtuels sont exclues des récents afin de ne pas multiplier les cartes du groupe.

L’accueil propose « Revenir à mes fenêtres » ; la notification revient également au groupe. Ouvrir une appli depuis l’accueil, une tuile ou la barre rejoint ce même espace. Balayer la carte du groupe pour la supprimer ferme les sessions et la barre. Supprimer la carte de l’accueil seul ne ferme pas les fenêtres.

Les sessions et leurs surfaces restent vivantes dans le service tant qu’Android le conserve. La fermeture du service, un arrêt forcé, un redémarrage ou la suppression du groupe ne peuvent pas préserver la navigation interne des applications. Une carte périmée n’essaie pas de relancer un service vide ni les applis automatiquement. La nouvelle interface ne modifie pas les préférences, les favoris ou les associations de tuiles.

Version : `1.0.1`, `versionCode 101`. Le paquet stable et le certificat permanent restent inchangés. Le workflow de PR exécute les tests debug **et release**, lint debug/release et les deux compilations. Les APK d’aperçu gardent `com.josias.floatx.preview`. La distribution stable dépend toujours du secret privé et du contrôle de certificat décrits dans `signing/README.md`.

À vérifier sur Infinix / XOS :

- Toucher et glisser la barre ; vérifier que les commandes de l’app juste dessous restent accessibles et que « Retour dans l’application » a disparu.
- Ouvrir deux puis quatre applications ; passer par Carré à une autre appli : aucune fenêtre, bulle ou barre ne doit rester au-dessus.
- Toucher la carte FloatX : vérifier tailles, positions, contenu déjà ouvert, ordre des fenêtres et clavier externe.
- Réduire une seule fenêtre en bulle avant la bascule : elle doit rester réduite au retour.
- Vérifier la rotation, l’ajout d’une appli par tuile et l’accès au groupe depuis la notification.
- Supprimer la carte de configuration seule puis la carte du groupe ; seule la seconde doit fermer les sessions.
- Arrêter Shizuku et le service : vérifier les messages et l’absence de faux retour à des sessions perdues.
- Installer une future stable de versionCode supérieur par-dessus une stable signée avec le même certificat ; vérifier la conservation des favoris et réglages. Ne pas désinstaller l’ancienne debug pour tester l’aperçu.

Le lanceur XOS choisit la présentation et le moment de capture de sa carte : l’aperçu et les transitions doivent être confirmés sur le téléphone, pas uniquement par la compilation.

# FloatX 1.0.0

L’accueil présente l’état de connexion, l’ouverture d’une appli, l’organisation de quatre applis et les favoris avec leurs logos. La barre flottante et le centre de contrôle restent directement accessibles. Les réglages regroupent la connexion Shizuku, la démo, le mode système de secours, le diagnostic et la fermeture de toutes les fenêtres.

La version stable utilise une clé permanente privée. Le certificat public est conservé dans `signing/` ; la procédure GitHub Actions est décrite dans `signing/README.md`. Les aperçus sont séparés du paquet stable et ne servent pas à la distribution des mises à jour.

Fonctions validées sur Infinix par l’utilisateur avant cette préparation : plusieurs applis, quatre coins redimensionnables, priorité au toucher, bulles, clavier externe, retour à l’accueil de l’appli, tuiles du centre de contrôle et dispositions. Le moteur reste local et dépend de Shizuku actif. La nouvelle présentation et l’installation de la version signée nécessitent une vérification sur téléphone.

# FloatX 0.5.1 — logos lisibles dans les tuiles

Les raccourcis du centre de contrôle utilisent le logo monochrome de l’icône adaptative lorsqu’il existe, puis son premier plan. Pour les icônes anciennes à fond uni, ce fond est retiré du masque ; les transparences du logo sont conservées. Le nom et les icônes des tuiles sont rafraîchis au retour dans FloatX. XOS garde la maîtrise de la couleur des tuiles : la couleur d’origine du logo n’est pas garantie par l’API Android (https://developer.android.com/develop/ui/views/quicksettings-tiles).

Le menu « Épingler / Verrouiller » de l’écran des applications récentes appartient au lanceur XOS. Cette version ne prétend pas y injecter de commande. Réduire une fenêtre FloatX en bulle reste disponible dans son propre menu ⋯ ; ouvrir une appli en flottant reste accessible par les raccourcis du centre de contrôle.

# FloatX 0.5.0 — centre de contrôle et dispositions

Dans FloatX, « Configurer le centre de contrôle » associe quatre raccourcis à des applis. Accepter l’ajout proposé par Android, ou ouvrir le centre de contrôle → Modifier et ajouter FloatX 1 à 4. Le nom et l’icône suivent l’application choisie. Chaque appui replie le panneau et ouvre/restaure l’appli en flottant. La tuile « FloatX · Choisir » permet de choisir une appli à chaque fois. Shizuku et l’autorisation d’affichage flottant doivent être prêts ; sinon FloatX ouvre la configuration. L’affichage des tuiles et leur ajout dépendent du panneau XOS.

Le menu ⋯ → Placement propose moitié gauche/droite, centre et chacun des quatre quarts. « Organiser les fenêtres (1 à 4) » place jusqu’à quatre fenêtres déjà ouvertes (une au centre, deux côte à côte, trois ou quatre dans les quarts ; la fenêtre sélectionnée en premier), et réduit les autres en bulles. Le bouton principal « Ouvrir jusqu’à quatre applis en grille » sélectionne et lance directement les applications. La grille laisse une place à la barre de raccourcis et les positions se recalculent après rotation. Glisser ou redimensionner manuellement quitte le placement automatique de cette fenêtre. Le minimum manuel passe à 120 × 140 dp pour permettre les quarts sur téléphone.

# FloatX 0.4.5 — Retour du menu et accueil de l’appli

La capture globale de Retour introduite en 0.4.4 est retirée (BackRouter et sa fenêtre auxiliaire supprimés). Le panneau principal reprend le comportement de 0.4.3. Les diagnostics de perte de Shizuku restent disponibles.

Le menu ⋯ transmet toujours Retour à l’application flottante. Si Android laisse ensuite l’écran virtuel sans tâche visible, deux observations successives (espacées de 900 ms) réaffichent l’activité de lancement de l’appli sur le même écran. Les tâches actives ne sont pas réinitialisées ; une lecture inconnue de la visibilité empêche toute relance. Le contrôle est suspendu lorsque la fenêtre est minimisée et retiré à sa fermeture.

À vérifier sur XOS : revenir d’une conversation à la liste, puis faire Retour à la racine et vérifier que l’accueil revient au lieu de laisser un cadre transparent. Le Retour du téléphone conserve son fonctionnement antérieur.

# FloatX 0.4.4 — Retour et perte de Shizuku

Le panneau de réglages est retiré de l’historique après ouverture d’une fenêtre ou de la barre. Une fenêtre auxiliaire transparente de 1 pixel capte Retour sur l’écran principal lorsque l’utilisateur sélectionne une application flottante ; la commande est injectée dans son écran virtuel. Cette fenêtre n’est pas un éditeur et ne recouvre pas le clavier. La capture est retirée quand la fenêtre sélectionnée est réduite, fermée, ou quand Shizuku s’arrête. Le menu ⋯ conserve sa commande Retour.

Le rendu et les commandes utilisent des Binder locaux. FloatX ne demande pas la permission INTERNET. Une coupure Wi-Fi peut toutefois s’accompagner d’un arrêt de Shizuku démarré via le débogage sans fil, selon le système. Cette version détecte la perte du Binder, affiche la cause et permet de recréer une session en touchant son raccourci après redémarrage de Shizuku. Elle ne garantit pas de maintenir Shizuku actif si XOS l’arrête. Conserver le débogage USB et les options développeur activés et autoriser Shizuku en arrière-plan, conformément à https://shizuku.rikka.app/guide/setup/.

À vérifier sur téléphone : Retour par geste et par bouton dans une conversation, clavier externe, réduction de toutes les fenêtres, puis ouverture de l’appli du dessous ; coupure du Wi-Fi en contrôlant l’état Shizuku.

# FloatX 0.4.3 — redimensionnement aux quatre coins

Glisser depuis n’importe lequel des quatre coins ajuste la taille. Le coin opposé reste fixe, y compris lorsque la taille minimum ou une limite de l’écran est atteinte. Les zones de prise restent invisibles (24 dp), sans ajouter de barre au cadre. Le menu ⋯, les coins arrondis et le passage au premier plan sont conservés.

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
