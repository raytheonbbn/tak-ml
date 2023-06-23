package com.bbn.tak.ml.mx_framework;

/**
 * A registrar for MX plugins.
 * <p>
 * Each plugin must register itself with the MX framework
 * on startup and deregister itself on shutdown.
 * <p>
 * Depending on the implementation of the MX framework
 * (client-side or server-side), the registrar may be
 * stateful, and upon shutdown callers must invoke {@link #stop()}.
 */
public interface MXFrameworkRegistrar {

    /**
     * Registers an MX plugin with the MX framework.
     *
     * @param mxpClass an implementing class of {@link MXPlugin} to be registered
     * @return whether the registration was successful
     */
    public boolean register(Class<?> mxpClass);

    /**
     * Deregisters an MX plugin with the MX framework.
     *
     * @param mxpClass an implementing class of {@link MXPlugin} to be deregistered
     * @return whether the registration was successful
     */
    public boolean deregister(Class<?> mxpClass);

    /**
     * Stop the registrar.
     */
    public void stop();
}
