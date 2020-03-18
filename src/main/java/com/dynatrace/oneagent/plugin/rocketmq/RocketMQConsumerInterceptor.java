package com.dynatrace.oneagent.plugin.rocketmq;

import com.dynatrace.oneagent.sdk.OneAgentSDKFactory;
import com.dynatrace.oneagent.sdk.api.IncomingMessageProcessTracer;
import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
import com.dynatrace.oneagent.sdk.api.enums.ChannelType;
import com.dynatrace.oneagent.sdk.api.enums.MessageDestinationType;
import com.dynatrace.oneagent.sdk.api.infos.MessagingSystemInfo;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.List;

public class RocketMQConsumerInterceptor {


	private static final OneAgentSDK oneAgentSdk = OneAgentSDKFactory.createInstance();

	private static RocketMQConsumerInterceptor instance;

	public RocketMQConsumerInterceptor() {
	}

	public static RocketMQConsumerInterceptor getInstance(){
		if(instance == null){
			instance = new RocketMQConsumerInterceptor();
		}
		return instance;
	}

	private boolean isActive() {
		switch (oneAgentSdk.getCurrentState()) {
			case ACTIVE:
				return true;
			case PERMANENTLY_INACTIVE:
				return false;
			case TEMPORARILY_INACTIVE:
				return false;
			default:
				return false;
		}
	}


    public Object intercept(ProceedingJoinPoint pointcut, List<MessageExt> msgs, ConsumeConcurrentlyContext context) throws Throwable {
//		System.out.println("=================instrumented method 'RocketMQConsumerInterceptor.intercept(), target method:'"+ pointcut.toShortString() +" called successfully...");
		if(msgs.size() != 1){
			return pointcut.proceed();
		}
		if(!isActive()){
			return pointcut.proceed();
		}
		IncomingMessageProcessTracer incomingMessageProcessTracer = null;
		try {
			MessageExt msg = msgs.get(0);
			MessageQueue mq = context.getMessageQueue();

			MessagingSystemInfo messagingSystemInfo = oneAgentSdk.createMessagingSystemInfo("RocketMQ", msg.getTopic(), MessageDestinationType.TOPIC, ChannelType.TCP_IP, mq.getBrokerName());
			incomingMessageProcessTracer = oneAgentSdk.traceIncomingMessageProcess(messagingSystemInfo);
			incomingMessageProcessTracer.setDynatraceStringTag(msg.getUserProperty(OneAgentSDK.DYNATRACE_MESSAGE_PROPERTYNAME));
			if(msg.getTransactionId() != null && !msg.getTransactionId().trim().isEmpty()){
				incomingMessageProcessTracer.setVendorMessageId(msg.getTransactionId());
			}
			incomingMessageProcessTracer.start();
		}catch (Exception e){
//			e.printStackTrace();
		}

		try {
			return pointcut.proceed();
		} catch (Exception e) {
			if(incomingMessageProcessTracer != null){
				incomingMessageProcessTracer.error(e);
			}
			throw e;
		} finally {
			if(incomingMessageProcessTracer != null){
				incomingMessageProcessTracer.end();
			}
		}
    }
}
