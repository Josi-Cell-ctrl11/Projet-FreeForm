# FloatX

Prototype Android de fenêtres flottantes inspirées de XOS, en Kotlin. Le lanceur et le système XOS restent en place. Une seule fenêtre à la fois dans cette première version.

## Télécharger et essayer

1. Ouvrir **Actions → APK Android** et sélectionner le dernier lancement réussi (vert).
2. Dans **Artifacts**, télécharger **FloatX-APK**, extraire le ZIP et installer `FloatX-0.1.0-debug.apk` sur le téléphone.
3. Ouvrir FloatX et autoriser **l’affichage par-dessus les autres applications**.
4. Choisir **Essayer les gestes — démo**.
5. Déplacer la fenêtre par sa poignée, tirer le coin inférieur droit, réduire avec **−**, puis toucher la bulle pour restaurer. La bulle se déplace et revient sur le bord.
6. **×** ferme la session. La notification et le bouton **Fermer toutes les fenêtres** permettent aussi de tout arrêter.

L’APK est signé avec une clé de développement temporaire de GitHub Actions. Pour le moment, une nouvelle compilation peut nécessiter de désinstaller l’ancienne version. Une signature stable sera ajoutée avant les versions distribuées durablement.

## Deux modes clairement distincts

### Démo interactive — sans accès système

Conversation fictive avec Alex, saisie locale et gestes réels dans une fenêtre Android au-dessus des autres applications. Aucun message n’est envoyé. Ce mode permet de vérifier le design et les gestes indépendamment de la compatibilité du moteur système.

### Vraies applications — expérimental

Nécessite **Shizuku v13 ou supérieur**, démarré et autorisé pour FloatX. Ce moteur est optionnel : l’interface fonctionne sans lui. Guide officiel : https://shizuku.rikka.app/guide/setup/

FloatX crée un écran virtuel avec l’identité shell autorisée par Shizuku, lance l’activité choisie sur cet écran et l’affiche dans sa propre fenêtre. Les interactions tactiles sont retransmises à cet écran. La surface reste attachée lorsque la fenêtre est réduite ; FloatX ne garantit cependant pas que XOS laissera l’application active en arrière-plan.

**Pas encore validé sur un Infinix HOT 70 / Android 16 / XOS 16.3.** Certains fabricants ou certaines applications peuvent refuser l’affichage secondaire ou l’injection des interactions. Les refus sont affichés dans la fenêtre. Une compilation réussie ne prouve pas la compatibilité sur téléphone.

Le clavier natif dans un écran virtuel n’est pas garanti. Le bouton **⌨** fournit une **saisie de test** : sélectionner d’abord un champ dans l’application, saisir dans le champ FloatX puis appuyer sur **➤**. Ce mécanisme repose sur la table de caractères du clavier Android et peut refuser des caractères, notamment certains accents et emojis. Ce n’est pas encore un clavier pleinement intégré. Le bouton **‹** envoie Retour.

Les contenus protégés peuvent rester noirs. Certaines applications peuvent se rouvrir au lieu de reprendre leur session. L’agrandissement remplit l’espace disponible avec la fenêtre FloatX ; il ne transfère pas encore la tâche vers l’écran principal. Le badge de notification et le panneau latéral de la maquette ne sont pas implémentés dans cette version.

## Compilation

- JDK 17, Gradle 8.9, Android SDK 35.
- Android Gradle Plugin 8.7.3, Kotlin 2.0.21 ; Android 11 minimum.
- `gradle testDebugUnitTest lintDebug assembleDebug`

GitHub Actions installe Gradle et le SDK. Pour une compilation locale, installer ces versions ou importer dans Android Studio. Aucun secret ni clé privée n’est nécessaire pour l’APK debug.

## Architecture

- `MainActivity` : autorisations, mode démo, sélection explicite d’une activité installée.
- `FloatingService` : notification persistante, fenêtre, bulle, affichage TextureView, cycle de vie et interaction utilisateur.
- `WindowGeometry` : limites de taille et position ; tests des sorties d’écran et de la rotation.
- `IWindowHost` / `WindowHost` : interface Binder et écran virtuel dans le processus Shizuku autorisé. Aucune commande shell fournie par l’utilisateur ; seul le composant sélectionné est lancé via des arguments séparés.

## Validation sur téléphone

Merci de noter le modèle, la version complète de XOS, le mode utilisé et tout message de refus.

- Démo : déplacement, petite/grande taille, rotation portrait/paysage, saisie clavier.
- Bulle : déplacement, restauration, puis fermeture depuis la notification.
- Vraies applis : commencer par Calculatrice, puis Chrome, puis WhatsApp. Vérifier contenu, clics, défilement, Retour et saisie de test.
- Après fermeture : aucune fenêtre ni bulle restante ; plus de notification de session.
- Après arrêt de Shizuku : échec explicite, puis nouvelle session après redémarrage.

## Origine et licences

Implémentation originale, inspirée des concepts de fenêtres Android, Mi-FreeForm et FreeformShell. Aucun code de ces projets n’est copié. Aucun composant ni ressource propriétaire XOS n’est intégré ; ce projet n’est pas affilié à Infinix.

Le code du projet est sous licence MIT (voir `LICENSE`). Shizuku API et Provider 13.1.5 : licence MIT, RikkaApps, https://github.com/RikkaApps/Shizuku-API . Les dépendances AndroidX transitives conservent leurs licences respectives.
