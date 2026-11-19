package com.radiocafe.domain.model;
public class BusinessException extends RuntimeException {
 public enum Tipo { NO_ENCONTRADO, CONFLICTO, INVALIDO, PROHIBIDO }
 private final Tipo tipo;
 public BusinessException(Tipo tipo, String mensaje) { super(mensaje); this.tipo=tipo; }
 public Tipo getTipo() { return tipo; }
 public static BusinessException noEncontrado(String mensaje) { return new BusinessException(Tipo.NO_ENCONTRADO,mensaje); }
 public static BusinessException conflicto(String mensaje) { return new BusinessException(Tipo.CONFLICTO,mensaje); }
}
