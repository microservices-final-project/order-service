package com.selimhorri.app.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.domain.Order;
import com.selimhorri.app.domain.enums.OrderStatus;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.exception.wrapper.CartNotFoundException;
import com.selimhorri.app.exception.wrapper.OrderNotFoundException;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.repository.OrderRepository;
import com.selimhorri.app.service.impl.OrderServiceImpl;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private OrderDto orderDto;
    private Cart cart;

    @BeforeEach
    void setUp() {
        cart = Cart.builder()
                .cartId(1)
                .isActive(true)
                .build();

        order = Order.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test order")
                .orderFee(100.0)
                .isActive(true)
                .status(OrderStatus.CREATED)
                .cart(cart)
                .build();

        orderDto = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test order")
                .orderFee(100.0)
                .orderStatus(OrderStatus.CREATED)
                .cartDto(CartDto.builder().cartId(1).build())
                .build();
    }

    @Test
    void findAll_ShouldReturnListOfActiveOrders() {
        // Arrange
        when(orderRepository.findAllByIsActiveTrue()).thenReturn(List.of(order));

        // Act
        List<OrderDto> result = orderService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(orderDto.getOrderId(), result.get(0).getOrderId());
        verify(orderRepository, times(1)).findAllByIsActiveTrue();
    }

    @Test
    void findById_ShouldReturnOrderWhenFound() {
        // Arrange
        when(orderRepository.findByOrderIdAndIsActiveTrue(anyInt())).thenReturn(Optional.of(order));

        // Act
        OrderDto result = orderService.findById(1);

        // Assert
        assertNotNull(result);
        assertEquals(orderDto.getOrderId(), result.getOrderId());
        verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(anyInt());
    }

    @Test
    void findById_ShouldThrowExceptionWhenNotFound() {
        // Arrange
        when(orderRepository.findByOrderIdAndIsActiveTrue(anyInt())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(OrderNotFoundException.class, () -> orderService.findById(1));
    }

    @Test
    void save_ShouldSaveOrderWithValidCart() {
        // Arrange
        OrderDto newOrderDto = OrderDto.builder()
                .orderDesc("New order")
                .orderFee(50.0)
                .cartDto(CartDto.builder().cartId(1).build())
                .build();

        when(cartRepository.findById(anyInt())).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        OrderDto result = orderService.save(newOrderDto);

        // Assert
        assertNotNull(result);
        assertEquals(orderDto.getOrderId(), result.getOrderId());
        verify(cartRepository, times(1)).findById(anyInt());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void save_ShouldThrowExceptionWhenCartNotFound() {
        // Arrange
        OrderDto newOrderDto = OrderDto.builder()
                .orderDesc("New order")
                .orderFee(50.0)
                .cartDto(CartDto.builder().cartId(999).build()) // Non-existent cart
                .build();

        when(cartRepository.findById(anyInt())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CartNotFoundException.class, () -> orderService.save(newOrderDto));
    }

    @Test
    void updateStatus_ShouldUpdateFromCreatedToOrdered() {
        // Arrange
        order.setStatus(OrderStatus.CREATED);
        when(orderRepository.findByOrderIdAndIsActiveTrue(anyInt())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        OrderDto result = orderService.updateStatus(1);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.ORDERED, result.getOrderStatus());
    }

    @Test
    void updateStatus_ShouldUpdateFromOrderedToInPayment() {
        // Arrange
        order.setStatus(OrderStatus.ORDERED);
        when(orderRepository.findByOrderIdAndIsActiveTrue(anyInt())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        OrderDto result = orderService.updateStatus(1);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.IN_PAYMENT, result.getOrderStatus());
    }

    @Test
    void updateStatus_ShouldThrowExceptionWhenAlreadyInPayment() {
        // Arrange
        order.setStatus(OrderStatus.IN_PAYMENT);
        when(orderRepository.findByOrderIdAndIsActiveTrue(anyInt())).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> orderService.updateStatus(1));
    }

    @Test
    void update_ShouldPreserveCartAssociation() {
        // Arrange
        OrderDto updatedDto = OrderDto.builder()
                .orderId(1)
                .orderDesc("Updated description")
                .orderFee(150.0)
                .build();

        when(orderRepository.findByOrderIdAndIsActiveTrue(anyInt())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        OrderDto result = orderService.update(1, updatedDto);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getCartDto());
        assertEquals(cart.getCartId(), result.getCartDto().getCartId());
    }

    @Test
    void deleteById_ShouldDeactivateOrder() {
        // Arrange
        order.setStatus(OrderStatus.CREATED);
        when(orderRepository.findByOrderIdAndIsActiveTrue(anyInt())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        orderService.deleteById(1);

        // Assert
        assertFalse(order.isActive());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void deleteById_ShouldThrowExceptionWhenOrderNotFound() {
        // Arrange
        when(orderRepository.findByOrderIdAndIsActiveTrue(anyInt())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(OrderNotFoundException.class, () -> orderService.deleteById(1));
    }

    @Test
    void deleteById_ShouldThrowExceptionWhenOrderIsInPayment() {
        // Arrange
        order.setStatus(OrderStatus.IN_PAYMENT);
        when(orderRepository.findByOrderIdAndIsActiveTrue(anyInt())).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> orderService.deleteById(1));
    }
}