package com.radiocafe.application.usecases;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.in.OrderQueryUseCase;
import com.radiocafe.domain.ports.out.OrderRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service @Transactional(readOnly=true)
public class OrderQueryUseCaseImpl implements OrderQueryUseCase {
 private final OrderRepositoryPort orders;
 public OrderQueryUseCaseImpl(OrderRepositoryPort orders) {this.orders=orders;}
 public List<Pedido> listar(Long usuario,boolean personal) {return personal?orders.obtenerTodos():orders.buscarPorUsuario(usuario);}
 public Pedido buscar(Long id) {return orders.buscarPorId(id).orElseThrow(()->BusinessException.noEncontrado("Pedido no encontrado."));}
}
