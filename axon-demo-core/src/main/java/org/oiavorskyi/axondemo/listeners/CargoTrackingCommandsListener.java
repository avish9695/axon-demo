package org.oiavorskyi.axondemo.listeners;

import org.oiavorskyi.axondemo.api.CargoTrackingCommandMessage;
import org.oiavorskyi.axondemo.api.JmsDestinationsSpec;
import org.oiavorskyi.axondemo.framework.DestinationBasedJackson2MessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Validator;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

@Component
public class CargoTrackingCommandsListener {

    private static Logger log = LoggerFactory.getLogger(CargoTrackingCommandsListener.class);

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private DestinationBasedJackson2MessageConverter converter;

    @Autowired
    private Validator validator;

    @Value( "#{spec.isTraceEnabled(T(org.oiavorskyi.axondemo.api.JmsDestinationsSpec).COMMANDS)}" )
    private Boolean isTraceEnabled;


    @JmsListener( destination = JmsDestinationsSpec.COMMANDS )
    public void onMessage(
            TextMessage jmsMessage,
            @Header( "TestCorrelationID" ) String testCorrelationID
    ) {
        logJMSMessage(jmsMessage);
        String executionStatus = "OK";

        try {
            CargoTrackingCommandMessage commandMessage = convertAndValidateMessage(jmsMessage);
        } catch ( Exception e ) {
            executionStatus = "FAIL";
            log.error("Unable to unmarshal and validate message", e);
        }

        reportStatusIfRequired(testCorrelationID, executionStatus);
    }

    private void logJMSMessage( TextMessage jmsMessage ) {
        try {
            log.debug("Received new message with correlationId = {} from {}",
                    jmsMessage.getJMSCorrelationID(),
                    jmsMessage.getJMSDestination());
            if ( isTraceEnabled ) {
                log.debug("Message content:\n{}", jmsMessage.getText());
            }
        } catch ( JMSException e ) {
            // Ignore as nothing could be done when we can't extract information from message
        }
    }

    private void reportStatusIfRequired( final String testCorrelationID, final String status ) {
        if ( testCorrelationID != null ) {
            log.debug("Sending status {} for testing purposes", status);
            jmsTemplate.send(JmsDestinationsSpec.STATUS, new MessageCreator() {
                @Override
                public Message createMessage( Session session ) throws JMSException {
                    TextMessage message = session.createTextMessage(status);
                    message.setStringProperty("TestCorrelationID", testCorrelationID);

                    return message;
                }
            });
        }
    }

    private CargoTrackingCommandMessage convertAndValidateMessage( TextMessage jmsMessage )
            throws Exception {
        CargoTrackingCommandMessage message = (CargoTrackingCommandMessage)
                converter.fromMessage(jmsMessage);

        BeanPropertyBindingResult validationResults =
                new BeanPropertyBindingResult(message, "cargoTrackingCommand");
        validator.validate(message, validationResults);

        if (validationResults.hasErrors()) {
            throw new IllegalAccessException("Invalid object");
        }

        return message;
    }

}
