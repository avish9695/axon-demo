package org.oiavorskyi.axondemo.listeners;

import org.oiavorskyi.axondemo.api.CargoTrackingCommandMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.jms.*;
import javax.validation.Valid;
import java.util.Map;

@Component
public class CargoTrackingCommandsListener {

    private static Logger log = LoggerFactory.getLogger(CargoTrackingCommandsListener.class);

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private Destination testStatusDestination;

    @JmsListener(destination = "QUEUE.AXONDEMO.COMMANDS")
    public void onMessage( @Valid @Payload CargoTrackingCommandMessage message,
                           @Headers Map<String, String> headers ) {
        log.debug("Received new message: " + message);

        String testCorrelationID = headers.get("TestCorrelationID");

        dispatchCargoTrackingCommand(message);
        reportStatusIfRequired(testCorrelationID, "OK");
    }

    private void reportStatusIfRequired( final String testCorrelationID, final String status ) {
        if ( testCorrelationID != null ) {
            log.debug("Sending status {} for testing purposes", status);
            jmsTemplate.send(testStatusDestination, new MessageCreator() {
                @Override
                public Message createMessage( Session session ) throws JMSException {
                    TextMessage message = session.createTextMessage(status);
                    message.setStringProperty("TestCorrelationID", testCorrelationID);

                    return message;
                }
            });
        }
    }

    private void dispatchCargoTrackingCommand( CargoTrackingCommandMessage message ) {
    }

}
