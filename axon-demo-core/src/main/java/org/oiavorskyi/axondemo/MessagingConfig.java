package org.oiavorskyi.axondemo;

import com.ibm.mq.jms.MQConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.oiavorskyi.axondemo.api.CargoTrackingCommandMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.ClassUtils;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@Configuration
@PropertySources( {
        @PropertySource( "/messaging.properties" ),
        @PropertySource( value = "/messaging-${execution.profile}.properties",
                ignoreResourceNotFound = true )
} )
@EnableJms
public class MessagingConfig {

    private static Logger log = LoggerFactory.getLogger(MessagingConfig.class);

    @Value( "#{environment.getProperty('messaging.connection.pool.size')}" )
    private int connectionPoolSize;

    @Value( "#{environment.getProperty('messaging.dest.impl.class')}" )
    private Class<Destination> destinationImplClass;

    // Destinations
    @Value( "#{environment.getProperty('messaging.dest.test.status')}" )
    private String testStatusDestinationName;

    @Value( "#{environment.getProperty('messaging.dest.inbound.commands')}" )
    private String inboundCommandsDestinationName;

    @Bean
    public JmsTemplate jmsTemplate( ConnectionFactory jmsConnectionFactory ) {
        return new JmsTemplate(jmsConnectionFactory);
    }

    @Bean
    public ConnectionFactory jmsConnectionFactory( ConnectionFactory rawConnectionFactory ) {
        CachingConnectionFactory connFactory = new CachingConnectionFactory(rawConnectionFactory);
        connFactory.setSessionCacheSize(connectionPoolSize);

        return connFactory;
    }

    // TODO: Replace with destination resolver
    @Bean
    public Destination inboundCommandsDestination()
            throws IllegalAccessException, InstantiationException, InvocationTargetException {
        return createEnvironmentSpecificDestination(inboundCommandsDestinationName);
    }

    @Bean
    public Destination testStatusDestination()
            throws IllegalAccessException, InstantiationException, InvocationTargetException {
        return createEnvironmentSpecificDestination(testStatusDestinationName);
    }

    /**
     * Creates JmsListenerContainers when needed for specific listeners
     */
    @Bean
    public ExtSimpleJmsListenerContainerFactory jmsListenerContainerFactory(
            ConnectionFactory jmsConnectionFactory ) {
        ExtSimpleJmsListenerContainerFactory factory =
                new ExtSimpleJmsListenerContainerFactory();
        factory.setConnectionFactory(jmsConnectionFactory);
//        factory.setDestinationResolver(destinationResolver());
        factory.setMessageConverter(
                new StaticJackson2MessageConverter(CargoTrackingCommandMessage.class));
        return factory;
    }

//    @Bean
//    @Autowired
//    public SimpleMessageListenerContainer commandsJmsListener(
//            CargoTrackingCommandsListener targetListener,
//            ConnectionFactory jmsConnectionFactory,
//            Destination inboundCommandsDestination ) {
//        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
//
//        container.setConnectionFactory(jmsConnectionFactory(jmsConnectionFactory));
//        container.setDestination(inboundCommandsDestination);
//        container.setConcurrentConsumers(1);
//        container.setMessageListener(createJSONUnmarshalingListener(targetListener,
//                CargoTrackingCommandMessage.class));
//
//        return container;
//    }
//
//    private MessageListener createJSONUnmarshalingListener( Object targetListener, Class<?>
//            targetPayloadClass) {
//        MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(targetListener);
//        listenerAdapter.setDefaultListenerMethod("onMessage");
//        listenerAdapter.setMessageConverter(new StaticJackson2MessageConverter
// (targetPayloadClass));
//
//        return listenerAdapter;
//    }


    private Destination createEnvironmentSpecificDestination( String destinationName )
            throws IllegalAccessException, InvocationTargetException, InstantiationException {
        log.debug("Creating {} destination using {} class", destinationName, destinationImplClass);

        if ( !ClassUtils.isAssignable(Destination.class, destinationImplClass) ) {
            log.error("Creation of {} destination failed - {} is not assignable to javax.jms" +
                    ".Destination. Please check value of property 'messaging.dest.impl.class' in " +
                    "configuration", destinationName, destinationImplClass);
            throw new IllegalArgumentException(destinationImplClass +
                    " is not assignable to javax.jms.Destination");
        }

        Constructor<Destination> constructor =
                ClassUtils.getConstructorIfAvailable(destinationImplClass, String.class);

        return constructor.newInstance(destinationName);
    }

    @Profile( "default" )
    @Configuration
    public static class DevelopmentConfig {

        @Bean
        public ConnectionFactory rawConnectionFactory() {
            return new ActiveMQConnectionFactory();
        }

    }

    @Profile( "production" )
    @Configuration
    public static class ProductionConfig {

        @Bean
        public ConnectionFactory rawConnectionFactory() {
            return new MQConnectionFactory();
        }

    }

}
