package de.ids_mannheim.korap.plkexport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for environment variable overrides in ExWSConf.
 * Tests that environment variables can override configuration file values
 * and have higher priority than config files.
 */
public class ExWSConfEnvTest {

    private Map<String, String> mockEnv;

    @Before
    public void setUp() {
        // Create a mock environment map for each test
        mockEnv = new HashMap<>();
        
        // Clear any previously loaded properties
        ExWSConf.clearProp();
        
        // Reset to default environment provider
        ExWSConf.setEnvironmentProvider(null);
    }

    @After
    public void tearDown() {
        // Reset state after each test
        ExWSConf.clearProp();
        ExWSConf.setEnvironmentProvider(null);
    }

    /**
     * Test that the environment-to-property mapping contains all expected entries
     */
    @Test
    public void testEnvMappingContainsAllExpectedVariables() {
        Map<String, String> mapping = ExWSConf.getEnvToPropertyMapping();
        
        // Server configuration
        assertEquals("server.port", mapping.get("KALAMAR_EXPORT_SERVER_PORT"));
        assertEquals("server.host", mapping.get("KALAMAR_EXPORT_SERVER_HOST"));
        assertEquals("server.scheme", mapping.get("KALAMAR_EXPORT_SERVER_SCHEME"));
        assertEquals("server.origin", mapping.get("KALAMAR_EXPORT_SERVER_ORIGIN"));
        
        // API configuration
        assertEquals("api.port", mapping.get("KALAMAR_EXPORT_API_PORT"));
        assertEquals("api.host", mapping.get("KALAMAR_EXPORT_API_HOST"));
        assertEquals("api.scheme", mapping.get("KALAMAR_EXPORT_API_SCHEME"));
        assertEquals("api.source", mapping.get("KALAMAR_EXPORT_API_SOURCE"));
        assertEquals("api.path", mapping.get("KALAMAR_EXPORT_API_PATH"));
        
        // Asset configuration
        assertEquals("asset.host", mapping.get("KALAMAR_EXPORT_ASSET_HOST"));
        assertEquals("asset.port", mapping.get("KALAMAR_EXPORT_ASSET_PORT"));
        assertEquals("asset.scheme", mapping.get("KALAMAR_EXPORT_ASSET_SCHEME"));
        assertEquals("asset.path", mapping.get("KALAMAR_EXPORT_ASSET_PATH"));
        
        // General configuration
        assertEquals("conf.page_size", mapping.get("KALAMAR_EXPORT_PAGE_SIZE"));
        assertEquals("conf.max_exp_limit", mapping.get("KALAMAR_EXPORT_MAX_EXP_LIMIT"));
        assertEquals("conf.file_dir", mapping.get("KALAMAR_EXPORT_FILE_DIR"));
        assertEquals("conf.default_hitc", mapping.get("KALAMAR_EXPORT_DEFAULT_HITC"));
        
        // Cookie configuration
        assertEquals("cookie.name", mapping.get("KALAMAR_EXPORT_COOKIE_NAME"));
    }

    /**
     * Test that KALAMAR_EXPORT_SERVER_PORT environment variable overrides server.port
     */
    @Test
    public void testServerPortEnvOverride() {
        mockEnv.put("KALAMAR_EXPORT_SERVER_PORT", "9999");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties props = ExWSConf.properties(null);
        assertEquals("9999", props.getProperty("server.port"));
    }

    /**
     * Test that KALAMAR_EXPORT_SERVER_HOST environment variable overrides server.host
     */
    @Test
    public void testServerHostEnvOverride() {
        mockEnv.put("KALAMAR_EXPORT_SERVER_HOST", "custom.host.example.com");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties props = ExWSConf.properties(null);
        assertEquals("custom.host.example.com", props.getProperty("server.host"));
    }

    /**
     * Test that KALAMAR_EXPORT_SERVER_SCHEME environment variable overrides server.scheme
     */
    @Test
    public void testServerSchemeEnvOverride() {
        mockEnv.put("KALAMAR_EXPORT_SERVER_SCHEME", "http");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties props = ExWSConf.properties(null);
        assertEquals("http", props.getProperty("server.scheme"));
    }

    /**
     * Test that KALAMAR_EXPORT_API_PORT environment variable overrides api.port
     */
    @Test
    public void testApiPortEnvOverride() {
        mockEnv.put("KALAMAR_EXPORT_API_PORT", "8080");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties props = ExWSConf.properties(null);
        assertEquals("8080", props.getProperty("api.port"));
    }

    /**
     * Test that KALAMAR_EXPORT_API_HOST environment variable overrides api.host
     */
    @Test
    public void testApiHostEnvOverride() {
        mockEnv.put("KALAMAR_EXPORT_API_HOST", "api.custom.example.com");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties props = ExWSConf.properties(null);
        assertEquals("api.custom.example.com", props.getProperty("api.host"));
    }

    /**
     * Test that KALAMAR_EXPORT_API_SCHEME environment variable overrides api.scheme
     */
    @Test
    public void testApiSchemeEnvOverride() {
        mockEnv.put("KALAMAR_EXPORT_API_SCHEME", "http");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties props = ExWSConf.properties(null);
        assertEquals("http", props.getProperty("api.scheme"));
    }

