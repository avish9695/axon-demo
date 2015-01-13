package org.oiavorskyi.axondemo;

import com.ibm.mq.jms.MQConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.oiavorskyi.axondemo.api.CargoTrackingCommandMessage;
import org.oiavorskyi.axondemo.framework.ExtSimpleJmsListenerContainerFactory;
import org.oiavorskyi.axondemo.framework.StaticJackson2MessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.DestinationResolutionException;
import org.springframework.jms.support.destination.DestinationResolver;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

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

    @Bean
    public JmsTemplate jmsTemplate( ConnectionFactory jmsConnectionFactory,
                                    DestinationResolver destinationResolver ) {
        JmsTemplate jmsTemplate = new JmsTemplate(jmsConnectionFactory);
        jmsTemplate.setDestinationResolver(destinationResolver);
        return jmsTemplate;
    }

    @Bean
    public ConnectionFactory jmsConnectionFactory( ConnectionFactory rawConnectionFactory ) {
        CachingConnectionFactory connFactory = new CachingConnectionFactory(rawConnectionFactory);
        connFactory.setSessionCacheSize(connectionPoolSize);

        return connFactory;
    }

    /**
     * Creates JmsListenerContainers when needed for specific listeners
     */
    @Bean
    public ExtSimpleJmsListenerContainerFactory jmsListenerContainerFactory(
            ConnectionFactory jmsConnectionFactory, DestinationResolver destinationResolver ) {
        ExtSimpleJmsListenerContainerFactory factory =
                new ExtSimpleJmsListenerContainerFactory();
        factory.setConnectionFactory(jmsConnectionFactory);
        factory.setDestinationResolver(destinationResolver);
        factory.setMessageConverter(
                new StaticJackson2MessageConverter(CargoTrackingCommandMessage.class));
        return factory;
    }

    @Bean
    public DestinationResolver externalizableDestinationResolver(
            final GenericApplicationContext ctx ) {
        return new DestinationResolver() {
            @Override
            public Destination resolveDestinationName( Session session, String destinationName,
                                                       boolean pubSubDomain ) throws JMSException {
                String actualDestinationName = ctx.getEnvironment().getProperty(destinationName);

                if ( actualDestinationName == null ) {
                    log.error("Cannot find required destination {} in properties", destinationName);
                    throw new DestinationResolutionException("Destination " + destinationName +
                            " cannot be resolved");
                }

                return session.createQueue(actualDestinationName);
            }
        };
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
