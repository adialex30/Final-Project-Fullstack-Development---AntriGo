package com.antrigo.backend.payment;

import com.antrigo.backend.domain.entity.Order;

public interface MidtransGatewayService {
    QrisChargeResult createQrisCharge(Order order);
}
