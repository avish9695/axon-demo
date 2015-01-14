package org.oiavorskyi.axondemo.framework;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.oiavorskyi.axondemo.api.JmsDestinationsSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component( "jmsJSONMessageConverter" )
public class DestinationBasedJackson2MessageConverter extends MappingJackson2MessageConverter {

    private static Logger log =
            LoggerFactory.getLogger(DestinationBasedJackson2MessageConverter.class);

    private final Map<String, JavaType> destinationToTypeMap = new ConcurrentHashMap<>();
    private final TypeFactory typeFactory;

    public DestinationBasedJackson2MessageConverter() {
        super();
        typeFactory = new ObjectMapper().getTypeFactory();
    }

    public void registerDestinationAlias( String destination, String alias ) {
        if (destinationToTypeMap.containsKey(destination)) {
            // Already registered, skipping
            return;
        }

        log.debug("Registering destination {} with alias {}", destination, alias);
        Class<?> targetClass = JmsDestinationsSpec.INPUT_MESSAGES_TYPES_MAP.get(alias);
        if ( targetClass == null ) {
            log.info("No configured class found for destination with alias {}. Attempts to " +
                    "auto convert messages from this destination to objects fail", alias);
            return;
        }

        destinationToTypeMap.put(destination, typeFactory.constructType(targetClass));
    }

    @Override
    protected JavaType getJavaTypeForMessage( Message message ) throws JMSException {
        return destinationToTypeMap.get(message.getJMSDestination().toString());
    }
}
