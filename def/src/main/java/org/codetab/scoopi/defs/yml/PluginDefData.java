package org.codetab.scoopi.defs.yml;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.codetab.scoopi.model.Plugin;

public class PluginDefData implements Serializable {

    private static final long serialVersionUID = -2820596116660885988L;

    private Map<String, List<Plugin>> pluginMap;

    public PluginDefData() {
    }

    public Map<String, List<Plugin>> getPluginMap() {
        return pluginMap;
    }

    public void setPluginMap(final Map<String, List<Plugin>> pluginMap) {
        this.pluginMap = pluginMap;
    }
}
