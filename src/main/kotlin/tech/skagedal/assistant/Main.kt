package tech.skagedal.assistant

import com.google.api.client.json.jackson2.JacksonFactory
import tech.skagedal.assistant.commands.Next
import tech.skagedal.assistant.commands.SimonsAssistant
import tech.skagedal.assistant.commands.TrackEdit
import tech.skagedal.assistant.configuration.ConfigurationLoader
import tech.skagedal.assistant.tasks.FileSystemLinterTaskFactory
import tech.skagedal.assistant.tasks.GitReposTaskFactory
import tech.skagedal.assistant.tasks.GmailCheckerTaskFactory
import tech.skagedal.assistant.tasks.IntervalTaskFactory
import tech.skagedal.assistant.tracker.Serializer
import java.nio.file.FileSystems

fun main(args: Array<String>) {
    val processRunner = ProcessRunner()
    val fileSystem = FileSystems.getDefault()
    val repository = Repository(fileSystem)
    val configurationLoader = ConfigurationLoader()

    val fileSystemLinter = FileSystemLinterTaskFactory(
        fileSystem,
        processRunner
    )
    val intervalTaskRunner = IntervalTaskFactory(
        processRunner,
        repository
    )
    val gmailChecker = GmailCheckerTaskFactory(
        fileSystem,
        processRunner,
        JacksonFactory.getDefaultInstance()
    )
    val gitReposTaskFactory = GitReposTaskFactory(
        fileSystem
    )

    val nextCommand = Next(
        fileSystem,
        repository,
        configurationLoader,
        fileSystemLinter,
        intervalTaskRunner,
        gmailChecker,
        gitReposTaskFactory
    )

    val trackEditCommand = TrackEdit(
        tech.skagedal.assistant.tracker.Repository(
            fileSystem,
            Serializer()
        ),
        processRunner
    )

    val simonsAssistant = SimonsAssistant(listOf(nextCommand, trackEditCommand))
    simonsAssistant.main(args)
}