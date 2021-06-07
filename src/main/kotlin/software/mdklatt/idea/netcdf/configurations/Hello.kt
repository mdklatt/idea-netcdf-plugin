/**
 * Sample implementation of a Run Configuration that calls an external process.
 */
package software.mdklatt.idea.netcdf.configurations

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.ui.layout.panel
import com.intellij.util.getOrCreate
import org.jdom.Element
import java.util.*
import javax.swing.JComponent
import javax.swing.JTextField

class HelloConfigurationType : ConfigurationTypeBase(
    HelloConfigurationType::class.java.simpleName,
    "Hello",
    "Display a greeting",
    AllIcons.General.GearPlain
) {

    init {
        addFactory(HelloConfigurationFactory(this))
    }
}

/**
 * Factory for HelloRunConfiguration instances.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#configuration-factory">Configuration Factory</a>
 */
class HelloConfigurationFactory internal constructor(type: ConfigurationType) : ConfigurationFactory(type) {
    /**
     * Creates a new template run configuration within the context of the specified project.
     *
     * @param project the project in which the run configuration will be used
     * @return the run configuration instance.
     */
    override fun createTemplateConfiguration(project: Project) =
        HelloRunConfiguration(project, this, "")

    /**
     * Run configuration ID used for serialization.
     *
     * @return: unique ID
     */
    override fun getId(): String = this::class.java.simpleName
}

/**
 * Run Configuration for printing "Hello, World!" to the console.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#run-configuration">Run Configuration</a>
 */
class HelloRunConfiguration internal constructor(project: Project, factory: ConfigurationFactory, name: String) :
    RunConfigurationBase<RunProfileState>(project, factory, name) {

    /**
     * HelloRunConfiguration settings.
     *
     * Each configuration instance will have a unique `id` attribute that can be
     * used to persist external data, e.g. PasswordSafe credentials.
     */
    class Settings {

        internal val xmlTagName = "hello"

        private val logger = Logger.getInstance(this::class.java)  // runtime class resolution
        private var id: UUID? = null

        var name = ""
            get() = field.ifEmpty { "World" }

        /**
         * Load persistent settings.
         *
         * @param element: input XML element
         */
        internal fun load(element: Element) {
            element.getOrCreate(xmlTagName).let {
                val str = JDOMExternalizerUtil.readField(it, "id", "")
                id = if (str.isEmpty()) UUID.randomUUID() else UUID.fromString(str)
                logger.debug("loading settings for configuration $id")
                name = JDOMExternalizerUtil.readField(it, "name", "")
            }
            return
        }

        /**
         * Save persistent settings.
         *
         * @param element: output XML element
         */
        internal fun save(element: Element) {
            val default = element.getAttributeValue("default")?.toBoolean() ?: false
            element.getOrCreate(xmlTagName).let {
                if (!default) {
                    id = id ?: UUID.randomUUID()
                    logger.debug("saving settings for configuration $id")
                    JDOMExternalizerUtil.writeField(it, "id", id.toString())
                } else {
                    logger.debug("saving settings for default configuration")
                }
                JDOMExternalizerUtil.writeField(it, "name", name)
            }
            return
        }
    }

    var settings = Settings()

    /**
     * Returns the UI control for editing the run configuration settings. If additional control over validation is required, the object
     * returned from this method may also implement [com.intellij.execution.impl.CheckableRunConfigurationEditor]. The returned object
     * can also implement [com.intellij.openapi.options.SettingsEditorGroup] if the settings it provides need to be displayed in
     * multiple tabs.
     *
     * @return the settings editor component.
     */
    override fun getConfigurationEditor() = HelloSettingsEditor()

    /**
     * Prepares for executing a specific instance of the run configuration.
     *
     * @param executor the execution mode selected by the user (run, debug, profile etc.)
     * @param environment the environment object containing additional settings for executing the configuration.
     * @return the RunProfileState describing the process which is about to be started, or null if it's impossible to start the process.
     */
    override fun getState(executor: Executor, environment: ExecutionEnvironment) =
        HelloCommandLineState(this, environment)

    /**
     * Read settings from an XML element.
     *
     * @param element: input element.
     */
    override fun readExternal(element: Element) {
        super.readExternal(element)
        settings.load(element)
        return
    }

    /**
     * Write settings to an XML element.
     *
     * @param element: output element.
     */
    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        settings.save(element)
        return
    }
}

/**
 * TODO
 */
class HelloCommandLineState internal constructor(private val config: HelloRunConfiguration, environment: ExecutionEnvironment) :
    CommandLineState(environment) {

    /**
     * Start the external process.
     *
     * @return the handler for the running process
     * @throws ExecutionException if the execution failed.
     * @see GeneralCommandLine
     *
     * @see com.intellij.execution.process.OSProcessHandler
     */
    override fun startProcess(): ProcessHandler {
        val settings = config.settings
        val command = GeneralCommandLine("echo", "Hello, ${settings.name}!")
        return KillableColoredProcessHandler(command).also {
            ProcessTerminatedListener.attach(it, environment.project)
        }
    }
}


/**
 * UI component for Hello Run Configuration settings.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#settings-editor">Settings Editor</a>
 */
class HelloSettingsEditor internal constructor() : SettingsEditor<HelloRunConfiguration>() {

    var name = JTextField("")

    /**
     * Create the widget for this editor.
     *
     * @return UI widget
     */
    override fun createEditor(): JComponent {
        // https://www.jetbrains.org/intellij/sdk/docs/user_interface_components/kotlin_ui_dsl.html
        return panel {
            row("Name:") { name() }
        }
    }

    /**
     * Reset editor fields from the configuration state.
     *
     * @param config: run configuration
     */
    override fun resetEditorFrom(config: HelloRunConfiguration) {
        config.let {
            name.text = it.settings.name
        }
        return
    }

    /**
     * Apply editor fields to the configuration state.
     *
     * @param config: run configuration
     */
    override fun applyEditorTo(config: HelloRunConfiguration) {
        // This apparently gets called for every key press, so performance is
        // critical.
        config.let {
            it.settings = HelloRunConfiguration.Settings()
            it.settings.name = name.text
        }
        return
    }
}
