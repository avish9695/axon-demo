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
import org.springframework.jms.support.converter.MessageConversionException;
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

    @Value("#{environment['messaging.content.logging.single.line']}")
    private boolean isNewLineReplacementEnabled = false;

    @Value( "#{spec.isTraceEnabled(T(org.oiavorskyi.axondemo.api.JmsDestinationsSpec).COMMANDS)}" )
    private boolean isTraceEnabled = false;


    @JmsListener( destination = JmsDestinationsSpec.COMMANDS )
    public void onMessage(
            TextMessage jmsMessage,
            @Header( "TestCorrelationID" ) String testCorrelationID
    ) {
        logJMSMessage(jmsMessage);
        String executionStatus = "OK";

        try {
            CargoTrackingCommandMessage commandMessage = convertAndValidateMessage(jmsMessage);
        } catch ( JMSDataExtractionException e ) {
            executionStatus = "FAIL";
            log.error("Input message content extraction has failed: {}",
                    e.getDetailedDescription(true, isNewLineReplacementEnabled));
        }

        reportStatusIfRequired(testCorrelationID, executionStatus);
    }

    private void logJMSMessage( TextMessage jmsMessage ) {
        try {
            log.debug("Received new message with correlationId = {} from {}",
                    jmsMessage.getJMSCorrelationID(),
                    jmsMessage.getJMSDestination());
            if ( isTraceEnabled ) {
                String payload = jmsMessage.getText();
                if (isNewLineReplacementEnabled) {
                    payload = payload.replace('\n', ' ');
                }
                log.debug("Message content:{}{}", (isNewLineReplacementEnabled ? " " : '\n'),
                        payload);
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
            throws JMSDataExtractionException {
        CargoTrackingCommandMessage message;
        try {
            message = (CargoTrackingCommandMessage) converter.fromMessage(jmsMessage);
        } catch ( JMSException | MessageConversionException e ) {
            throw JMSDataExtractionException.Builder
                    .forJmsMessage("Input message content extraction has failed", jmsMessage)
                    .withCause(e)
                    .build();
        }

        BeanPropertyBindingResult validationResults =
                new BeanPropertyBindingResult(message, "cargoTrackingCommand");
        validator.validate(message, validationResults);

        if ( validationResults.hasErrors() ) {
            throw JMSDataExtractionException.Builder
                    .forJmsMessage("Input message content extraction has failed", jmsMessage)
                    .withExtractedObject(message)
                    .withValidationErrors(validationResults)
                    .build();
        }

        return message;
    }

}
