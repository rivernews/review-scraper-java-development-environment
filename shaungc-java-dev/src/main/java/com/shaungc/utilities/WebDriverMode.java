package com.shaungc.utilities;

public enum WebDriverMode {
    SELENIUM_SERVER_FROM_MACOS_DOCKER_CONTAINER("seleniumServerFromMacosDockerContainer"),
    SELENIUM_SERVER_FROM_PORT_FORWARD("seleniumServerFromPortForward"),
    SELENIUM_SERVER_FROM_CUSTOM_HOST("seleniumServerFromCustomHost"),
    LOCAL_INSTALLED_DRIVER("localInstalledDriver");

    private final String string;

    private WebDriverMode(String string) {
        this.string = string;
    }

    public String getString() {
        return this.string;
    }
}
