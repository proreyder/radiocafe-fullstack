package com.radiocafe.domain.ports.out;
public interface PasswordPort { String codificar(String password); boolean coincide(String raw,String hash); }