    /**
     * Test that KALAMAR_EXPORT_API_SOURCE environment variable sets api.source
     */
    @Test
    public void testApiSourceEnvOverride() {
        mockEnv.put("KALAMAR_EXPORT_API_SOURCE", "custom-source.example.com");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties props = ExWSConf.properties(null);
        assertEquals("custom-source.example.com", props.getProperty("api.source"));
    }

    /**
     * Test that KALAMAR_EXPORT_API_PATH environment variable sets api.path
     */
    @Test
    public void testApiPathEnvOverride() {
        mockEnv.put("KALAMAR_EXPORT_API_PATH", "/custom/api/path");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties props = ExWSConf.properties(null);
        assertEquals("/custom/api/path", props.getProperty("api.path"));
    }

    /**
     * Test that KALAMAR_EXPORT_ASSET_HOST environment variable overrides asset.host
     */
    @Test
    public void testAssetHostEnvOverride() {
        mockEnv.put("KALAMAR_EXPORT_ASSET_HOST", "assets.custom.example.com");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties props = ExWSConf.properties(null);
        assertEquals("assets.custom.example.com", props.getProperty("asset.host"));
    }

    /**
     * Test that KALAMAR_EXPORT_ASSET_PORT environment variable sets asset.port
     */
    @Test
    public void testAssetPortEnvOverride() {
        mockEnv.put("KALAMAR_EXPORT_ASSET_PORT", "8888");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties props = ExWSConf.properties(null);
        assertEquals("8888", props.getProperty("asset.port"));
    }

    /**
     * Test that KALAMAR_EXPORT_ASSET_SCHEME environment variable overrides asset.scheme
     */
    @Test
    public void testAssetSchemeEnvOverride() {
        mockEnv.put("KALAMAR_EXPORT_ASSET_SCHEME", "http");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties props = ExWSConf.properties(null);
        assertEquals("http", props.getProperty("asset.scheme"));
    }

    /**
     * Test that KALAMAR_EXPORT_ASSET_PATH environment variable sets asset.path
     */
    @Test
    public void testAssetPathEnvOverride() {
        mockEnv.put("KALAMAR_EXPORT_ASSET_PATH", "/custom/asset/path");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties props = ExWSConf.properties(null);
        assertEquals("/custom/asset/path", props.getProperty("asset.path"));
    }

    /**
     * Test that KALAMAR_EXPORT_PAGE_SIZE environment variable overrides conf.page_size
     */
    @Test
    public void testPageSizeEnvOverride() {
        mockEnv.put("KALAMAR_EXPORT_PAGE_SIZE", "25");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties props = ExWSConf.properties(null);
        assertEquals("25", props.getProperty("conf.page_size"));
    }

    /**
     * Test that KALAMAR_EXPORT_MAX_EXP_LIMIT environment variable overrides conf.max_exp_limit
     */
    @Test
    public void testMaxExpLimitEnvOverride() {
        mockEnv.put("KALAMAR_EXPORT_MAX_EXP_LIMIT", "50000");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties props = ExWSConf.properties(null);
        assertEquals("50000", props.getProperty("conf.max_exp_limit"));
    }

    /**
     * Test that KALAMAR_EXPORT_FILE_DIR environment variable sets conf.file_dir
     */
    @Test
    public void testFileDirEnvOverride() {
        mockEnv.put("KALAMAR_EXPORT_FILE_DIR", "/custom/export/dir");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties props = ExWSConf.properties(null);
        assertEquals("/custom/export/dir", props.getProperty("conf.file_dir"));
    }

    /**
     * Test that KALAMAR_EXPORT_DEFAULT_HITC environment variable overrides conf.default_hitc
     */
    @Test
    public void testDefaultHitcEnvOverride() {
        mockEnv.put("KALAMAR_EXPORT_DEFAULT_HITC", "500");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties props = ExWSConf.properties(null);
        assertEquals("500", props.getProperty("conf.default_hitc"));
    }

    /**
     * Test that KALAMAR_EXPORT_SERVER_ORIGIN environment variable sets server.origin
     */
    @Test
    public void testServerOriginEnvOverride() {
        mockEnv.put("KALAMAR_EXPORT_SERVER_ORIGIN", "https://custom.origin.example.com");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties props = ExWSConf.properties(null);
        assertEquals("https://custom.origin.example.com", props.getProperty("server.origin"));
    }

    /**
     * Test that KALAMAR_EXPORT_COOKIE_NAME environment variable sets cookie.name
     */
    @Test
    public void testCookieNameEnvOverride() {
        mockEnv.put("KALAMAR_EXPORT_COOKIE_NAME", "custom_cookie_name");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties props = ExWSConf.properties(null);
        assertEquals("custom_cookie_name", props.getProperty("cookie.name"));
    }

