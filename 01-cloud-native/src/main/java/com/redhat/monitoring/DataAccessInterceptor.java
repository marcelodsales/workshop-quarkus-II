package com.redhat.monitoring;

import io.quarkus.jfr.runtime.IdProducer;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@TrackDataAccess
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class DataAccessInterceptor {

    @Inject
    IdProducer idProducer;

    @AroundInvoke
    public Object profile(InvocationContext context) throws Exception {
        DataAccessEvent event = new DataAccessEvent();
        event.setEntity(context.getTarget().getClass().getSimpleName());
        event.setMethod(context.getMethod().getName());
        event.begin();
        try {
            Object result = context.proceed();
            event.setSuccess(true);
            return result;
        } catch (Exception e) {
            event.setSuccess(false);
            event.setExceptionMessage(e.getMessage());
            throw e;
        } finally {
            event.end();
            if (event.shouldCommit()) {
                event.setTraceId(idProducer.getTraceId());
                event.setSpanId(idProducer.getSpanId());
                event.commit();
            }
        }
    }
}