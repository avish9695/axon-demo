package org.oiavorskyi.axondemo.itest.cargotracking;

import org.oiavorskyi.axondemo.itest.JmsRequester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.jms.Destination;
import java.util.concurrent.Future;

@Configuration
public class CargoTrackingSUT {

    @Bean
    public API api() {
        return new JmsAPIImpl();
    }

    public static interface API {

        public Future<String> startCargoTracking( String cargoId, String correlationId, String
                timestamp );

    }

    private static class JmsAPIImpl implements API {

        @SuppressWarnings( "SpringJavaAutowiringInspection" )
        @Autowired
        private JmsRequester requester;

        @Autowired
        private Destination inboundCommandsDestination;

        @Override
        public Future<String> startCargoTracking( String cargoId, String correlationId,
                                                  String timestamp ) {
            return requester.sendRequest(
                    new CargoTrackingCommandMessage("START", cargoId, correlationId, timestamp)
                            .toString(),
                    inboundCommandsDestination);
        }
    }

    /**
     * We don't want to use Command class to marshall the input message as this would hide potential
     * errors when implementation of Command is wrong and our message ends up to not correspond to
     * the actual API. Besides it is good practice in general to not use implementation level
     * classes from application in integration tests.
     */
    private static final class CargoTrackingCommandMessage {

        final String commandId;
        final String cargoId;
        final String correlationId;
        final String timestamp;

        public CargoTrackingCommandMessage( String commandId, String cargoId, String correlationId,
                                            String timestamp ) {
            this.commandId = commandId;
            this.cargoId = cargoId;
            this.correlationId = correlationId;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("{");
            sb.append("\"commandId\": \"").append(commandId).append('\"');
            sb.append(", \"cargoId\": \"").append(cargoId).append('\"');
            sb.append(", \"correlationId\": \"").append(correlationId).append('\"');
            sb.append(", \"timestamp\": \"").append(timestamp).append('\"');
            sb.append('}');
            return sb.toString();
        }
    }
}
