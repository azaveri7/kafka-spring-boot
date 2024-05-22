package com.paathshala.kafka.command.action;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.paathshala.kafka.api.request.PromotionRequest;
import com.paathshala.kafka.broker.message.PromotionMessage;
import com.paathshala.kafka.broker.producer.PromotionProducer;

@Component
public class PromotionAction {

	@Autowired
	private PromotionProducer producer;
	
	public void publishToKafka(PromotionRequest request) {
		var message = new PromotionMessage(request.getPromotionCode());
		
		producer.publish(message);
	}

}
