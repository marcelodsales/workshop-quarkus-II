package com.redhat.monitoring;

import io.quarkus.jfr.runtime.SpanIdRelational;
import io.quarkus.jfr.runtime.TraceIdRelational;
import jdk.jfr.*;
import lombok.Getter;
import lombok.Setter;

@Name("com.redhat.monitoring.DataAccess")
@Label("Data Access Operation")
@Category({"Quarkus","Database"})
@Description("Measure database execution time")
@Getter @Setter
public class DataAccessEvent extends Event {

    @Label("Trace ID")
    @Description("Trace ID to identify the request")
    @TraceIdRelational
    protected String traceId;

    @Label("Span ID")
    @Description("Span ID to identify the request if necessary")
    @SpanIdRelational
    protected String spanId;

    @Label("Entity Class")
    protected String entity;

    @Label("Method Name")
    protected String method;

    @Label("Success")
    protected boolean success;

    @Label("Exception Message")
    protected String exceptionMessage;
}
