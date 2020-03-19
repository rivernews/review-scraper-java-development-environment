package com.shaungc.utilities;

public enum ExternalServiceMode {
    SERVER_FROM_MACOS_DOCKER_CONTAINER("serverFromMacosDockerContainer"),
    SERVER_FROM_PORT_FORWARD("serverFromPortForward"),
    SERVER_FROM_CUSTOM_HOST("serverFromCustomHost"),
    LOCAL_INSTALLED("localInstalled");

    private final String string;

    private ExternalServiceMode(String string) {
        this.string = string;
    }

    public String getString() {
        return this.string;
    }
}
