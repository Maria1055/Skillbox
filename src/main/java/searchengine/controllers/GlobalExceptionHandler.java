package searchengine.controllers;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import searchengine.dto.common.Response;
import searchengine.exception.BadRequestException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public Response handleBadRequest(BadRequestException ex) {
        return new Response(false, ex.getMessage());
    }
}
