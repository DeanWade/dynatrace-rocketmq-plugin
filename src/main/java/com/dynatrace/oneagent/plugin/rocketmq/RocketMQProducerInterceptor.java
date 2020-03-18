package com.dynatrace.oneagent.plugin.rocketmq;

import com.dynatrace.oneagent.sdk.OneAgentSDKFactory;
import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
import com.dynatrace.oneagent.sdk.api.OutgoingMessageTracer;
import com.dynatrace.oneagent.sdk.api.enums.ChannelType;
import com.dynatrace.oneagent.sdk.api.enums.MessageDestinationType;
import com.dynatrace.oneagent.sdk.api.infos.MessagingSystemInfo;
import org.apache.rocketmq.common.message.Message;
import org.aspectj.lang.ProceedingJoinPoint;

public class RocketMQProducerInterceptor {

	private static final OneAgentSDK oneAgentSdk = OneAgentSDKFactory.createInstance();

	private static RocketMQProducerInterceptor instance;

	public RocketMQProducerInterceptor() {
	}

	public static RocketMQProducerInterceptor getInstance(){
		if(instance == null){
			instance = new RocketMQProducerInterceptor();
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


	public Object intercept(ProceedingJoinPoint pointcut, Message msg) throws Throwable {
//		System.out.println("=================instrumented method 'RocketMQProducerInterceptor.intercept(), target method:'"+ pointcut.toShortString() +" called successfully...");
		if(!isActive()){
			return pointcut.proceed();
		}
		OutgoingMessageTracer outgoingMessageTracer = null;
		try{
			MessagingSystemInfo messagingSystemInfo = oneAgentSdk.createMessagingSystemInfo("RocketMQ", msg.getTopic(), MessageDestinationType.QUEUE, ChannelType.TCP_IP, null);
			outgoingMessageTracer = oneAgentSdk.traceOutgoingMessage(messagingSystemInfo);
			outgoingMessageTracer.start();

			msg.putUserProperty(OneAgentSDK.DYNATRACE_MESSAGE_PROPERTYNAME, outgoingMessageTracer.getDynatraceStringTag());
			if(msg.getTransactionId() != null && !msg.getTransactionId().trim().isEmpty()){
				outgoingMessageTracer.setVendorMessageId(msg.getTransactionId());
			}
		} catch (Exception e){
//			e.printStackTrace();
		}

		try{
			return pointcut.proceed();
		} catch (Exception e) {
			if(outgoingMessageTracer != null){
				outgoingMessageTracer.error(e);
			}
			throw e;
		} finally {
			if(outgoingMessageTracer != null){
				outgoingMessageTracer.end();
			}
		}
	}
}
