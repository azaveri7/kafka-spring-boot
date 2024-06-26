package com.paathshala.kafka.broker.producer;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

import com.paathshala.kafka.broker.message.OrderMessage;

@Service
public class OrderProducer {

	private static final Logger LOG = LoggerFactory.getLogger(OrderProducer.class);

	@Autowired
	private KafkaTemplate<String, OrderMessage> kafkaTemplate;

	public void publish(OrderMessage message) throws ExecutionException, InterruptedException {
		var producerRecord = buildProducerRecord(message);

		/*kafkaTemplate.send(producerRecord)
				.addCallback(new ListenableFutureCallback<SendResult<String, OrderMessage>>() {

					@Override
					public void onSuccess(SendResult<String, OrderMessage> result) {
						LOG.info("Order {}, item {} published succesfully", message.getOrderNumber(),
								message.getItemName());
					}

					@Override
					public void onFailure(Throwable ex) {
						LOG.warn("Order {}, item {} failed to publish because {}", message.getOrderNumber(),
								message.getItemName(), ex.getMessage());
					}
				});*/

		CompletableFuture<SendResult<String, OrderMessage>> future = kafkaTemplate.send(producerRecord);
		CompletableFuture<String> mapped = future
				.thenApply(result -> {
					// or future.thenApply(SendResult::getMetadata).thenApply(metadata ->...
					var metadata = result.getRecordMetadata();
					var partition = metadata.partition();
					var offset = metadata.offset();
					//log success
					LOG.info("Order {}, item {} published succesfully", message.getOrderNumber(),
							message.getItemName());
					return String.format("%d-%d", partition, offset);

				}).exceptionally(err -> {
					//log error - shouldn't this be "payload" instead of "message"?
					LOG.warn("Order {}, item {} failed to publish because {}", message.getOrderNumber(),
							message.getItemName(), err.getMessage());
					return null;
				});

		LOG.info("Just a dummy message for order {}, item {}", message.getOrderNumber(), message.getItemName());

		mapped.get();
	}

	private ProducerRecord<String, OrderMessage> buildProducerRecord(OrderMessage message) {
		var surpriseBonus = StringUtils.startsWithIgnoreCase(message.getOrderLocation(), "A") ? 25 : 15;
		var headers = new ArrayList<Header>();
		var surpriseBonusHeader = new RecordHeader("surpriseBonus", Integer.toString(surpriseBonus).getBytes());

		headers.add(surpriseBonusHeader);

		return new ProducerRecord<String, OrderMessage>("t-commodity-order", null,
				message.getOrderNumber(), message,
				headers);
	}

}
