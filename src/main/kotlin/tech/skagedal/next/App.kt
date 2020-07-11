/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package tech.skagedal.next

import java.nio.file.FileSystems

class App(
    val fileSystemLinter: FileSystemLinter
) {
    fun run() {
        fileSystemLinter.run()
    }
}

fun main(args: Array<String>) {
    val fileSystemLinter = FileSystemLinter(
        FileSystems.getDefault(),
        ShellStarter()
    )
    val app = App(fileSystemLinter)

    app.run()
}
