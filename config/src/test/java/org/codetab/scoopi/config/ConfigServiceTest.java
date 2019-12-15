package org.codetab.scoopi.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.text.ParseException;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.codetab.scoopi.config.ConfigService.ConfigIndex;
import org.codetab.scoopi.exception.ConfigNotFoundException;
import org.codetab.scoopi.exception.CriticalException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ConfigServiceTest {

    private static final int DEFAULT_CONFIGS_COUNT = 38;

    private static final int USER_CONFIGS_COUNT = 2;

    @Mock
    private CompositeConfiguration configs;

    @InjectMocks
    private ConfigService configService;

    @Rule
    public ExpectedException testRule = ExpectedException.none();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConfigIndex() {
        // for test coverage of enum, we need to run both values and valueOf
        assertThat(ConfigIndex.SYSTEM).isEqualTo(ConfigIndex.values()[0]);
        assertThat(ConfigIndex.PROVIDED).isEqualTo(ConfigIndex.values()[1]);
        assertThat(ConfigIndex.DEFAULTS).isEqualTo(ConfigIndex.values()[2]);
        assertThat(ConfigIndex.SYSTEM).isEqualTo(ConfigIndex.valueOf("SYSTEM"));
        assertThat(ConfigIndex.PROVIDED)
                .isEqualTo(ConfigIndex.valueOf("PROVIDED"));
        assertThat(ConfigIndex.DEFAULTS)
                .isEqualTo(ConfigIndex.valueOf("DEFAULTS"));
    }

    // TODO enable after moving DI as module
    // @Test
    // public void testSingleton() {
    // DInjector dInjector = new DInjector().instance(DInjector.class);
    //
    // ConfigService instanceA = dInjector.instance(ConfigService.class);
    // ConfigService instanceB = dInjector.instance(ConfigService.class);
    //
    // assertThat(instanceA).isNotNull();
    // assertThat(instanceA).isSameAs(instanceB);
    // }

    @Test
    public void testConfigsInvalidFiles() {
        testRule.expect(CriticalException.class);
        configService.init("xyz", "xyz");
    }

    @Test
    public void testConfigsInvalidUserProvidedFile() {
        configService.init("xyz", "scoopi-default.xml");
        Configuration defaults =
                configService.getConfiguration(ConfigIndex.DEFAULTS);
        assertThat(defaults.size()).isEqualTo(DEFAULT_CONFIGS_COUNT);

        Configuration userProvided =
                configService.getConfiguration(ConfigIndex.PROVIDED);
        assertThat(userProvided.size()).isEqualTo(0);
    }

    @Test
    public void testAddRunDateAndTimeAlreadySetAsSystemProperty()
            throws ConfigNotFoundException, ParseException {
        String runDate = "1980-01-01";
        String runDateTime = "1980-01-01 03:02:01";
        System.setProperty("scoopi.runDate", runDate);
        System.setProperty("scoopi.runDateTime", runDateTime);

        configService.init("xyz", "scoopi-default.xml");

        String actual = configService.getConfig("scoopi.runDate");
        assertThat(actual).isEqualTo(runDate);

        actual = configService.getConfig("scoopi.runDateTime");
        assertThat(actual).isEqualTo(runDateTime);

        System.clearProperty("scoopi.runDateTime");
        System.clearProperty("scoopi.runDate");
    }

    // TODO test should fail when new configs are not tested
    @Test
    public void testDefaultConfigs() throws Exception {
        String userProvidedFile = "xyz";
        String defaultsFile = "scoopi-default.xml";
        configService.init(userProvidedFile, defaultsFile);

        Configuration defaultConfigs =
                configService.getConfiguration(ConfigIndex.DEFAULTS);

        assertThat(defaultConfigs.size()).isEqualTo(DEFAULT_CONFIGS_COUNT);

        assertThat(defaultConfigs.getString("scoopi.propertyPattern"))
                .isEqualTo("scoopi/properties/property");

        assertThat(defaultConfigs.getString("scoopi.defs.dir"))
                .isEqualTo("/defs/examples/fin/jsoup/quickstart");
        assertThat(defaultConfigs.getString("scoopi.defs.defaultStepsFile"))
                .isEqualTo("/steps-default.yml");
        assertThat(defaultConfigs.getString("scoopi.defs.defaultSteps"))
                .isEqualTo("jsoupDefault");
        assertThat(defaultConfigs.getString("scoopi.defs.definedSchema"))
                .isEqualTo("/schema/defs-defined.json");
        assertThat(defaultConfigs.getString("scoopi.defs.effectiveSchema"))
                .isEqualTo("/schema/defs-effective.json");
        assertThat(defaultConfigs.getString("scoopi.seederClass"))
                .isEqualTo("org.codetab.scoopi.step.extract.LocatorSeeder");

        assertThat(defaultConfigs.getString("scoopi.fact.replaceBlank"))
                .isEqualTo("true");
        assertThat(defaultConfigs.getString("scoopi.fact.replaceWith"))
                .isEqualTo("-");

        assertThat(defaultConfigs.getString("scoopi.poolsize.start"))
                .isEqualTo("4");
        assertThat(defaultConfigs.getString("scoopi.poolsize.seeder"))
                .isEqualTo("6");
        assertThat(defaultConfigs.getString("scoopi.poolsize.loader"))
                .isEqualTo("4");
        assertThat(defaultConfigs.getString("scoopi.poolsize.parser"))
                .isEqualTo("4");
        assertThat(defaultConfigs.getString("scoopi.poolsize.process"))
                .isEqualTo("4");
        assertThat(defaultConfigs.getString("scoopi.poolsize.filter"))
                .isEqualTo("4");
        assertThat(defaultConfigs.getString("scoopi.poolsize.appender"))
                .isEqualTo("2");
        assertThat(defaultConfigs.getString("scoopi.appender.queuesize"))
                .isEqualTo("4096");

        assertThat(defaultConfigs.getString("scoopi.webClient.timeout"))
                .isEqualTo("120000");
        assertThat(defaultConfigs.getString("scoopi.webClient.userAgent"))
                .isEqualTo(
                        "Mozilla/5.0 (X11; Linux x86_64; rv:50.0) Gecko/20100101 Firefox/50.0");

        assertThat(defaultConfigs.getString("scoopi.webDriver.driverPath"))
                .isEqualTo(".gecko/geckodriver");
        assertThat(defaultConfigs.getString("scoopi.webDriver.log"))
                .isEqualTo("geckodriver.log");
        assertThat(defaultConfigs.getString("scoopi.webDriver.waitType"))
                .isEqualTo("explicit");
        assertThat(defaultConfigs
                .getString("scoopi.webDriver.timeout.explicitWait"))
                        .isEqualTo("10");
        assertThat(defaultConfigs
                .getString("scoopi.webDriver.timeout.implicitWait"))
                        .isEqualTo("10");

        assertThat(defaultConfigs.getString("scoopi.highDate"))
                .isEqualTo("31-12-2037 23:59:59.999");

        assertThat(defaultConfigs.getString("scoopi.wait")).isEqualTo("false");
        assertThat(defaultConfigs.getString("scoopi.fork.locator"))
                .isEqualTo("0");

        assertThat(defaultConfigs.getString("scoopi.useDatastore"))
                .isEqualTo("true");
        assertThat(defaultConfigs.getString("scoopi.datastore.name"))
                .isEqualTo("datastore");
        assertThat(defaultConfigs.getString("scoopi.datastore.orm"))
                .isEqualTo("jdo");
        assertThat(defaultConfigs.getString("scoopi.datastore.configFile"))
                .isEqualTo("jdoconfig.properties");
        assertThat(defaultConfigs.getString("scoopi.persist.dataDef"))
                .isEqualTo("true");
        assertThat(defaultConfigs.getString("scoopi.persist.locator"))
                .isEqualTo("true");
        assertThat(defaultConfigs.getString("scoopi.persist.data"))
                .isEqualTo("true");

        assertThat(defaultConfigs.getString("scoopi.metrics.server.enable"))
                .isEqualTo("true");
        assertThat(defaultConfigs.getString("scoopi.metrics.server.port"))
                .isEqualTo("9010");

        String[] dateTimePatterns =
                {"dd-MM-yyyy HH:mm:ss.SSS", "dd/MM/yyyy HH:mm:ss.SSS"};
        assertArrayEquals(
                defaultConfigs.getStringArray("scoopi.dateTimeParsePattern"),
                dateTimePatterns);
        String[] datePatterns = {"dd-MM-yyyy", "dd/MM/yyyy"};
        assertArrayEquals(
                defaultConfigs.getStringArray("scoopi.dateParsePattern"),
                datePatterns);
    }

    /*
     * test stock scoopi.properties that is distributed
     */
    @Test
    public void testUserProvidedConfigs() {
        configService.init("scoopi.properties", "scoopi-default.xml");

        Configuration userConfigs =
                configService.getConfiguration(ConfigIndex.PROVIDED);

        assertThat(userConfigs.size()).isEqualTo(USER_CONFIGS_COUNT);

        assertThat(userConfigs.getString("scoopi.defs.dir"))
                .isEqualTo("/defs/examples/fin/jsoup/quickstart");
        assertThat(userConfigs.getString("scoopi.useDatastore"))
                .isEqualTo("false");
    }

    @Test
    public void testGetConfigs() {
        assertThat(configService.getConfigs()).isEqualTo(configs);
    }

    @Test
    public void testGetRunnerClass() {
        String eclipseTestRunner =
                "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner"; //$NON-NLS-1$
        String mavenTestRunner =
                "org.apache.maven.surefire.booter.ForkedBooter"; //$NON-NLS-1$

        assertThat(configService.getRunnerClass()).isIn(eclipseTestRunner,
                mavenTestRunner);
    }

    @Test
    public void testGetConfig() throws ConfigNotFoundException {
        String key = "key";
        String value = "value";
        given(configs.getString(key)).willReturn(value);

        String actual = configService.getConfig(key);

        assertThat(actual).isEqualTo(value);
    }

    @Test
    public void testGetConfigConfigNotFoundException()
            throws ConfigNotFoundException {
        String key = "key";
        String value = null;
        given(configs.getString(key)).willReturn(value);

        testRule.expect(ConfigNotFoundException.class);
        configService.getConfig(key);
    }

    @Test
    public void testGetConfigArray() throws ConfigNotFoundException {
        String key = "key";
        String[] value = {"value", "value2"};
        given(configs.getStringArray(key)).willReturn(value);

        String[] actual = configService.getConfigArray(key);

        assertThat(actual).isEqualTo(value);
    }

    @Test
    public void testGetConfigArrayConfigNotFoundException()
            throws ConfigNotFoundException {
        String key = "key";
        String[] value = {};
        given(configs.getStringArray(key)).willReturn(value);

        testRule.expect(ConfigNotFoundException.class);
        configService.getConfigArray(key);
    }

    @Test
    public void testGetString() {
        String key = "key";
        String value = "value";
        given(configs.getString(key)).willReturn(value);

        String actual = configService.getString(key);

        assertThat(actual).isEqualTo(value);
    }

    @Test
    public void testGetStringNull() {
        String key = "key";
        String value = null;
        given(configs.getString(key)).willReturn(value);

        String actual = configService.getString(key);

        assertThat(actual).isNull();
    }

    @Test
    public void testGetBoolean() {
        String key = "key";
        boolean defaultValue = true;
        given(configs.getBoolean(key, defaultValue)).willReturn(true, false);

        assertThat(configService.getBoolean(key, defaultValue)).isTrue();
        assertThat(configService.getBoolean(key, defaultValue)).isFalse();
    }

    @Test
    public void testGetProperty() {
        String key = "key";
        String value = "value";
        given(configs.getProperty(key)).willReturn(value);

        Object actual = configService.getProperty(key);

        assertThat(actual).isEqualTo(value);
    }

    @Test
    public void testGetPropertyNull() {
        String key = "key";
        String value = null;
        given(configs.getProperty(key)).willReturn(value);

        Object actual = configService.getProperty(key);

        assertThat(actual).isNull();
    }

    @Test
    public void testSetProperty() {
        String key = "key";
        String value = "value";

        configService.setProperty(key, value);

        verify(configs).setProperty(key, value);
    }

    @Test
    public void testAddProperty() {
        String key = "key";
        String value = "value";

        configService.addProperty(key, value);

        verify(configs).addProperty(key, value);
    }
}
