package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.selimhorri.app.domain.Order;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.exception.wrapper.CartNotFoundException;
import com.selimhorri.app.exception.wrapper.OrderNotFoundException;
import com.selimhorri.app.helper.OrderMappingHelper;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.repository.OrderRepository;
import com.selimhorri.app.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

        private final OrderRepository orderRepository;
        private final CartRepository cartRepository;

        @Override
        public List<OrderDto> findAll() {
                log.info("*** OrderDto List, service; fetch all orders *");
                return this.orderRepository.findAll()
                                .stream()
                                .map(OrderMappingHelper::map)
                                .distinct()
                                .collect(Collectors.toUnmodifiableList());
        }

        @Override
        public OrderDto findById(final Integer orderId) {
                log.info("*** OrderDto, service; fetch order by id *");
                return this.orderRepository.findById(orderId)
                                .map(OrderMappingHelper::map)
                                .orElseThrow(() -> new OrderNotFoundException(String
                                                .format("Order with id: %d not found", orderId)));
        }

        @Override
        public OrderDto save(final OrderDto orderDto) {
                log.info("*** OrderDto, service; save order *");
                orderDto.setOrderId(null);

                // Service-level validation
                if (orderDto.getCartDto() == null || orderDto.getCartDto().getCartId() == null) {
                        log.error("Order must be associated with a cart");
                        throw new IllegalArgumentException("Order must be associated with a cart");
                }

                // Check if cart exists
                cartRepository.findById(orderDto.getCartDto().getCartId())
                                .orElseThrow(() -> {
                                        log.error("Cart not found with ID: {}", orderDto.getCartDto().getCartId());
                                        return new CartNotFoundException(
                                                        "Cart not found with ID: " + orderDto.getCartDto().getCartId());
                                });

                // Proceed with saving if validations pass
                return OrderMappingHelper.map(
                                this.orderRepository.save(OrderMappingHelper.map(orderDto)));
        }

        @Override
        public OrderDto update(final OrderDto orderDto) {
                log.info("*** OrderDto, service; update order *");

                try {
                        Order existingOrder = this.orderRepository.findById(orderDto.getOrderId())
                                        .orElseThrow(() -> new OrderNotFoundException(
                                                        "Order not found with ID: " + orderDto.getOrderId()));

                        log.info("Existing order fetched successfully");

                        Order updatedOrder = OrderMappingHelper.mapForUpdate(orderDto, existingOrder.getCart());
                        updatedOrder.setOrderDate(existingOrder.getOrderDate());

                        log.info("Order mapped successfully");

                        // Test if the issue occurs during save
                        Order savedOrder = this.orderRepository.save(updatedOrder);

                        log.info("Order saved successfully");

                        return OrderMappingHelper.map(savedOrder);

                } catch (Exception e) {
                        log.error("Error during order update: ", e);
                        throw e;
                }
        }

        @Override
        public OrderDto update(final Integer orderId, final OrderDto orderDto) {
                log.info("*** OrderDto, service; update order with orderId *");

                // Get existing order to preserve cart association
                Order existingOrder = this.orderRepository.findById(orderId)
                                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
                orderDto.setOrderId(orderId);
                // Map the updates but preserve the cart from existing order
                Order updatedOrder = OrderMappingHelper.mapForUpdate(orderDto, existingOrder.getCart());
                updatedOrder.setOrderDate(existingOrder.getOrderDate());
                return OrderMappingHelper.map(this.orderRepository.save(updatedOrder));
        }

        @Override
        public void deleteById(final Integer orderId) {
                log.info("*** Void, service; delete order by id *");
                this.orderRepository.delete(OrderMappingHelper.map(this.findById(orderId)));
        }

}
