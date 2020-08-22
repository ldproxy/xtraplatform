package de.ii.xtraplatform.runtime.domain;

public class Constants {

    public static final String DATA_DIR_KEY = "de.ii.xtraplatform.directories.data";
    public static final String ENV_KEY = "de.ii.xtraplatform.environment";
    public static final String USER_CONFIG_PATH_KEY = "de.ii.xtraplatform.userConfigPath";

    public enum ENV {
        PRODUCTION,
        DEVELOPMENT,
        CONTAINER
    }
}
