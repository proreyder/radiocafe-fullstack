package com.radiocafe.domain.model;

import java.time.Instant;

public record ImagenProducto(String nombre, String url, long bytes, Instant fecha) {
    public static final String FILE_PATTERN = "[a-f0-9-]{36}\\.(png|jpg)";
}
