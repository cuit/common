package com.xsc.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author xia
 * @date 2019/7/30 18:00
 */
public class ConfigUtils {

    private ConfigUtils() {
    }

    private static Properties properties;

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigUtils.class);

    static {
        loadProperties();
    }

    private static synchronized void loadProperties() {
        properties = new Properties();
        InputStream inputStream = null;
        try {
            inputStream = ConfigUtils.class.getResourceAsStream("/default.properties");
            properties.load(inputStream);
        } catch (IOException e) {
            LOGGER.error("Configuration file[default.properties] loading error! {}， {}", e.getMessage(), e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    LOGGER.error("file stream close error! {}， {}", e.getMessage(), e);
                }
            }
        }
    }

    public static String getPropertyByKey(String key) {
        if (StringUtils.isNotBlank(key)) {
            return properties.getProperty(key);
        }
        return StringUtils.EMPTY;
    }

}
