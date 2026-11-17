package fr.lunaison.ui.backup

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Garde-fous sur les fichiers de configuration de l'appli : aucune permission réseau ni de stockage, et seul le dossier
 * privé de la dernière sauvegarde chiffrée peut être copié par le système (nuage comme transfert direct).
 */
class ManifestRulesTest {

    private val appMain = File("../app/src/main")

    private fun withoutComments(file: File): String = file.readText().replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")

    @Test
    fun `le manifeste n ajoute ni Internet ni permission de stockage`() {
        val manifest = withoutComments(File(appMain, "AndroidManifest.xml"))
        val permissions = Regex("""<uses-permission[^>]*android:name="([^"]+)"""").findAll(manifest).map { it.groupValues[1] }.toSet()

        assertEquals(setOf("android.permission.POST_NOTIFICATIONS", "android.permission.CAMERA"), permissions)
        assertFalse(manifest.contains("INTERNET"))
        assertFalse(manifest.contains("EXTERNAL_STORAGE"))
    }

    @Test
    fun `l agent de sauvegarde est declare et la sauvegarde systeme active`() {
        val manifest = withoutComments(File(appMain, "AndroidManifest.xml"))

        assertTrue(manifest.contains("""android:backupAgent=".LunaisonBackupAgent""""))
        assertTrue(manifest.contains("""android:allowBackup="${'$'}{allowAutoBackup}""""))
        assertTrue(File("../app/build.gradle.kts").readText().contains("""getOrElse("true")"""))
    }

    @Test
    fun `seul le dossier prive de la derniere sauvegarde chiffree peut etre copie`() {
        val rules = withoutComments(File(appMain, "res/xml/data_extraction_rules.xml"))
        val includes = Regex("""<include [^>]*/>""").findAll(rules).map { it.value }.toList()

        // Un include dans le nuage, un dans le transfert direct, tous deux sur le même dossier du domaine file.
        assertEquals(2, includes.size)
        includes.forEach { include ->
            assertTrue(include.contains("""domain="file""""), include)
            assertTrue(include.contains("""path="${PrivateCopyFile.DIRECTORY}""""), include)
        }
        assertFalse(rules.contains("""domain="database""""))
        assertFalse(rules.contains("""domain="sharedpref""""))
        assertTrue(rules.contains("""disableIfNoEncryptionCapabilities="true""""))
    }
}