    /**
     * Test that environment variables override values from configuration files
     */
    @Test
    public void testEnvOverridesConfigFile() {
        // First, get default config value
        ExWSConf.clearProp();
        ExWSConf.setEnvironmentProvider(name -> null);  // No env vars
        Properties propsWithoutEnv = ExWSConf.properties(null);
        String defaultPort = propsWithoutEnv.getProperty("server.port");
        
        // Now set environment variable to different value
        ExWSConf.clearProp();
        mockEnv.put("KALAMAR_EXPORT_SERVER_PORT", "12345");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties propsWithEnv = ExWSConf.properties(null);
        String envPort = propsWithEnv.getProperty("server.port");
        
        assertEquals("12345", envPort);
        assertNotEquals(defaultPort, envPort);
    }

    /**
     * Test that environment variables also override values from additional config file
     */
    @Test
    public void testEnvOverridesSecondaryConfigFile() {
        // Set environment variable
        mockEnv.put("KALAMAR_EXPORT_PAGE_SIZE", "999");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        // Load with secondary config file that also sets page_size
        Properties props = ExWSConf.properties("exportPluginSec.conf");
        
        // Environment should win
        assertEquals("999", props.getProperty("conf.page_size"));
    }

    /**
     * Test that empty environment variables do not override config values
     */
    @Test
    public void testEmptyEnvDoesNotOverride() {
        // Set empty environment variable
        mockEnv.put("KALAMAR_EXPORT_SERVER_PORT", "");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties props = ExWSConf.properties(null);
        
        // Should use config file value, not empty string
        assertNotEquals("", props.getProperty("server.port"));
    }

    /**
     * Test that null environment variables do not override config values
     */
    @Test
    public void testNullEnvDoesNotOverride() {
        // Set up provider that returns null for all variables
        ExWSConf.setEnvironmentProvider(name -> null);
        
        Properties props = ExWSConf.properties(null);
        
        // Should use config file values
        assertEquals("1024", props.getProperty("server.port"));
        assertEquals("localhost", props.getProperty("server.host"));
    }

    /**
     * Test multiple environment variables being set at once
     */
    @Test
    public void testMultipleEnvOverrides() {
        mockEnv.put("KALAMAR_EXPORT_SERVER_PORT", "7777");
        mockEnv.put("KALAMAR_EXPORT_SERVER_HOST", "multi.test.com");
        mockEnv.put("KALAMAR_EXPORT_API_PORT", "8888");
        mockEnv.put("KALAMAR_EXPORT_PAGE_SIZE", "50");
        mockEnv.put("KALAMAR_EXPORT_MAX_EXP_LIMIT", "100000");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties props = ExWSConf.properties(null);
        
        assertEquals("7777", props.getProperty("server.port"));
        assertEquals("multi.test.com", props.getProperty("server.host"));
        assertEquals("8888", props.getProperty("api.port"));
        assertEquals("50", props.getProperty("conf.page_size"));
        assertEquals("100000", props.getProperty("conf.max_exp_limit"));
    }

    /**
     * Test that properties not set via environment use config file values
     */
    @Test
    public void testNonOverriddenPropertiesRetainConfigValues() {
        // Only override one property
        mockEnv.put("KALAMAR_EXPORT_SERVER_PORT", "9876");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties props = ExWSConf.properties(null);
        
        // Overridden property
        assertEquals("9876", props.getProperty("server.port"));
        
        // Non-overridden properties should retain config file values
        assertEquals("localhost", props.getProperty("server.host"));
        assertEquals("https", props.getProperty("server.scheme"));
    }

    /**
     * Test that environment variables work with numeric values as strings
     */
    @Test
    public void testNumericEnvValues() {
        mockEnv.put("KALAMAR_EXPORT_SERVER_PORT", "65535");
        mockEnv.put("KALAMAR_EXPORT_PAGE_SIZE", "100");
        mockEnv.put("KALAMAR_EXPORT_MAX_EXP_LIMIT", "1000000");
        mockEnv.put("KALAMAR_EXPORT_DEFAULT_HITC", "250");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties props = ExWSConf.properties(null);
        
        // Verify these can be parsed as integers
        assertEquals(65535, Integer.parseInt(props.getProperty("server.port")));
        assertEquals(100, Integer.parseInt(props.getProperty("conf.page_size")));
        assertEquals(1000000, Integer.parseInt(props.getProperty("conf.max_exp_limit")));
        assertEquals(250, Integer.parseInt(props.getProperty("conf.default_hitc")));
    }

    /**
     * Test resetting environment provider to default
     */
    @Test
    public void testResetEnvironmentProvider() {
        // Set mock provider
        mockEnv.put("KALAMAR_EXPORT_SERVER_PORT", "1111");
        ExWSConf.setEnvironmentProvider(mockEnv::get);
        
        Properties propsWithMock = ExWSConf.properties(null);
        assertEquals("1111", propsWithMock.getProperty("server.port"));
        
        // Reset and reload
        ExWSConf.clearProp();
        ExWSConf.setEnvironmentProvider(null);  // Reset to System.getenv
        
        Properties propsReset = ExWSConf.properties(null);
        // Should use config file value (assuming no actual env var is set)
        assertEquals("1024", propsReset.getProperty("server.port"));
    }
}
