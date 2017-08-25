package com.spi.loader;

import com.spi.registry.ServiceRegistry;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.ServiceConfigurationError;
import java.util.Set;


public final class ServiceLoader<S> implements Iterable<S> {
    public static final <S> ServiceLoader<S> load(final Class<S> serviceClass) {
        return new ServiceLoader<S>(serviceClass);
    }

    private final Class<S> mService;

    private final Set<S> mProviders = new LinkedHashSet<S>();

    private ServiceLoader(final Class<S> service) {
        this.mService = service;
        this.load();
    }

    public S get() {
        final Iterator<S> i = this.mProviders.iterator();
        if (i.hasNext()) {
            return i.next();
        }

        return null;
    }

    @Override
    public Iterator<S> iterator() {
        return Collections.unmodifiableSet(this.mProviders).iterator();
    }

    private void load() {
        for (final Class<?> provider : ServiceRegistry.get(this.mService)) {
            try {
                final S p = this.mService.cast(provider.newInstance());
                this.mProviders.add(p);
            } catch (final Throwable t) {
                throw new ServiceConfigurationError("Provider " + provider.getName() + " could not be initialized", t);
            }
        }
    }
}
