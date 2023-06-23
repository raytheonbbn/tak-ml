package com.atakmap.android.takml_android;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TestTakml extends Takml{

    protected TestTakml(){
        super(null);
    }

    @Override
    protected void discoverMxPlugins() {
        pluginNamesFoundOnClasspath.add(ExampleMXPlugin.class.getName());
        pluginNamesFoundOnClasspath.add(ExampleMXPlugin2.class.getName());

        fileExtensionToMxPluginClassNames.computeIfAbsent(".test", k ->
                    new HashSet<>()).add(ExampleMXPlugin.class.getName());
        fileExtensionToMxPluginClassNames.computeIfAbsent(".test2", k ->
                new HashSet<>()).add(ExampleMXPlugin2.class.getName());
    }

}
