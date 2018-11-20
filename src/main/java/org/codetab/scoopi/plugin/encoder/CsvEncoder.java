package org.codetab.scoopi.plugin.encoder;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.Validate.notNull;
import static org.apache.commons.lang3.Validate.validState;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.codetab.scoopi.defs.IPluginDef;
import org.codetab.scoopi.model.Axis;
import org.codetab.scoopi.model.Data;
import org.codetab.scoopi.model.Item;
import org.codetab.scoopi.model.Plugin;
import org.codetab.scoopi.model.TaskInfo;

public class CsvEncoder implements IEncoder<List<String>> {

    @Inject
    private TaskInfo taskInfo;
    @Inject
    private IPluginDef pluginDef;

    private Plugin plugin;

    @Override
    public List<String> encode(final Data data) {

        notNull(data, "data must not be null");
        validState(nonNull(plugin), "plugin is not set");

        List<String> encodedData = new ArrayList<>();

        String delimiter = pluginDef.getValue(plugin, "delimiter", ",");
        boolean outputTags =
                Boolean.valueOf(pluginDef.getValue(plugin, "tags", "true"));

        // encode and append data
        for (Item item : data.getItems()) {
            StringBuilder sb = new StringBuilder();
            sb.append(taskInfo.getName());
            sb.append(delimiter);
            sb.append(taskInfo.getGroup());
            sb.append(delimiter);
            if (outputTags) {
                sb.append(item.getParent().getTagValue("page"));
                sb.append(delimiter);
                sb.append(item.getParent().getTagValue("index"));
                sb.append(delimiter);
                sb.append(item.getParent().getTagValue("item"));
                sb.append(delimiter);
            }
            for (Axis axis : item.getAxes()) {
                sb.append(axis.getValue());
                sb.append(delimiter);
            }

            encodedData.add(sb.toString());
        }
        return encodedData;
    }

    @Override
    public void setPlugin(final Plugin plugin) {
        this.plugin = plugin;
    }
}
