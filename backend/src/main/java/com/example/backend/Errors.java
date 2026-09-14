package com.example.backend;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class Errors {
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception e,HttpServletRequest r){org.slf4j.LoggerFactory.getLogger(Errors.class).atError().addKeyValue("requestId",String.valueOf(r.getAttribute("requestId"))).addKeyValue("exceptionType",e.getClass().getSimpleName()).log("unexpected_application_error");return ResponseEntity.status(500).body(body(500,"INTERNAL_ERROR","The request could not be completed",String.valueOf(r.getAttribute("requestId"))));}
    static Map<String,Object> body(int status,String code,String message,String id) {
        return Map.of("timestamp",Instant.now().toString(),"status",status,"error",code,"message",message,"requestId",id==null?"":id);
    }
    @ExceptionHandler(ApiError.class)
    ResponseEntity<?> api(ApiError e,HttpServletRequest r) { return ResponseEntity.status(e.status).body(body(e.status,e.code,e.getMessage(),String.valueOf(r.getAttribute("requestId")))); }
    @ExceptionHandler({MethodArgumentNotValidException.class,org.springframework.http.converter.HttpMessageNotReadableException.class,IllegalArgumentException.class})
    ResponseEntity<?> invalid(Exception e,HttpServletRequest r) { return ResponseEntity.badRequest().body(body(400,"INVALID_INPUT","Check the supplied fields",String.valueOf(r.getAttribute("requestId")))); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict(Exception e,HttpServletRequest r) { return ResponseEntity.status(409).body(body(409,"CONFLICT","Duplicate value or resource still in use",String.valueOf(r.getAttribute("requestId")))); }
}
