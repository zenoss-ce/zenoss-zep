
package org.zenoss.zep.zing.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zenoss.protobufs.zep.Zep.EventSummary;
import org.zenoss.protobufs.zep.Zep.Event;
import org.zenoss.protobufs.zep.Zep.EventDetail;
import org.zenoss.protobufs.zep.Zep.EventActor;

import org.zenoss.zep.zing.ZingEventProcessor;
import org.zenoss.zep.zing.ZingConfig;


public class ZingEventProcessorImpl implements ZingEventProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ZingEventProcessorImpl.class);

    private boolean enabled;

    private final ZingConfig config;

    private ZingMessagePublisher publisher = null;

    public ZingEventProcessorImpl(ZingConfig cfg) {
        this.config = cfg;
        logger.info("Zing Event Processor created with config: {}", cfg.toString());
        this.enabled = this.config.forwardEvents();
        if (this.enabled) {
            if (!this.config.validate()) {
                logger.error("Zing configuration is not valid. Events will not be forwarded to Zenoss Cloud");
                this.enabled = false;
            }
        }
    }

    public void init() {
        // FIXME THIS TAKES FOREVER IF IT CANT ACCESS PUBSUB
        logger.info("initializing zing event processor...");
        if (this.enabled) {
            this.publisher = new ZingMessagePublisher(this.config);
        }
        if(this.publisher==null) {
            this.enabled = false;
            logger.error("Could not create publisher. Events will not be forwarded to Zenoss Cloud");
        }
        logger.info("initializing zing event processor...DONE");
    }

    public void processEvent(Event event, EventSummary summary) {
        if (this.enabled) {
            // convert event to zing protobuf and send
            long ts;
            if (!event.hasCreatedTime())
                return;
            ZingEvent.Builder builder = new ZingEvent.Builder(this.config.tenant,
                                                              this.config.source,
                                                              event.getCreatedTime());
            if (event.hasUuid()) builder.setUuid(event.getUuid());
            if (event.hasFingerprint()) builder.setFingerprint(event.getFingerprint());
            if (event.hasSeverity()) builder.setSeverity(event.getSeverity().name());
            EventActor actor = event.getActor();
            if (actor != null) {
                if (actor.hasElementUuid()) builder.setContextUUID(actor.getElementUuid());
                if (actor.hasElementIdentifier()) builder.setContextIdentifier(actor.getElementIdentifier());
                if (actor.hasElementTitle()) builder.setContextTitle(actor.getElementTitle());
                if (actor.hasElementTypeId()) builder.setContextType(actor.getElementTypeId().name());
                if (actor.hasElementSubUuid()) builder.setChildContextUUID(actor.getElementSubUuid());
                if (actor.hasElementSubIdentifier()) builder.setChildContextIdentifier(actor.getElementSubIdentifier());
                if (actor.hasElementSubTitle()) builder.setChildContextTitle(actor.getElementSubTitle());
                if (actor.hasElementSubTypeId()) builder.setChildContextType(actor.getElementSubTypeId().name());
            }
            if (event.hasMessage()) builder.setMessage(event.getMessage());
            if (event.hasSummary()) builder.setSummary(event.getSummary());
            if (event.hasMonitor()) builder.setMonitor(event.getMonitor());
            if (event.hasAgent()) builder.setAgent(event.getAgent());

            if (event.hasEventKey()) builder.setEventKey(event.getEventKey());
            if (event.hasEventClass()) builder.setEventClass(event.getEventClass());
            if (event.hasEventClassKey()) builder.setEventClassKey(event.getEventClassKey());
            if (event.hasEventClassMappingUuid()) builder.setEventClassMappingUuid(event.getEventClassMappingUuid());
            if (event.hasEventGroup()) builder.setEventGroup(event.getEventGroup());

            for (EventDetail d : event.getDetailsList()) {
                builder.setDetail(d.getName(), d.getValueList());
            }

            ZingEvent zingEvent = builder.build();
            logger.info("publishing event {}", zingEvent);
            if (zingEvent.isValid()) {
                this.publisher.publishEvent(zingEvent);
            } else {
                // FIXME remove logging and replace it with instrumentation
                logger.info("DROPPING BAD EVENT!!!!!!");
            }
        }
    }

    public void shutdown() {
        logger.info("SHUTTING DOWN ZING EVENT PROCESSOR");
        if (this.enabled) {
            this.publisher.shutdown();
        }
    }
}
