package com.radiocafe;
import com.radiocafe.domain.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
class OrderRulesTest {
 private Pedido pedido(String precio,TipoEntrega tipo) {var p=new Pedido();p.setTipoEntrega(tipo);p.agregarDetalle(new DetallePedido(1L,"Café",1,new BigDecimal(precio)));return p;}
 @Test void deliveryBelowThresholdCostsFive() {var p=pedido("49.99",TipoEntrega.DELIVERY);assertThat(p.getCostoEnvio()).isEqualByComparingTo("5.00");assertThat(p.getTotal()).isEqualByComparingTo("54.99");}
 @Test void deliveryAtThresholdIsFree() {var p=pedido("50.00",TipoEntrega.DELIVERY);assertThat(p.getCostoEnvio()).isEqualByComparingTo("0");assertThat(p.getTotal()).isEqualByComparingTo("50.00");}
 @Test void pickupNeverHasDeliveryFee() {assertThat(pedido("1.00",TipoEntrega.PICKUP).getCostoEnvio()).isEqualByComparingTo("0");}
 @Test void deliveryMustPassThroughEnCamino() {var p=pedido("2",TipoEntrega.DELIVERY);p.cambiarEstado(EstadoPedido.EN_PREPARACION);p.cambiarEstado(EstadoPedido.LISTO);assertThatThrownBy(()->p.cambiarEstado(EstadoPedido.ENTREGADO)).isInstanceOf(BusinessException.class);p.cambiarEstado(EstadoPedido.EN_CAMINO);p.cambiarEstado(EstadoPedido.ENTREGADO);assertThatThrownBy(()->p.cambiarEstado(EstadoPedido.PENDIENTE)).isInstanceOf(BusinessException.class);}
 @Test void cancelledOrderIsTerminal() {var p=pedido("2",TipoEntrega.PICKUP);p.cambiarEstado(EstadoPedido.CANCELLED);assertThatThrownBy(()->p.cambiarEstado(EstadoPedido.EN_PREPARACION)).isInstanceOf(BusinessException.class);}
 @Test void prizeCanOnlyBeRedeemedOnce() {var i=new IlustracionKappu();assertThatThrownBy(i::canjearPremio).isInstanceOf(IllegalStateException.class);i.setEsGanador(true);i.setEstadoPremio(EstadoPremio.PENDIENTE_RECOJO);i.canjearPremio();assertThat(i.getFechaCanje()).isNotNull();assertThatThrownBy(i::canjearPremio).isInstanceOf(IllegalStateException.class);}
}
