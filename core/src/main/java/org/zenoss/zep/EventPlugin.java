/*
 * Copyright (C) 2010, Zenoss Inc.  All Rights Reserved.
 */
package org.zenoss.zep;

import java.util.Map;
import java.util.Set;

/**
 * General interface defined for all plug-ins. Plug-ins come in two types,
 * {@link EventPreProcessingPlugin} which can make changes to an event
 * occurrence before it is persisted, and {@link EventPostProcessingPlugin}
 * which can handle the processed event.
 */
public interface EventPlugin {
    /**
     * Returns the unique id for the plug-in.
     * 
     * @return The unique id for the plug-in.
     */
    public String getId();

    /**
     * Returns the dependent plug-in ids.
     * 
     * @return The plug-in ids that this plug-in depends on.
     */
    public Set<String> getDependencies();

    /**
     * Returns a descriptive name for the event plug-in.
     * 
     * @return The event plug-in's name.
     */
    public String getName();

    /**
     * Returns properties of the event plug-in.
     * 
     * @return Properties of the event plug-in.
     */
    public Map<String, String> getProperties();

    /**
     * Initializes the plug-in with configuration properties.
     * 
     * @param properties
     *            Configuration properties for the plug-in.
     */
    public void init(Map<String, String> properties);
}