// Vị trí: src/main/java/com/swt301/ecommerce/service/AddressService.java
package com.swt301.ecommerce.service;

import com.swt301.ecommerce.dto.request.AddressRequest;
import com.swt301.ecommerce.entity.Address;
import java.util.List;

public interface AddressService {
    List<Address> getUserAddresses(Integer userId);
    Address createAddress(Integer userId, AddressRequest request);
    Address updateAddress(Integer addressId, Integer userId, AddressRequest request);
}