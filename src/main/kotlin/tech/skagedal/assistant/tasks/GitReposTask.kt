package tech.skagedal.assistant.tasks

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import tech.skagedal.assistant.RunnableTask
import tech.skagedal.assistant.TaskResult
import tech.skagedal.assistant.commands.GitCleanCommand
import tech.skagedal.assistant.git.GitRepo
import tech.skagedal.assistant.isGloballyIgnored
import tech.skagedal.assistant.ui.UserInterface
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

class GitReposTask(val path: Path) : RunnableTask {
    sealed class GitResult {
        object Clean: GitResult()
        object Dirty: GitResult()
        object NotGitRepository: GitResult()
        object NotDirectory: GitResult()
        data class UnmergedBranches(val branches: List<String>): GitResult()
    }

    data class ResultWithPath(
        val path: Path,
        val result: GitResult
    )

    override fun runTask() = fetchAllResults()
        .find { it.result != GitResult.Clean }?.let {
            val result = it.result
            when (result) {
                GitResult.Dirty -> {
                    System.err.println("Dirty git repository")
                    TaskResult.ShellActionRequired(it.path)
                }
                GitResult.NotGitRepository -> {
                    System.err.println("Not a git repository")
                    TaskResult.ShellActionRequired(it.path)
                }
                GitResult.NotDirectory -> {
                    System.err.println("Not a directory: ${it.path}")
                    TaskResult.ShellActionRequired(path)
                }
                GitResult.Clean -> throw IllegalStateException()
                is GitResult.UnmergedBranches -> {
                    System.err.println("Contains unmerged branches: ${it.path}")
                    val gitClean = GitCleanCommand(FileSystems.getDefault(), UserInterface())
                    val gitRepo = GitRepo(it.path)
                    gitClean.handleBranches(gitRepo, result.branches)
                    TaskResult.Proceed
                }
            }
        } ?: TaskResult.Proceed

    private fun fetchAllResults() =
        runBlocking {
            filesInDirectory(path).map {
                async(Dispatchers.IO) {
                    ResultWithPath(it, repoResult(it))
                }
            }.awaitAll()
        }

    private fun repoResult(dir: Path): GitResult {
        if (!Files.isDirectory(dir)) {
            return if (dir.isGloballyIgnored()) GitResult.Clean else GitResult.NotDirectory
        }
        val statusResult = gitStatusResult(dir)
        if (statusResult != GitResult.Clean) {
            return statusResult
        }

        val notMergedBranches = GitRepo(dir).getNotMergedBranches()
        if (notMergedBranches.isNotEmpty()) {
            return GitResult.UnmergedBranches(notMergedBranches)
        }
        return GitResult.Clean
    }

    private fun gitStatusResult(dir: Path): GitResult {
        val process = ProcessBuilder("git", "status", "--porcelain", "-unormal")
            .directory(dir.toFile())
            .start()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            return GitResult.NotGitRepository
        }
        val nonEmptyGitStatusResult = process
            .inputStream
            .readAllBytes()
            .isNotEmpty()
        return if (nonEmptyGitStatusResult) GitResult.Dirty else GitResult.Clean
    }
}

fun filesInDirectory(path: Path): List<Path> = Files.newDirectoryStream(path).use { it.toList() }
