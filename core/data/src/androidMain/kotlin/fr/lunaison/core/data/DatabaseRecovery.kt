package fr.lunaison.core.data

import android.content.Context

/**
 * Cas où une base chiffrée est arrivée sur ce téléphone sans sa clé (restaurée d'un autre téléphone) : elle est
 * mise de côté, l'appli démarre sur une base neuve, et ce repère permet de proposer la restauration d'un fichier `.pilmil`.
 * Le repère est écrit sur ce téléphone seulement (dossier des bases, exclu de toute sauvegarde).
 */
object DatabaseRecovery {

    fun keyLostNoticePending(context: Context): Boolean = marker(context).exists()

    /** La personne a restauré un fichier ou commencé à zéro : le message n'a plus lieu d'être. */
    fun acknowledgeKeyLost(context: Context) {
        marker(context).delete()
    }

    private fun marker(context: Context) =
        DatabaseFiles(context.applicationContext.getDatabasePath(DATABASE_NAME)).keyLostMarker
}
