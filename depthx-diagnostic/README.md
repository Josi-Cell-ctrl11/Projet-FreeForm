# DepthX Diagnostic 0.1.0

Première étape de l’application de fond d’écran à effet de profondeur : inspecter les composants réellement disponibles sur l’Infinix. Ce diagnostic ne crée pas encore de fond à effet de profondeur et ne confirme pas sa disponibilité par de simples noms de composants.

L’application affiche Android, le modèle et le build ; elle ne déduit pas la version XOS du build. Elle examine les paquets de personnalisation visibles et leurs activités, y compris les composants désactivés. Seules les pages dont le nom évoque un menu de personnalisation, exportées, actives et utilisables avec les permissions actuelles sont proposées à l’ouverture. Les noms évoquant l’application d’un fond, l’effacement, la réinitialisation ou l’authentification sont exclus. Un bouton ne contourne jamais un refus de XOS.

Aucun réglage n’est écrit, aucune activité n’est activée, aucun fond n’est changé. Aucune permission réseau, photo, stockage général ou identifiant de l’appareil n’est demandée. QUERY_ALL_PACKAGES sert uniquement à la découverte locale des fournisseurs OEM dont les noms sont inconnus ; ce prototype est destiné à l’installation directe, pas à la soumission Play Store.

Le rapport contient modèle, version Android, build, correctif, paquets de personnalisation et informations des activités. Il exclut IMEI, numéro de série, comptes et liste des applications sans rapport avec la personnalisation. Il reste dans l’application jusqu’à ce que l’utilisateur choisisse Copier ou Enregistrer. Ouvrir un écran système reste une action manuelle et l’utilisateur peut y modifier ses réglages.

Utilisation : installer l’APK, attendre l’analyse, essayer les menus proposés, puis copier le rapport dans la conversation. Si aucun menu n’est accessible, le rapport permet de distinguer une activité privée, désactivée ou absente et de choisir la suite. La présence d’un nom « depth » ne signifie pas que la fonction est disponible.

Projet distinct de FloatX, paquet `com.josias.depthxdiag`. Les tests unitaires vérifient le filtrage des accès privés, des permissions, des composants désactivés et des écrans à effet de bord. La compilation et lint ne valident pas l’ouverture réelle des menus XOS ; cela doit être testé sur le téléphone.
