package com.aionemu.commons.database;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.apache.log4j.Logger;
import com.aionemu.commons.configuration.ConfigurableProcessor;
import com.aionemu.commons.configuration.Property;
import com.aionemu.commons.utils.PropertiesUtils;

public class DatabaseConfig {

    private static final Logger log = Logger.getLogger(DatabaseConfig.class);
    public static final String CONFIG_FILE = "./common/dao/config/database.properties"; // 定位配置文件

    @Property(key = "database.url", defaultValue = "")
    public static String DATABASE_URL;
    @Property(key = "database.driver", defaultValue = "com.mysql.jdbc.Driver")
    public static Class<?> DATABASE_DRIVER;
    @Property(key = "database.user", defaultValue = "root")
    public static String DATABASE_USER;
    @Property(key = "database.password", defaultValue = "")
    public static String DATABASE_PASSWORD;
    @Property(key = "database.connections.min", defaultValue = "2")
    public static int DATABASE_CONNECTIONS_MIN;
    @Property(key = "database.connections.max", defaultValue = "10")
    public static int DATABASE_CONNECTIONS_MAX;
    @Property(key = "database.scriptcontext.descriptor", defaultValue = "")
    public static File DATABASE_SCRIPTCONTEXT_DESCRIPTOR;

    /**
     * 加载配置文件（通过反射）
     */
    public static void load() {
        Properties p;
        try {
            p = PropertiesUtils.load(CONFIG_FILE);
        } catch (IOException e) {
            log.fatal("Can't load database configuration...");
            throw new Error("Can't load " + CONFIG_FILE, e);
        }
        ConfigurableProcessor.process(DatabaseConfig.class, p);
    }
}