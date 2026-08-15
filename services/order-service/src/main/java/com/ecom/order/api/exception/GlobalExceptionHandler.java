package com.ecom.order.api.exception;

import com.ecom.order.domain.exception.IllegalStateTransitionException;
import com.ecom.order.domain.exception.IdempotencyConflictException;
import com.ecom.order.domain.exception.InvalidOrderException;
import com.ecom.order.domain.exception.OrderNotFoundException;
import com.ecom.order.domain.exception.InventoryUnavailableException;
import com.ecom.order.api.config.RequestBodySizeLimitFilter.PayloadTooLargeIOException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/** Traduz excecoes de dominio em respostas RFC 7807 (ProblemDetail). */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail handleNotFound(OrderNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Pedido nao encontrado");
        pd.setType(URI.create("urn:ecom:order:not-found"));
        return pd;
    }

    @ExceptionHandler(IllegalStateTransitionException.class)
    ProblemDetail handleIllegalTransition(IllegalStateTransitionException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Transicao de estado invalida");
        pd.setType(URI.create("urn:ecom:order:illegal-transition"));
        return pd;
    }

    @ExceptionHandler(InvalidOrderException.class)
    ProblemDetail handleInvalidOrder(InvalidOrderException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Requisicao invalida");
        pd.setType(URI.create("urn:ecom:order:bad-request"));
        return pd;
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ProblemDetail handleIdempotencyConflict(IdempotencyConflictException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Conflito de idempotencia");
        pd.setType(URI.create("urn:ecom:order:idempotency-conflict"));
        return pd;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleConcurrentUpdate(ObjectOptimisticLockingFailureException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "O pedido foi alterado por outra operacao");
        pd.setTitle("Conflito de concorrencia");
        pd.setType(URI.create("urn:ecom:order:concurrent-update"));
        return pd;
    }

    @ExceptionHandler(InventoryUnavailableException.class)
    ProblemDetail handleInventoryUnavailable(InventoryUnavailableException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        pd.setTitle("Estoque temporariamente indisponivel");
        pd.setType(URI.create("urn:ecom:inventory:unavailable"));
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
        if (hasCause(ex, PayloadTooLargeIOException.class)) {
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.PAYLOAD_TOO_LARGE);
            pd.setTitle("Requisicao muito grande");
            pd.setType(URI.create("urn:ecom:request:too-large"));
            return pd;
        }
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "JSON ausente ou malformado");
        pd.setTitle("Requisicao invalida");
        pd.setType(URI.create("urn:ecom:request:malformed-json"));
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Parametros fora dos limites permitidos");
        pd.setTitle("Requisicao invalida");
        pd.setType(URI.create("urn:ecom:request:constraint-violation"));
        return pd;
    }

    private static boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        Throwable current = error;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
