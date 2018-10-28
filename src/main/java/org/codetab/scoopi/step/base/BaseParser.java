package org.codetab.scoopi.step.base;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.Validate.validState;
import static org.codetab.scoopi.util.Util.LINE;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.script.ScriptException;

import org.apache.commons.lang3.time.StopWatch;
import org.codetab.scoopi.defs.IDataDefDefs;
import org.codetab.scoopi.exception.DataDefNotFoundException;
import org.codetab.scoopi.exception.DefNotFoundException;
import org.codetab.scoopi.exception.StepRunException;
import org.codetab.scoopi.model.Data;
import org.codetab.scoopi.model.DataComponent;
import org.codetab.scoopi.model.DataDef;
import org.codetab.scoopi.model.Document;
import org.codetab.scoopi.model.Item;
import org.codetab.scoopi.model.factory.DataFactory;
import org.codetab.scoopi.model.helper.DataHelper;
import org.codetab.scoopi.persistence.DataPersistence;
import org.codetab.scoopi.step.Step;
import org.codetab.scoopi.step.parse.IValueParser;
import org.codetab.scoopi.step.parse.ItemProcessor;
import org.codetab.scoopi.step.parse.ItemStack;
import org.codetab.scoopi.step.parse.ValueProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;

public abstract class BaseParser extends Step {

    static final Logger LOGGER = LoggerFactory.getLogger(BaseParser.class);

    @Inject
    private ItemStack itemStack;
    @Inject
    private ValueProcessor valueProcessor;
    @Inject
    private ItemProcessor itemProcessor;
    @Inject
    private DataPersistence dataPersistence;
    @Inject
    private DataFactory dataFactory;
    @Inject
    private IDataDefDefs dataDefDefs;
    @Inject
    private DataHelper dataHelper;
    @Inject
    private StopWatch timer;

    private Data data;
    protected Document document;

    /**
     * set by subclass
     */
    private IValueParser valueParser;

    @Override
    public boolean initialize() {
        validState(nonNull(getPayload()), "payload is null");
        validState(nonNull(getPayload().getData()), "payload data is null");

        timer.start();

        Object pData = getPayload().getData();
        if (pData instanceof Document) {
            document = (Document) pData;
        } else {
            String message =
                    String.join(" ", "payload data type is not Document but",
                            pData.getClass().getName());
            throw new StepRunException(message);
        }

        // TODO move this to load as loadPage()
        return postInitialize();
    }

    protected abstract boolean postInitialize();

    @Override
    public boolean load() {
        try {
            String dataDefName = getJobInfo().getDataDef();
            Long dataDefId = dataDefDefs.getDataDefId(dataDefName);
            Long documentId = document.getId();
            if (nonNull(documentId) && nonNull(dataDefId)) {
                data = dataPersistence.loadData(dataDefId, documentId);
            }
            return true;
        } catch (DataDefNotFoundException e) {
            String message = "unable to get datadef id";
            throw new StepRunException(message, e);
        }
    }

    @Override
    public boolean store() {
        if (persist()) {
            if (dataPersistence.storeData(data)) {
                data = dataPersistence.loadData(data.getId());
                setOutput(data);
                LOGGER.debug(marker, getLabeled("data stored"));
            }
        } else {
            LOGGER.debug(marker, getLabeled("persist false, data not stored"));
        }
        return true;
    }

    @Override
    public boolean process() {
        Counter dataParseCounter =
                metricsHelper.getCounter(this, "data", "parse");
        Counter dataReuseCounter =
                metricsHelper.getCounter(this, "data", "reuse");
        if (data == null) {
            try {
                LOGGER.debug(marker, "{}", getLabeled("parse data"));
                String dataDefName = getJobInfo().getDataDef();
                data = dataFactory.createData(dataDefName, document.getId(),
                        getJobInfo().getLabel());

                dataHelper.addPageTag(data);
                dataHelper.addItemTag(data);
                dataHelper.addAxisTags(data, null);

                parse();
                setConsistent(true);
                dataParseCounter.inc();
            } catch (IllegalAccessException | InvocationTargetException
                    | NoSuchMethodException | DataDefNotFoundException
                    | ScriptException e) {
                String message = "unable to parse data";
                throw new StepRunException(message, e);
            }
        } else {
            setConsistent(true);
            dataReuseCounter.inc();
            LOGGER.debug(marker, "{}", getLabeled("data exists, reuse"));
        }

        setOutput(data);

        timer.stop();
        LOGGER.trace(marker, "parse time: {}", timer.toString());

        return true;
    }

    protected void setValueParser(final IValueParser valueParser) {
        this.valueParser = valueParser;
    }

    private void parse()
            throws IllegalAccessException, InvocationTargetException,
            NoSuchMethodException, DataDefNotFoundException, ScriptException {

        valueProcessor.addScriptObject("document", document);
        valueProcessor.addScriptObject("configs", configService);

        itemStack.pushItems(data.getItems());

        List<DataComponent> items = new ArrayList<>(); // expanded item list
        String dataDefName = getJobInfo().getDataDef();
        DataDef dataDef = dataDefDefs.getDataDef(dataDefName);

        List<DataComponent> addItems = new ArrayList<>();
        List<DataComponent> removeItems = new ArrayList<>();

        while (!itemStack.isEmpty()) {
            Item item = itemStack.popItem();
            item.setParent(data);
            items.add(item);
            // collections.sort not possible as axes is a Set so implied sort
            // as value field of an axis may be referred by later axis
            valueProcessor.setAxisValues(dataDef, item, valueParser);
            // TODO write test
            Optional<Data> extraData =
                    itemProcessor.createItems(dataDef, data, item, valueParser);
            if (extraData.isPresent()) {
                addItems.add(extraData.get());
                removeItems.add(item);
            }
            itemStack.pushAdjacentItems(dataDef, item);
        }

        data.setItems(items); // replace with expanded item list

        dataHelper.removeItems(data, removeItems);
        dataHelper.addItems(data, addItems);

        LOGGER.trace(marker, "-- data after parse --{}{}{}", LINE, getLabel(),
                LINE, data);
    }

    private boolean persist() {
        // TODO code and move it to yaml
        // write itest and verify Ex-12
        String taskGroup = getJobInfo().getGroup();
        String taskName = getJobInfo().getTask();
        boolean persistData = true;
        try {
            persistData = Boolean.valueOf(taskDefs.getFieldValue(taskGroup,
                    taskName, "persist", "data"));
        } catch (DefNotFoundException e) {
        }
        Optional<Boolean> taskLevelPersistence =
                Optional.ofNullable(persistData);
        return dataPersistence.persist(taskLevelPersistence);
    }
}
