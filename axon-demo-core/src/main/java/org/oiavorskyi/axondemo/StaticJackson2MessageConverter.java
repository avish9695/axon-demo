package org.oiavorskyi.axondemo;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;

import javax.jms.JMSException;
import javax.jms.Message;

public class StaticJackson2MessageConverter extends MappingJackson2MessageConverter {

    private final JavaType targetType;

    public StaticJackson2MessageConverter( Class<?> targetClass ) {
        super();
        this.targetType = new ObjectMapper().getTypeFactory().constructType(targetClass);
    }

    @Override
    protected JavaType getJavaTypeForMessage( Message message ) throws JMSException {
        System.out.println(message.getJMSDestination());
        return this.targetType;
    }
}
