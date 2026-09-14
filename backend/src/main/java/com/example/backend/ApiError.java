package com.example.backend;

public class ApiError extends RuntimeException {
    final int status; final String code;
    public ApiError(int status,String code,String message) { super(message);this.status=status;this.code=code; }
}
