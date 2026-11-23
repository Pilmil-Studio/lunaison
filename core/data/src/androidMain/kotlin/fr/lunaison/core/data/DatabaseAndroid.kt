package fr.lunaison.core.data

import android.content.Context
import androidx.room.Room

/**
 * Base Android : même fichier (`lunaison.db`) et même moteur SQLite du système qu'avant le passage
 * en multiplateforme, donc les données des utilisateurs actuels restent lisibles telles quelles.
 */
fun LunaisonDatabase.Companion.create(context: Context, fileName: String = "lunaison.db"): LunaisonDatabase {
    val appContext = context.applicationContext
    return Room.databaseBuilder<LunaisonDatabase>(
        context = appContext,
        name = appContext.getDatabasePath(fileName).absolutePath,
    ).withMigrations().build()
}

fun LunaisonRepository.Companion.create(context: Context): LunaisonRepository =
    LunaisonRepository(LunaisonDatabase.create(context).dao())

/**
 * Seconde base, ouverte par le code leurre : même schéma, fichier séparé, aucun lien avec la vraie.
 * Le nom du fichier reste anodin pour ne rien révéler à qui parcourrait le stockage de l'appli.
 */
fun LunaisonRepository.Companion.createDecoy(context: Context): LunaisonRepository =
    LunaisonRepository(LunaisonDatabase.create(context, fileName = "lunaison-2.db").dao())
