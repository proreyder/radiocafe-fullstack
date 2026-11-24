package com.radiocafe.domain.ports.out;
import com.radiocafe.domain.model.Comercio.*;
import java.util.*;
public interface CommerceRepositoryPort {
 List<Campana> campanas(); Optional<Campana> campana(Long id); Campana guardar(Campana c);
 List<Opinion> opiniones(Long producto,boolean todas); Optional<Opinion> opinion(Long id); boolean yaOpino(Long producto,Long usuario); Opinion guardar(Opinion o);
 List<Consulta> consultas(Long usuario); Optional<Consulta> consulta(Long id); Consulta guardar(Consulta c);
 Tienda tienda(); Tienda guardar(Tienda t);
}
