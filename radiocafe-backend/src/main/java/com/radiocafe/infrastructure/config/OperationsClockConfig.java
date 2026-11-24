package com.radiocafe.infrastructure.config;
import org.springframework.context.annotation.*;
import java.time.*;
@Configuration
public class OperationsClockConfig { @Bean public Clock operationsClock() {return Clock.system(ZoneId.of("America/Lima"));} }
