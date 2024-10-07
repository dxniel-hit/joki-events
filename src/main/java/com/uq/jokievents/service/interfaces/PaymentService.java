package com.uq.jokievents.service.interfaces;

import com.mercadopago.resources.preference.Preference;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface PaymentService {

    ResponseEntity<?> doPayment(String shoppingCartID) throws Exception;
    void receiveMercadopagoNotification(Map<String, Object> request);
}
