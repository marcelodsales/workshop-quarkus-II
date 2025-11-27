package com.redhat.exception.mapper;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.redhat.exception.AccountAlreadyExistsException;
import com.redhat.exception.AccountNotFoundException;
import com.redhat.exception.InsufficientBalanceException;
import com.redhat.rest.dto.ErrorResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.LocalDateTime;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Exception exception) {
        ErrorResponse errorResponse = buildErrorResponse(exception);
        return Response.status(errorResponse.getStatus())
                .entity(errorResponse)
                .build();
    }

    private ErrorResponse buildErrorResponse(Exception exception) {
        String path = uriInfo != null ? uriInfo.getPath() : null;
        
        if (exception instanceof JsonParseException) {
            return ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(Response.Status.BAD_REQUEST.getStatusCode())
                    .error("Bad Request")
                    .message("Invalid JSON format: " + extractJsonErrorMessage(exception))
                    .path(path)
                    .build();
        }
        
        if (exception instanceof InvalidFormatException ife) {
            String fieldName = ife.getPath().isEmpty() ? "field" : ife.getPath().get(0).getFieldName();
            return ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(Response.Status.BAD_REQUEST.getStatusCode())
                    .error("Bad Request")
                    .message(String.format("Invalid value for field '%s': %s", fieldName, ife.getValue()))
                    .path(path)
                    .build();
        }
        
        if (exception instanceof MismatchedInputException mie) {
            String fieldName = mie.getPath().isEmpty() ? "field" : mie.getPath().get(0).getFieldName();
            return ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(Response.Status.BAD_REQUEST.getStatusCode())
                    .error("Bad Request")
                    .message(String.format("Missing or invalid field: '%s'", fieldName))
                    .path(path)
                    .build();
        }
        
        if (exception instanceof JsonProcessingException jpe) {
            return ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(Response.Status.BAD_REQUEST.getStatusCode())
                    .error("Bad Request")
                    .message("Invalid JSON: " + extractJsonErrorMessage(jpe))
                    .path(path)
                    .build();
        }
        
        if (exception instanceof WebApplicationException wae) {
            int status = wae.getResponse().getStatus();
            String message = wae.getMessage() != null ? wae.getMessage() : "Request failed";
            return ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(status)
                    .error(Response.Status.fromStatusCode(status).getReasonPhrase())
                    .message(message)
                    .path(path)
                    .build();
        }
        
        if (exception instanceof AccountAlreadyExistsException) {
            return ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(Response.Status.CONFLICT.getStatusCode())
                    .error("Conflict")
                    .message(exception.getMessage())
                    .path(path)
                    .build();
        }
        
        if (exception instanceof AccountNotFoundException) {
            return ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(Response.Status.NOT_FOUND.getStatusCode())
                    .error("Not Found")
                    .message(exception.getMessage())
                    .path(path)
                    .build();
        }
        
        if (exception instanceof InsufficientBalanceException) {
            return ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(Response.Status.BAD_REQUEST.getStatusCode())
                    .error("Bad Request")
                    .message(exception.getMessage())
                    .path(path)
                    .build();
        }
        
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .path(path)
                .build();
    }
    
    private String extractJsonErrorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message != null && message.length() > 200) {
            return message.substring(0, 200) + "...";
        }
        return message != null ? message : "Invalid JSON syntax";
    }
}

