package org.oiavorskyi.axondemo.listeners;

import org.oiavorskyi.axondemo.api.CargoTrackingCommandMessage;
import org.oiavorskyi.axondemo.api.JmsDestinationsSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.validation.Valid;

@Component
public class CargoTrackingCommandsListener {

    private static Logger log = LoggerFactory.getLogger(CargoTrackingCommandsListener.class);

    @Autowired
    private JmsTemplate jmsTemplate;

    @JmsListener( destination = JmsDestinationsSpec.INBOUND_COMMANDS )
    public void onMessage( @Valid @Payload CargoTrackingCommandMessage message,
                           @Header("TestCorrelationID") String testCorrelationID ) {
        log.debug("Received new message: " + message);

        reportStatusIfRequired(testCorrelationID, "OK");
    }

    private void reportStatusIfRequired( final String testCorrelationID, final String status ) {
        if ( testCorrelationID != null ) {
            log.debug("Sending status {} for testing purposes", status);
            jmsTemplate.send(JmsDestinationsSpec.TEST_STATUS, new MessageCreator() {
                @Override
                public Message createMessage( Session session ) throws JMSException {
                    TextMessage message = session.createTextMessage(status);
                    message.setStringProperty("TestCorrelationID", testCorrelationID);

                    return message;
                }
            });
        }
    }

}
