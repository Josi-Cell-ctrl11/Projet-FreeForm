# DepthX 0.3.0 — studio de profondeur et fond animé

L’application ouvre maintenant son propre studio : horloge haute et condensée inspirée de la capture utilisateur, date, photo et masque de premier plan. Un seul moteur Canvas dessine photo → horloge → sujet pour l’éditeur, l’aperçu plein écran, l’export PNG et le service de fond animé. Une démo dessinée dans le code montre immédiatement les couches ; aucune capture avec des chiffres déjà imprimés n’est embarquée comme photo.

Fonctions : import via sélecteur de documents (rotation EXIF et réduction mémoire via ImageDecoder), détourage de personnes ML Kit intégré, masque manuel au pinceau/effaceur avec quatre annulations, cadrage/pincement, placement et taille de l’horloge, quatre couleurs, date et cible accueil/verrouillage/les deux. Le masque manuel permet de travailler sur un dessin comme Sasuke lorsque la segmentation portrait n’est pas adaptée.

Enregistrer publie une génération complète de photo + masque + JSON puis change le pointeur actif. Les trois générations les plus récentes sont gardées. L’aperçu utilise la même composition ; exporter un PNG fige l’heure, alors que le fond animé la met à jour chaque minute et après changement d’heure/fuseau. Le fond ne programme aucun dessin quand il est invisible. Le sélecteur système choisit les destinations réellement disponibles sur le téléphone.

Limite explicite : l’horloge est celle de DepthX dans le fond animé. L’horloge système XOS peut rester au-dessus et produire un doublon. L’application ne la masque pas et ne simule pas le déverrouillage ; aucun code PIN/biométrie n’est remplacé. L’aperçu plein écran est un aperçu dans l’application, pas un nouvel écran sécurisé de verrouillage. Les réglages XOS restent accessibles depuis un bouton séparé.

Paquet com.josias.depthx, version 0.3.0, code 3, même clé permanente que 0.2.0. La release est signée hors ligne avec vérification de la référence publique. La clé privée ne doit jamais entrer dans GitHub. Voir INSPIRATIONS.md pour les projets et documentations étudiés ; aucun code d’Atmo n’est repris.

Validation : tests de géométrie et de politique des écrans, tests Canvas natifs Robolectric vérifiant que le premier plan occulte réellement les pixels de l’horloge, et sauvegarde/restauration de masque et réglages. Tests debug/release, lint debug/release et deux compilations dans Actions. Restent à tester sur Infinix : modèle automatique, pinceaux, import réel, installation du fond, horloge système et rendu au verrouillage.

# DepthX 0.2.0 — accès aux menus natifs

Le rapport réel du X6895 a identifié un menu `DepthFieldWallpaperActivity` exporté et un guide de profondeur du lanceur, ainsi que les entrées principales du sélecteur de fond et du générateur IA. DepthX les présente en premier avec des libellés distincts et recontrôle disponibilité, activation, export et permission à chaque ouverture. Il ne fournit aucun extra d’intention inventé et ne lance pas les aperçus qui pourraient exiger des données inconnues.

L’activité `LockScreenEditActivity` du générateur IA est privée : elle n’est pas lancée directement. Les noms repérés et l’acceptation d’un startActivity ne confirment pas que l’effet peut être appliqué ni qu’il concerne l’horloge de verrouillage ; le contenu des pages doit être testé sur le téléphone. Le guide du lanceur peut concerner seulement l’accueil.

Paquet `com.josias.depthx`, nom **DepthX**, version 0.2.0, code 2. Cette application est distincte du diagnostic initial et de FloatX. La release est signée hors ligne avec sa propre clé permanente ; la clé privée n’est jamais déposée dans GitHub. La référence publique du certificat est dans `signing/depthx-release-sha256.txt`.

Le rapport reste disponible et inclut les demandes d’ouverture acceptées ou refusées, sans affirmer que l’écran cible s’est affiché correctement. Aucun écran n’est ouvert automatiquement, aucun réglage n’est écrit et aucun fond n’est changé par DepthX. Le diagnostic complet reste disponible en dessous des accès ciblés.

# DepthX Diagnostic 0.1.0

Première étape de l’application de fond d’écran à effet de profondeur : inspecter les composants réellement disponibles sur l’Infinix. Ce diagnostic ne crée pas encore de fond à effet de profondeur et ne confirme pas sa disponibilité par de simples noms de composants.

L’application affiche Android, le modèle et le build ; elle ne déduit pas la version XOS du build. Elle examine les paquets de personnalisation visibles et leurs activités, y compris les composants désactivés. Seules les pages dont le nom évoque un menu de personnalisation, exportées, actives et utilisables avec les permissions actuelles sont proposées à l’ouverture. Les noms évoquant l’application d’un fond, l’effacement, la réinitialisation ou l’authentification sont exclus. Un bouton ne contourne jamais un refus de XOS.

Aucun réglage n’est écrit, aucune activité n’est activée, aucun fond n’est changé. Aucune permission réseau, photo, stockage général ou identifiant de l’appareil n’est demandée. QUERY_ALL_PACKAGES sert uniquement à la découverte locale des fournisseurs OEM dont les noms sont inconnus ; ce prototype est destiné à l’installation directe, pas à la soumission Play Store.

Le rapport contient modèle, version Android, build, correctif, paquets de personnalisation et informations des activités. Il exclut IMEI, numéro de série, comptes et liste des applications sans rapport avec la personnalisation. Il reste dans l’application jusqu’à ce que l’utilisateur choisisse Copier ou Enregistrer. Ouvrir un écran système reste une action manuelle et l’utilisateur peut y modifier ses réglages.

Utilisation : installer l’APK, attendre l’analyse, essayer les menus proposés, puis copier le rapport dans la conversation. Si aucun menu n’est accessible, le rapport permet de distinguer une activité privée, désactivée ou absente et de choisir la suite. La présence d’un nom « depth » ne signifie pas que la fonction est disponible.

Projet distinct de FloatX, paquet `com.josias.depthxdiag`. Les tests unitaires vérifient le filtrage des accès privés, des permissions, des composants désactivés et des écrans à effet de bord. La compilation et lint ne valident pas l’ouverture réelle des menus XOS ; cela doit être testé sur le téléphone.
