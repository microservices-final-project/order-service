package com.selimhorri.app.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.domain.enums.OrderStatus;
import com.selimhorri.app.exception.wrapper.OrderNotFoundException;
import com.selimhorri.app.service.OrderService;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class OrderResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private OrderDto orderDto;
    private List<OrderDto> orderDtos;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        
        UserDto userDto = UserDto.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();

        CartDto cartDto = CartDto.builder()
                .cartId(1)
                .userDto(userDto)
                .build();

        this.orderDto = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("First order")
                .orderFee(99.99)
                .orderStatus(OrderStatus.CREATED)
                .cartDto(cartDto)
                .build();

        this.orderDtos = Collections.singletonList(this.orderDto);
    }

    @Test
    void testFindAll() throws Exception {
        when(this.orderService.findAll())
                .thenReturn(this.orderDtos);

        this.mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].orderId").value(this.orderDto.getOrderId()))
                .andExpect(jsonPath("$.collection[0].orderDesc").value(this.orderDto.getOrderDesc()))
                .andExpect(jsonPath("$.collection[0].orderStatus").value(this.orderDto.getOrderStatus().name()));
    }

    @Test
    void testFindById() throws Exception {
        when(this.orderService.findById(anyInt()))
                .thenReturn(this.orderDto);

        this.mockMvc.perform(get("/api/orders/{orderId}", this.orderDto.getOrderId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(this.orderDto.getOrderId()))
                .andExpect(jsonPath("$.orderFee").value(this.orderDto.getOrderFee()))
                .andExpect(jsonPath("$.cart.cartId").value(this.orderDto.getCartDto().getCartId()));
    }

    @Test
    void testFindByIdNotFound() throws Exception {
        when(this.orderService.findById(anyInt()))
                .thenThrow(new OrderNotFoundException("Order not found"));

        this.mockMvc.perform(get("/api/orders/{orderId}", 999))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSave() throws Exception {
        when(this.orderService.save(any(OrderDto.class)))
                .thenReturn(this.orderDto);

        this.mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(this.orderDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(this.orderDto.getOrderId()))
                .andExpect(jsonPath("$.orderStatus").value(OrderStatus.CREATED.name()));
    }

    @Test
    void testSaveValidationFailed() throws Exception {
        OrderDto invalidDto = OrderDto.builder().build(); // Invalid DTO

        this.mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateStatus() throws Exception {
        OrderDto updatedOrder = OrderDto.builder()
                .orderId(1)
                .orderStatus(OrderStatus.ORDERED)
                .build();

        when(this.orderService.updateStatus(anyInt()))
                .thenReturn(updatedOrder);

        this.mockMvc.perform(patch("/api/orders/{orderId}/status", this.orderDto.getOrderId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value(OrderStatus.ORDERED.name()));
    }

    @Test
    void testUpdateStatusNotFound() throws Exception {
        when(this.orderService.updateStatus(anyInt()))
                .thenThrow(new OrderNotFoundException("Order not found"));

        this.mockMvc.perform(patch("/api/orders/{orderId}/status", 999))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdate() throws Exception {
        when(this.orderService.update(anyInt(), any(OrderDto.class)))
                .thenReturn(this.orderDto);

        this.mockMvc.perform(put("/api/orders/{orderId}", this.orderDto.getOrderId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(this.orderDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(this.orderDto.getOrderId()));
    }

    @Test
    void testUpdateNotFound() throws Exception {
        when(this.orderService.update(anyInt(), any(OrderDto.class)))
                .thenThrow(new OrderNotFoundException("Order not found"));

        this.mockMvc.perform(put("/api/orders/{orderId}", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(this.orderDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteById() throws Exception {
        doNothing().when(this.orderService).deleteById(anyInt());

        this.mockMvc.perform(delete("/api/orders/{orderId}", this.orderDto.getOrderId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void testDeleteByIdNotFound() throws Exception {
        doThrow(new OrderNotFoundException("Order not found"))
                .when(this.orderService)
                .deleteById(anyInt());

        this.mockMvc.perform(delete("/api/orders/{orderId}", 999))
                .andExpect(status().isNotFound());
    }
}