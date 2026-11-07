# Signature permanente FloatX

La clé privée ne se trouve pas dans ce dépôt. Les fichiers PEM et SHA-256 sont publics et identifient la signature permanente.

1. Ouvrir la sauvegarde privée remise avec la version 1.0.0.
2. Dans Settings → Secrets and variables → Actions, créer un secret de dépôt nommé `FLOATX_SIGNING_BUNDLE`.
3. Copier comme valeur tout le contenu du JSON de la sauvegarde, sans le publier dans un commit, une issue ou un commentaire.
4. Après fusion sur main, le workflow **FloatX stable signé** produit automatiquement l’APK signée. Il est aussi lançable manuellement sur main. Sans le secret, il échoue explicitement ; il ne génère jamais une autre clé.

Les pull requests produisent un aperçu au paquet distinct `com.josias.floatx.preview`, et une APK release non signée réservée à la signature hors ligne. Elles n’utilisent pas la clé privée. Le workflow stable vérifie le certificat contre `floatx-release-sha256.txt` à chaque compilation.

La première transition depuis les APK debug nécessite une désinstallation de l’ancienne application si sa signature diffère. Les mises à jour stables conservent `com.josias.floatx`, la même clé et un versionCode croissant ; elles s’installent ensuite par-dessus la version stable et conservent ses données. Conserver la sauvegarde de signature en privé, sans en générer une nouvelle pour une mise à jour.
