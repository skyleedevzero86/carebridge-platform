package com.sleekydz86.carebridge.backend.server.interfaces.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import com.sleekydz86.carebridge.backend.server.domain.workitem.WorkItemNotFoundException;
import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = fieldError == null ? "?ÖÎ†•Í∞íÏùÑ ?ïÏù∏??Ï£ºÏÑ∏??" : fieldError.getDefaultMessage();
        return ResponseEntity.badRequest().body(new ErrorResponse("?ÖÎ†•Í≤ÄÏ¶ùÏò§Î•?, message, LocalDateTime.now()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return ResponseEntity.status(status)
                .body(new ErrorResponse(String.valueOf(status.value()), exception.getReason(), LocalDateTime.now()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("?òÎ™ª?úÏöîÏ≤?, exception.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(WorkItemNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWorkItemNotFound(WorkItemNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Ï∞æÏùÑ?òÏóÜ??, exception.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandled(Exception exception) {
        log.error("Ï≤òÎ¶¨?òÏ? ?äÏ? ?àÏô∏Í∞Ä Î∞úÏÉù?àÏäµ?àÎã§.", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("?¥Î??úÎ≤Ñ?§Î•ò", "?úÎ≤Ñ Ï≤òÎ¶¨ Ï§?Î¨∏Ï†úÍ∞Ä Î∞úÏÉù?àÏäµ?àÎã§.", LocalDateTime.now()));
    }

    public record ErrorResponse(String code, String message, LocalDateTime timestamp)  {}
}