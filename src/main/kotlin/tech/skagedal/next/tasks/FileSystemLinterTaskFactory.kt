package tech.skagedal.next.tasks

import tech.skagedal.next.ProcessRunner
import tech.skagedal.next.RunnableTask
import tech.skagedal.next.desktop
import tech.skagedal.next.home
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path

class FileSystemLinterTaskFactory(
    val fileSystem: FileSystem,
    val processRunner: ProcessRunner
) {
    fun standardTasks(): List<RunnableTask> = listOf(
        // Rule: We should not have non-hidden, non-directory files laying around in the home directory.

        FileSystemLinterTask(
            fileSystem.home(),
            "in your home directory",
            ::homeRules
        ),

        // Rule: We should not have files laying around on the Desktop.  The .DS_Store file is ok.

        FileSystemLinterTask(
            fileSystem.desktop(),
            "on the Desktop",
            ::desktopRules
        )
    )

    private fun homeRules(path: Path) =
        Files.isRegularFile(path) && !Files.isHidden(path)

    private fun desktopRules(path: Path) =
        Files.isRegularFile(path) && !path.fileName.toString().equals(".DS_Store", true)
}