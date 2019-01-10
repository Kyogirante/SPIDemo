package com.spi.loader;

import java.util.Set;

public class ServiceRegistry {
    private ServiceRegistry() {
    }

    public static synchronized void register(final Class<?> serviceClass, final Class<?> providerClass) {
        throw new RuntimeException("Stub!");
    }

    public static synchronized Set<Class<?>> get(final Class<?> clazz) {
        throw new RuntimeException("Stub!");
    }
}
