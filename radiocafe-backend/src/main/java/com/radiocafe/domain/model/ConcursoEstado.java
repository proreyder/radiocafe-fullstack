package com.radiocafe.domain.model;
import java.time.LocalDateTime;import java.util.List;
public record ConcursoEstado(LocalDateTime inicio,LocalDateTime fin,boolean cerrado,Long idGanador,String fase,List<Long> finalistas) {}
