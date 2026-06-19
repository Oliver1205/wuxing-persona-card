package com.wuxing.persona.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusinessExceptionShouldPreserveServerErrorStatus() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleBusinessException(new BusinessException(500, "external short link binding failed"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getCode());
    }

    @Test
    void handleBusinessExceptionShouldKeepValidationErrorsAsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleBusinessException(new BusinessException(422, "invalid state"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(422, response.getBody().getCode());
    }

    @Test
    void handleBusinessExceptionShouldPreserveCommonApiStatusCodes() {
        assertEquals(HttpStatus.FORBIDDEN,
                handler.handleBusinessException(new BusinessException(403, "forbidden")).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,
                handler.handleBusinessException(new BusinessException(409, "conflict")).getStatusCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS,
                handler.handleBusinessException(new BusinessException(429, "too many requests")).getStatusCode());
        assertEquals(HttpStatus.BAD_GATEWAY,
                handler.handleBusinessException(new BusinessException(502, "bad gateway")).getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                handler.handleBusinessException(new BusinessException(503, "service unavailable")).getStatusCode());
    }

    @Test
    void handleNoResourceFoundShouldReturnNotFoundWithoutServerError() {
        ApiResponse<Void> response =
                handler.handleNoResourceFound(new NoResourceFoundException(HttpMethod.GET, "/admin-api/unknown"));

        assertEquals(404, response.getCode());
        assertEquals("not found", response.getMessage());
    }
}
