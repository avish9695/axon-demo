package org.oiavorskyi.axondemo;

import com.ibm.mq.jms.MQConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.oiavorskyi.axondemo.framework.DestinationBasedJackson2MessageConverter;
import org.oiavorskyi.axondemo.framework.ExtSimpleJmsListenerContainerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListenerConfigurer;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.DestinationResolutionException;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.validation.Validator;

import javax.jms.*;

@Configuration
@PropertySources( {
        @PropertySource( "/messaging.properties" ),
        @PropertySource( value = "/messaging-${execution.profile}.properties",
                ignoreResourceNotFound = true )
} )
@EnableJms
public class MessagingConfig implements JmsListenerConfigurer {

    private static Logger log = LoggerFactory.getLogger(MessagingConfig.class);

    @Value( "#{environment.getProperty('messaging.connection.pool.size')}" )
    private int connectionPoolSize;

    @Autowired
    private Validator validator;

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
            ConnectionFactory jmsConnectionFactory,
            DestinationResolver destinationResolver,
            DestinationBasedJackson2MessageConverter converter ) {

        ExtSimpleJmsListenerContainerFactory factory =
                new ExtSimpleJmsListenerContainerFactory();

        factory.setConnectionFactory(jmsConnectionFactory);
        factory.setDestinationResolver(destinationResolver);
        factory.setMessageConverter(converter);

        return factory;
    }

    @Bean
    public DestinationResolver externalizableDestinationResolver(
            final GenericApplicationContext ctx,
            final DestinationBasedJackson2MessageConverter converter ) {
        return new DestinationResolver() {
            @Override
            public Destination resolveDestinationName( Session session, String destinationAlias,
                                                       boolean pubSubDomain ) throws JMSException {
                String actualDestinationName = ctx.getEnvironment().getProperty(destinationAlias);

                if ( actualDestinationName == null ) {
                    log.error("Cannot resolve destination for alias {}", destinationAlias);
                    throw new DestinationResolutionException("Destination with alias " +
                            destinationAlias + " cannot be resolved");
                }

                Queue queue = session.createQueue(actualDestinationName);

                converter.registerDestinationAlias(queue.toString(), destinationAlias);
                return queue;
            }
        };
    }

    @Override
    public void configureJmsListeners( JmsListenerEndpointRegistrar registrar ) {
        registrar.setMessageHandlerMethodFactory(validatingHandlerMethodFactory());
    }

    @Bean
    public DefaultMessageHandlerMethodFactory validatingHandlerMethodFactory() {
        DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();
        factory.setValidator(validator);
        return factory;
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
