package com.ardom.automotive_event_api.common.exception;

import com.ardom.automotive_event_api.application.exception.*;
import com.ardom.automotive_event_api.auth.exception.InvalidCredentialsException;
import com.ardom.automotive_event_api.auth.exception.InvalidGoogleTokenException;
import com.ardom.automotive_event_api.auth.exception.InvalidRefreshTokenException;
import com.ardom.automotive_event_api.auth.exception.UserAlreadyExistsException;
import com.ardom.automotive_event_api.common.dto.response.ErrorResponse;
import com.ardom.automotive_event_api.event.exception.*;
import com.ardom.automotive_event_api.payment.exception.PaymentAlreadyInitiatedException;
import com.ardom.automotive_event_api.payment.exception.WebhookException;
import com.ardom.automotive_event_api.storage.exception.UnsupportedFiletypeException;
import com.ardom.automotive_event_api.ticket.exception.TicketNotFoundException;
import com.ardom.automotive_event_api.user.exception.UserNotFoundException;
import com.stripe.exception.StripeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        return buildResponse(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException e) {
        log.warn("User not found: {}", e.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ErrorResponse> handleMissingCookieException(MissingRequestCookieException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Required cookie is missing: " + e.getCookieName());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handeInvalidCredentialsException(InvalidCredentialsException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshTokenException(InvalidRefreshTokenException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException e) {
        return buildResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(InvalidGoogleTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidGoogleTokenException(InvalidGoogleTokenException e) {
        return buildResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(DeletingNotDraftEventException.class)
    public ResponseEntity<ErrorResponse> handleDeletingNotDraftEventException(DeletingNotDraftEventException e) {
        return buildResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEventNotFoundException(EventNotFoundException e) {
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatusTransitionException(InvalidStatusTransitionException e) {
        return buildResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(EventNotEditableException.class)
    public ResponseEntity<ErrorResponse> handleEventNotEditableException(EventNotEditableException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleApplicationNotFoundException(ApplicationNotFoundException e) {
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ApplicationNotEditableException.class)
    public ResponseEntity<ErrorResponse> handleApplicationNotEditableException(ApplicationNotEditableException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ApplicationNotSubmittableException.class)
    public ResponseEntity<ErrorResponse> handleApplicationNotSubmittableException(ApplicationNotSubmittableException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(TooManyCarsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyCarsException(TooManyCarsException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(InvalidApplicationStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleApplicationNotApprovableException(InvalidApplicationStatusTransitionException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(UserAlreadyHasApplicationException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyHasApplicationException(UserAlreadyHasApplicationException e) {
        return buildResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(UnsupportedFiletypeException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedFiletypeException(UnsupportedFiletypeException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(InvalidPhotoKeyException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPhotoKeyException(InvalidPhotoKeyException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ApplicationDeadlineException.class)
    public ResponseEntity<ErrorResponse> handleApplicationDeadlineException(ApplicationDeadlineException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(PaymentAlreadyInitiatedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentAlreadyInitiatedException(PaymentAlreadyInitiatedException e) {
        return buildResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(StripeException.class)
    public ResponseEntity<ErrorResponse> handleStripeException(StripeException e) {
        return buildResponse(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    @ExceptionHandler(EventSoldOutException.class)
    public ResponseEntity<ErrorResponse> handleEventSoldOutException(EventSoldOutException e) {
        return buildResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(NotEnoughEventTicketsException.class)
    public ResponseEntity<ErrorResponse> handleNotEnoughEventTicketsException(NotEnoughEventTicketsException e) {
        return buildResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ApplicationPaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleApplicationPaymentNotFoundException(ApplicationPaymentNotFoundException e) {
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(TicketPaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTicketPaymentNotFoundException(TicketPaymentNotFoundException e) {
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(WebhookException.class)
    public ResponseEntity<ErrorResponse> handleWebhookException(WebhookException e) {
        return buildResponse(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTicketNotFoundException(TicketNotFoundException e) {
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        log.error("Unexpected error occurred: ", e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected error occurred: ", e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred");
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                message,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, status);
    }
}
