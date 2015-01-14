package org.oiavorskyi.axondemo.api;

import java.util.HashMap;
import java.util.Map;

public final class JmsDestinationsSpec {

    // Inbound destinations
    public final static String INBOUND_COMMANDS = "messaging.dest.inbound.commands";

    // Test destinations
    public final static String TEST_STATUS = "messaging.dest.test.status";

    /**
     * Contains mapping between all queues and expected message types in them
     */
    public final static Map<String, Class<?>> INPUT_MESSAGES_TYPES_MAP;

    static {
        INPUT_MESSAGES_TYPES_MAP = new HashMap<>();
        INPUT_MESSAGES_TYPES_MAP.put(INBOUND_COMMANDS, CargoTrackingCommandMessage.class);
    }

}
