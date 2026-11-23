# Règles de réduction du code pour la version publiée.
# Room, WorkManager et Compose fournissent leurs propres règles.

# SQLCipher (chiffrement de la base) : la bibliothèque native appelle des classes Java par leur nom.
# Ses propres règles le prévoient dans les versions récentes ; on les répète par sécurité.
-keep,includedescriptorclasses class net.zetetic.database.** { *; }
-keep,includedescriptorclasses interface net.zetetic.database.** { *; }
-dontwarn net.zetetic.database.**
