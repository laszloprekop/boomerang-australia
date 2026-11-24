package boomerang

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * The dependency rules 1 to 6 of architecture section 2.3 (S6), checked on the text of the main source
 * files. JUnit only. Comments (line, block and KDoc) are left out before any rule is checked: the
 * documentation may name what the code may not use.
 */
class DependencyRulesTest {

    private class Source(val file: File, root: File) {
        /** "game", "scoring", ... ; "" for a file directly in boomerang. */
        val folder: String = file.parentFile.relativeTo(root).path
        /** The file without its comments. Good enough here: no source has "//" or a comment mark inside a string. */
        val text: String = file.readText().replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ").replace(Regex("//.*"), " ")
        val imports: List<String> = text.lines().map { it.trim() }.filter { it.startsWith("import ") }
            .map { it.removePrefix("import ").substringBefore(" as ").trim() }

        /** Everything but the import lines. */
        val code: String = text.lines().filterNot { it.trim().startsWith("import ") }.joinToString("\n")
        override fun toString(): String = if (folder.isEmpty()) file.name else "$folder/${file.name}"
    }

    // Gradle runs the tests in the project directory `app`; the second path is for a run from the repository root.
    private val root: File = listOf("src/main/kotlin/boomerang", "app/src/main/kotlin/boomerang").map(::File).first { it.isDirectory }
    private val sources: List<Source> = root.walkTopDown().filter { it.isFile && it.extension == "kt" }.map { Source(it, root) }.toList()

    private val fixedFolders = setOf("", "game", "scoring", "players", "net")
    private fun inFolder(folder: String) = sources.filter { it.folder == folder }

    /** A folder that is none of the fixed ones holds an edition (today: australia). */
    private val editionSources: List<Source> get() = sources.filter { it.folder !in fixedFolders }

    private fun assertImportsOnly(source: Source, what: String, allowed: (String) -> Boolean) {
        assertEquals(emptyList<String>(), source.imports.filterNot(allowed), "$source may import $what only")
    }

    private fun String.isIn(vararg packages: String) = packages.any { startsWith("$it.") }

    /** The boomerang packages a text names in full, such as "boomerang.net" in boomerang.net.IConnection. */
    private fun namedPackages(text: String): Set<String> =
        Regex("\\bboomerang\\.([a-z]+)\\.").findAll(text).map { it.groupValues[1] }.toSet()

    @Test
    fun `the main sources are where the architecture puts them`() {
        assertEquals(listOf("App.kt"), inFolder("").map { it.file.name }, "files directly in boomerang")
        for (folder in fixedFolders - "") assertTrue(inFolder(folder).isNotEmpty(), "no source file in $folder: the rules below would hold of nothing")
        assertTrue(editionSources.isNotEmpty(), "no edition package")
        for (source in sources) {
            val expected = if (source.folder.isEmpty()) "package boomerang" else "package boomerang.${source.folder.replace(File.separatorChar, '.')}"
            assertTrue(source.text.lines().any { it.trim() == expected }, "$source does not declare '$expected'")
        }
    }

    @Test
    fun `rule 1 - game imports java_io_Serializable and kotlin names only, and names no input, output or thread`() {
        for (source in inFolder("game")) {
            assertImportsOnly(source, "java.io.Serializable and kotlin.*") { it == "java.io.Serializable" || it.isIn("kotlin") }
            for (forbidden in listOf("java.", "javax.", "System.", "Thread", "println", "readLine")) {
                assertTrue(forbidden !in source.code, "$source contains '$forbidden' outside its import lines")
            }
            assertEquals(emptySet<String>(), namedPackages(source.code) - "game", "$source names another boomerang package")
        }
    }

    @Test
    fun `rule 2 - scoring imports from game and kotlin only, and names no edition`() {
        // The names an edition gives its sites, regions and symbols. A scoring class gets them through its constructor.
        // (The message of a TODO in a file that is still a stub is not such a name.)
        val editionNames = editionSources.flatMap { source ->
            val code = source.code.replace(Regex("TODO\\(\"[^\"]*\"\\)"), "")
            Regex("\"([^\"\\\\]{3,})\"").findAll(code).map { it.groupValues[1] }
        }.toSet()

        for (source in inFolder("scoring")) {
            assertImportsOnly(source, "boomerang.game.* and kotlin.*") { it.isIn("boomerang.game", "kotlin") }
            assertEquals(emptySet<String>(), namedPackages(source.code) - setOf("game", "scoring"), "$source names another boomerang package")
            assertEquals(emptyList<String>(), editionNames.filter { it in source.code }, "$source contains names of an edition")
            assertTrue("Symbol(" !in source.code, "$source makes a symbol of its own")
        }
    }

