package com.freeftr.batch.common.config;

import com.freeftr.batch.couponhistory.dto.event.CouponHistoryEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.kafka.listener.ContainerProperties.AckMode.BATCH;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapServers;

	@Value("${spring.kafka.consumer.group-id}")
	private String groupId;

	@Bean
	public ConsumerFactory<String, CouponHistoryEvent> consumerFactory() {
		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

		//커밋을 true로 하면 자동으로 커밋을 함으로써 오프셋 증가 => 학습 필요
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
		props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1048576);

		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

		props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
		props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.freeftr.batch.couponhistory.dto.event.CouponHistoryEvent");
		props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.freeftr.batch.couponhistory.dto.event.CouponHistoryEvent");

		return new DefaultKafkaConsumerFactory<>(
				props,
				new StringDeserializer(),
				new JsonDeserializer<>(CouponHistoryEvent.class, false)
		);
	}

	@Bean(name = "couponBatchListenerFactory")
	public ConcurrentKafkaListenerContainerFactory<String, CouponHistoryEvent> kafkaListenerContainerFactory(
			ConsumerFactory<String, CouponHistoryEvent> consumerFactory) {

		var factory = new ConcurrentKafkaListenerContainerFactory<String, CouponHistoryEvent>();
		factory.setConsumerFactory(consumerFactory);
		factory.setBatchListener(true);
		factory.getContainerProperties().setAckMode(BATCH);

		return factory;
	}
}
