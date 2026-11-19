package com.radiocafe.domain.ports.in;
import com.radiocafe.domain.model.*;
public interface ManageOrderStateUseCase { Pedido cambiarEstadoPedido(Long id,EstadoPedido nuevo,Long actor); }
