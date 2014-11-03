package com.netty.push.handler.process;

import com.netty.push.handler.context.ApplicationContext;

public abstract class AbstractHandleProcessor<T> implements IHandleProcessor{

	private Object processObject;
	protected ApplicationContext applicationContext = ApplicationContext.getInstance();

	@SuppressWarnings("unchecked")
	public T getProcessObject() {
		return (T) processObject;
	}

	public void setProcessObject(Object processObject) {
		this.processObject = null;
		this.processObject = processObject;
	}

	public void updateObject(Object t) {
		setProcessObject(t);
	}

}
