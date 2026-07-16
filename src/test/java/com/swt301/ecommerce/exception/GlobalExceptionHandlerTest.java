package com.swt301.ecommerce.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();


    @Test void validationErrorsAreReturnedByFieldName() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult binding = mock(BindingResult.class);
        when(exception.getBindingResult()).thenReturn(binding);
        when(binding.getFieldErrors()).thenReturn(java.util.List.of(
                new FieldError("request", "phone", "invalid phone"),
                new FieldError("request", "email", "invalid email")));
        var response = handler.handleValidationExceptions(exception);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrors()).containsEntry("phone", "invalid phone")
                .containsEntry("email", "invalid email");
    }

    @Test void mapsExpectedStatusCodes() {
        assertStatus(handler.handleBadRequest(new BadRequestException("bad")), HttpStatus.BAD_REQUEST);
        assertStatus(handler.handleAuthentication(new BadCredentialsException("bad")), HttpStatus.UNAUTHORIZED);
        assertStatus(handler.handleAccessDenied(new AccessDeniedException("no")), HttpStatus.FORBIDDEN);
        assertStatus(handler.handleNotFound(new ResourceNotFoundException("missing")), HttpStatus.NOT_FOUND);
        assertStatus(handler.handleConflict(new ConflictException("duplicate")), HttpStatus.CONFLICT);
        assertStatus(handler.handleBusinessRule(new BusinessRuleException("rule")), HttpStatus.UNPROCESSABLE_ENTITY);
        assertStatus(handler.handlePayloadTooLarge(new PayloadTooLargeException("large")), HttpStatus.PAYLOAD_TOO_LARGE);
        assertStatus(handler.handleDataIntegrity(new DataIntegrityViolationException("constraint")), HttpStatus.CONFLICT);
        assertStatus(handler.handleExternalService(new ExternalServiceException("cloud", null)), HttpStatus.BAD_GATEWAY);
        assertStatus(handler.handleConfigurationFailure(new IllegalStateException("config")), HttpStatus.INTERNAL_SERVER_ERROR);
        assertStatus(handler.handleUnexpected(new RuntimeException("boom")), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test void unexpectedErrorDoesNotExposeInternalMessage() {
        var response = handler.handleUnexpected(new RuntimeException("database password is secret"));
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).doesNotContain("password");
    }

    private void assertStatus(org.springframework.http.ResponseEntity<ErrorResponse> response, HttpStatus expected) {
        assertThat(response.getStatusCode()).isEqualTo(expected);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(expected.value());
    }
}