    @Test
    fun `rule 3 - an edition imports from game, scoring and kotlin only, never from another edition`() {
        for (source in editionSources) {
            assertImportsOnly(source, "boomerang.game.*, boomerang.scoring.* and kotlin.*") { it.isIn("boomerang.game", "boomerang.scoring", "kotlin") }
            assertEquals(emptySet<String>(), namedPackages(source.code) - setOf("game", "scoring", source.folder),
                "$source names another boomerang package")
        }
    }

    @Test
    fun `rule 3 - Symbol is constructed only in an edition package, once per symbol, as a named constant`() {
        val construction = Regex("(?<![A-Za-z0-9_])Symbol\\s*\\(")
        val declaration = Regex("\\bclass\\s+Symbol\\s*\\(")
        val constant = Regex("\\bval\\s+[A-Z][A-Z0-9_]*\\s*(?::\\s*Symbol\\s*)?=\\s*Symbol\\s*\\(\\s*\"([^\"]*)\"\\s*,\\s*\"([^\"]*)\"\\s*\\)")

        for (source in sources - editionSources.toSet()) {
            val calls = construction.findAll(source.code).count() - declaration.findAll(source.code).count()
            assertEquals(0, calls, "$source constructs a Symbol outside an edition package")
        }
        for (source in editionSources) {
            val constants = constant.findAll(source.code).map { it.groupValues[1] to it.groupValues[2] }.toList()
            assertEquals(construction.findAll(source.code).count(), constants.size,
                "$source: every Symbol(...) is to be the value of a constant such as val KOALAS = Symbol(\"Animal\", \"Koalas\")")
            assertEquals(constants.toSet().size, constants.size, "$source declares a symbol twice: $constants")
        }
    }

    @Test
    fun `rule 4 - players imports from game and kotlin only, net from game, java_io and java_net only`() {
        for (source in inFolder("players")) {
            assertImportsOnly(source, "boomerang.game.* and kotlin.*") { it.isIn("boomerang.game", "kotlin") }
            assertEquals(emptySet<String>(), namedPackages(source.code) - setOf("game", "players"), "$source names another boomerang package")
            assertTrue("java." !in source.code && "javax." !in source.code, "$source names a java package in full")
        }
        for (source in inFolder("net")) {
            assertImportsOnly(source, "boomerang.game.*, java.io.* and java.net.*") { it.isIn("boomerang.game", "java.io", "java.net") }
            assertEquals(emptySet<String>(), namedPackages(source.code) - setOf("game", "net"), "$source names another boomerang package")
            val javaPackages = Regex("\\bjavax?\\.[a-z]+").findAll(source.code).map { it.value }.toSet()
            assertEquals(emptySet<String>(), javaPackages - setOf("java.io", "java.net"), "$source names a java package other than java.io and java.net")
        }
    }

    @Test
    fun `rule 5 - nothing imports from App_kt, the only file that knows more than one outer package`() {
        val topLevel = Regex("boomerang\\.[A-Za-z_]\\w*|boomerang\\.\\*") // boomerang.parseArguments, boomerang.PORT, boomerang.*
        for (source in sources) {
            assertEquals(emptyList<String>(), source.imports.filter { topLevel.matches(it) && it.substringAfter("boomerang.") !in fixedFolders },
                "$source imports from the start-up file")
        }
        // every other file stays inside its own package and game (and scoring, for an edition): rules 1 to 4
        for (source in sources.filter { it.folder.isNotEmpty() }) {
            val outer = namedPackages(source.text) - setOf("game", "scoring")
            assertTrue(outer.size <= 1, "$source names more than one outer package: $outer")
        }
    }

    @Test
    fun `rule 6 - Random is constructed only in App_kt, and nothing shuffles or draws without a source passed in`() {
        for (source in sources) {
            if (source.folder.isNotEmpty()) {
                assertTrue(!Regex("(?<![A-Za-z0-9_])Random\\s*\\(").containsMatchIn(source.code), "$source constructs a Random")
            }
            assertTrue(!Regex("\\bshuffled\\s*\\(\\s*\\)").containsMatchIn(source.code), "$source calls shuffled() without a source of randomness")
            assertTrue(!Regex("\\brandom\\s*\\(\\s*\\)").containsMatchIn(source.code), "$source calls random() without a source of randomness")
        }
    }
}
