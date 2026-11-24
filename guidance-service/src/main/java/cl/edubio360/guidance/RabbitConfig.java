package cl.edubio360.guidance;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.amqp.core.Declarables;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "edubio.orientation";
    public static final String QUEUE = "orientation.confirmed.notification";
    public static final String ROUTING_KEY = "orientation.confirmed";
    public static final String DLX = "edubio.orientation.dlx";
    public static final String DLQ = "orientation.confirmed.notification.dlq";
    public static final String DLQ_ROUTING_KEY = "orientation.confirmed.dlq";

    @Bean
    @ConditionalOnProperty(name = "ep3.cloud.events", havingValue = "true")
    Declarables cloudOrientationQueue() {
        Queue dead = QueueBuilder.durable("ep3.cloud.confirmed.dlq").build();
        Queue queue = QueueBuilder.durable("ep3.cloud.confirmed")
                .deadLetterExchange("")
                .deadLetterRoutingKey(dead.getName())
                .build();
        return new Declarables(dead, queue,
                BindingBuilder.bind(queue).to(orientationExchange()).with(ROUTING_KEY));
    }

    @Bean
    DirectExchange orientationExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Binding orientationBinding() {
        return BindingBuilder.bind(notificationQueue()).to(orientationExchange()).with(ROUTING_KEY);
    }

    @Bean
    Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DLQ_ROUTING_KEY);
    }
}
