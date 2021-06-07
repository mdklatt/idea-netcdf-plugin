/**
 * Unit tests for the Hello module.
 */
package software.mdklatt.idea.netcdf.configurations

import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.getOrCreate
import org.jdom.Element

// The IDEA platform tests use JUnit3, so test class method names are used to 
// determine behavior instead of annotations. Notably, test classes are *not* 
// constructed before each test, so setUp() methods should be used for 
// per-test initialization where necessary. Also, test functions must be named 
// `testXXX` or they will not be found during automatic discovery.

/**
 * Unit tests for the HelloConfigurationFactory class.
 */
class HelloConfigurationFactoryTest : BasePlatformTestCase() {

    private lateinit var factory: HelloConfigurationFactory

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        factory = HelloConfigurationFactory(HelloConfigurationType())
    }

    /**
     * Test the `id` property.
     */
    fun testId() {
        assertTrue(factory.id.isNotBlank())
    }

    /**
     * Test the `name` property.
     */
    fun testName() {
        assertTrue(factory.name.isNotBlank())
    }
}

/**
 * Unit tests for the HelloRunConfiguration class.
 */
class HelloRunConfigurationTest : BasePlatformTestCase() {

    private lateinit var config: HelloRunConfiguration
    private lateinit var element: Element

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        val factory = HelloConfigurationFactory(HelloConfigurationType())
        config = HelloRunConfiguration(project, factory, "Hello Test")
        config.settings.apply {
            name = "abc"
        }
        element = Element("configuration")
    }

    /**
     * Test the constructor.
     */
    fun testConstructor() {
        assertEquals("Hello Test", config.name)
    }

    /**
     * Test round-trip write/read of settings.
     */
    fun testPersistence() {
        config.writeExternal(element)
        element.getOrCreate(config.settings.xmlTagName).let {
            assertTrue(JDOMExternalizerUtil.readField(it, "id", "").isNotEmpty())
            assertEquals("abc", JDOMExternalizerUtil.readField(it, "name", ""))
        }
        HelloRunConfiguration.Settings().apply {
            load(element)
            assertEquals("abc", name)
        }
    }
}

/**
 * Unit tests for the HelloSettingsEditor class.
 */
class HelloSettingsEditorTest : BasePlatformTestCase() {
    /**
     * Test the constructor.
     */
    fun testConstructor() {
        HelloSettingsEditor().apply {
            assertTrue(name.text.isEmpty())
        }
    }
}
