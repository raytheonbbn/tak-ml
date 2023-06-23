/*******************************************************************************
 * DISTRIBUTION C. Distribution authorized to U.S. Government agencies and their contractors. Other requests for this document shall be referred to the United States Air Force Research Laboratory.
 *
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
package com.bbn.takml.mxf;

import java.util.Set;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.tak.ml.mx_framework.MXFrameworkRegistrar;

public class MXFrameworkRogerRegistrar implements MXFrameworkRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(MXFrameworkRogerRegistrar.class);
    private MXFrameworkPlugin mxf;
    private Set<Class<?>> mxpClasses;

    public MXFrameworkRogerRegistrar(MXFrameworkPlugin mxf) {
        this.mxf = mxf;
        this.mxpClasses = new HashSet<Class<?>>();
    }

    @Override
    public boolean register(Class<?> mxpClass) {
        boolean registered = mxf.register(mxpClass);
        if (registered)
            this.mxpClasses.add(mxpClass);
        return registered;
    }

    @Override
    public boolean deregister(Class<?> mxpClass) {
        boolean deregistered = mxf.deregister(mxpClass);;
        this.mxpClasses.remove(mxpClass);
        return deregistered;
    }

    @Override
    public void stop() {
        for (Class<?> mxpClass : this.mxpClasses)
            deregister(mxpClass);
    }
}
