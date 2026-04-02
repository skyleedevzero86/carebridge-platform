package com.sleekydz86.carebridge.backend.server.domain.workitem;

public class WorkItemNotFoundException extends RuntimeException {
    public WorkItemNotFoundException(String message) {
        super(message);
    }
}