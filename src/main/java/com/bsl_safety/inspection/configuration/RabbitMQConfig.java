package com.bsl_safety.inspection.configuration;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PHOTO_UPLOAD_QUEUE = "photo.upload.queue";
    public static final String PHOTO_UPLOAD_EXCHANGE = "photo.upload.exchange";
    public static final String PHOTO_UPLOAD_ROUTING_KEY = "photo.upload.routingkey";

    //queue where messages sit
    @Bean
    public Queue photoUploadQueue(){
        return QueueBuilder.durable(PHOTO_UPLOAD_QUEUE) //survives RabbitMQ restart
                .withArgument("x-dead-letter-exchange", "photo.upload.dlx") //failed messages go here
                .build();
    }

    //The exchange receives messages and routes them to queues
    @Bean
    public DirectExchange photoUploadExchange(){
        return new DirectExchange(PHOTO_UPLOAD_EXCHANGE);
    }

    //Binding tells the exchange which queue to route to based on routing key
    @Bean
    public Binding photoUploadBinding(){
        return BindingBuilder
                .bind(photoUploadQueue())
                .to(photoUploadExchange())
                .with(PHOTO_UPLOAD_ROUTING_KEY);
    }

    //Dead Letter Queues - messages go here after all retries are exhausted
    @Bean
    public Queue deadLetterQueue(){
        return QueueBuilder.durable("photo.upload.dlq").build();
    }

    @Bean
    public DirectExchange deadLetterExchange(){
        return new DirectExchange("photo.upload.dlx");
    }

    @Bean
    public Binding deadLetterBinding(){
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(PHOTO_UPLOAD_QUEUE);
    }

    //Tells Spring to serialize/deserailize messages as JSON
    @Bean
    public MessageConverter jsonMessageConverter(){
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory){
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }


}
