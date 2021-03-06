package org.codetab.scoopi.step.process;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codetab.scoopi.defs.IItemDef;
import org.codetab.scoopi.metrics.Errors;
import org.codetab.scoopi.model.Axis;
import org.codetab.scoopi.model.Item;
import org.codetab.scoopi.model.Locator;
import org.codetab.scoopi.model.LocatorGroup;
import org.codetab.scoopi.model.ObjectFactory;

import com.google.common.collect.Lists;

public class LocatorGroupFactory {

    private static final Logger LOG = LogManager.getLogger();

    @Inject
    private IItemDef itemDef;
    @Inject
    private ObjectFactory objectFactory;
    @Inject
    private Errors errors;

    public List<LocatorGroup> createLocatorGroups(final String dataDef,
            final List<Item> items, final String locatorName) {
        Map<String, LocatorGroup> lgs = new HashMap<>();

        for (Item item : items) {

            Axis axis = item.getFirstAxis();
            Optional<String> oLinkGroup =
                    itemDef.getLinkGroup(dataDef, axis.getItemName());
            Optional<List<String>> oLinkBreakOn =
                    itemDef.getLinkBreakOn(dataDef, axis.getItemName());

            if (oLinkGroup.isPresent()) {
                String linkGroup = oLinkGroup.get();
                String url = axis.getValue();
                boolean createLink = true;
                if (oLinkBreakOn.isPresent()) {
                    if (oLinkBreakOn.get().contains(url)) {
                        createLink = false;
                    }
                }
                if (StringUtils.isNotBlank(url) && createLink) {
                    Locator locator = objectFactory.createLocator(locatorName,
                            linkGroup, url);
                    if (!lgs.containsKey(linkGroup)) {
                        LocatorGroup lg =
                                objectFactory.createLocatorGroup(linkGroup);
                        // LocatorGroup not defined by defs
                        lg.setByDef(false);
                        lgs.put(linkGroup, lg);
                    }
                    LocatorGroup lg = lgs.get(linkGroup);
                    lg.getLocators().add(locator);
                }
            } else {
                String label = String.join(":", dataDef,
                        item.getAxes().get(0).getItemName());
                errors.inc();
                LOG.error(
                        "create locator from link, no linkGroup defined for item: {}",
                        label);
            }
        }
        return Lists.newArrayList(lgs.values());
    }
}
