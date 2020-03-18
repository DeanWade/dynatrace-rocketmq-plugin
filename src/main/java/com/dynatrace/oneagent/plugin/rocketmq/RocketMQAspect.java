package com.dynatrace.oneagent.plugin.rocketmq;

import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.impl.CommunicationMode;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.List;

@Aspect
public class RocketMQAspect {

	@Around("execution(* com.example.HomeController.home(..))")
	public Object intercept(ProceedingJoinPoint pointcut) throws Throwable {
		Object result = pointcut.proceed();
		System.out.println("RocketMQAspect:"+pointcut.toShortString() + ": " + result);
		return result;
	}

	@Around("execution(* org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently.consumeMessage(..)) && args(msgs, context)")
	public Object consumeMessage(ProceedingJoinPoint pointcut, List<MessageExt> msgs, ConsumeConcurrentlyContext context)throws Throwable {
		return RocketMQConsumerInterceptor.getInstance().intercept(pointcut, msgs, context);
	}

	//org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl
	@Around("execution(* org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl.sendDefaultImpl(..)) && args(msg, communicationMode, sendCallback, timeout)")
	public Object sendDefaultImpl(ProceedingJoinPoint pointcut, Message msg, final CommunicationMode communicationMode, final SendCallback sendCallback, final long timeout) throws Throwable{
		return RocketMQProducerInterceptor.getInstance().intercept(pointcut, msg);
	}

//	org.apache.rocketmq.client.producer.DefaultMQProducer
//	@Around("execution(* org.apache.rocketmq.client.producer.DefaultMQProducer.send(..)) && args(msg)")
//	public Object send1(ProceedingJoinPoint pointcut,Message msg) throws Throwable{
//		return RocketMQProducerInterceptor.getInstance().intercept(pointcut, msg);
//	}
//
//	@Around("execution(* org.apache.rocketmq.client.producer.DefaultMQProducer.send(..)) && args(msg, timeout)")
//	public Object send2(ProceedingJoinPoint pointcut, Message msg, long timeout) throws Throwable{
//		return RocketMQProducerInterceptor.getInstance().intercept(pointcut, msg);
//	}
//
//	@Around("execution(* org.apache.rocketmq.client.producer.DefaultMQProducer.request(..)) && args(msg, timeout)")
//	public Object request(ProceedingJoinPoint pointcut, Message msg, long timeout) throws Throwable{
//		return RocketMQProducerInterceptor.getInstance().intercept(pointcut, msg);
//	}

}
