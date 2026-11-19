package com.radiocafe.infrastructure.adapters.in.web;
import com.radiocafe.domain.model.BusinessException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.*;
import java.util.Map;
@RestControllerAdvice
public class ApiExceptionHandler {
 private ResponseEntity<Map<String,String>> error(HttpStatus status,String message) {return ResponseEntity.status(status).body(Map.of("message",message));}
 @ExceptionHandler(BusinessException.class) ResponseEntity<Map<String,String>> business(BusinessException e) {return error(switch(e.getTipo()){case NO_ENCONTRADO->HttpStatus.NOT_FOUND;case CONFLICTO->HttpStatus.CONFLICT;case INVALIDO->HttpStatus.BAD_REQUEST;case PROHIBIDO->HttpStatus.FORBIDDEN;},e.getMessage());}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<Map<String,String>> validation(MethodArgumentNotValidException e) {return error(HttpStatus.BAD_REQUEST,e.getBindingResult().getFieldErrors().stream().map(x->x.getField()+": "+x.getDefaultMessage()).findFirst().orElse("Datos inválidos."));}
 @ExceptionHandler({IllegalArgumentException.class,HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class}) ResponseEntity<Map<String,String>> invalid(Exception e) {return error(HttpStatus.BAD_REQUEST,"Revisa los datos enviados y los valores permitidos.");}
 @ExceptionHandler(IllegalStateException.class) ResponseEntity<Map<String,String>> state(IllegalStateException e) {return error(HttpStatus.CONFLICT,e.getMessage());}
 @ExceptionHandler(AuthenticationException.class) ResponseEntity<Map<String,String>> auth(AuthenticationException e) {return error(HttpStatus.UNAUTHORIZED,"Correo o contraseña incorrectos.");}
 @ExceptionHandler(AccessDeniedException.class) ResponseEntity<Map<String,String>> denied(AccessDeniedException e) {return error(HttpStatus.FORBIDDEN,"No tienes permiso para esta operación.");}
 @ExceptionHandler({DataIntegrityViolationException.class,OptimisticLockingFailureException.class,PessimisticLockingFailureException.class}) ResponseEntity<Map<String,String>> conflict(Exception e) {return error(HttpStatus.CONFLICT,"La operación entra en conflicto con datos existentes. Actualiza e inténtalo nuevamente.");}
}
