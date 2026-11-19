package com.radiocafe.domain.ports.in;
import com.radiocafe.domain.model.Pedido;
import java.util.List;
public interface OrderQueryUseCase { List<Pedido> listar(Long usuario,boolean personal); Pedido buscar(Long id); }
