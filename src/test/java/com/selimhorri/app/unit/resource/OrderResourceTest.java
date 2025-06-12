package com.selimhorri.app.unit.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.selimhorri.app.domain.enums.OrderStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.exception.ApiExceptionHandler;
import com.selimhorri.app.resource.OrderResource;
import com.selimhorri.app.service.OrderService;

@ExtendWith(MockitoExtension.class)
class OrderResourceTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderResource orderResource;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private OrderDto orderDto;
    private List<OrderDto> orderList;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderResource).setControllerAdvice(new ApiExceptionHandler()) // Añade
                                                                                                                // tu
                                                                                                                // manejador
                                                                                                                // de
                                                                                                                // excepciones
                                                                                                                // global
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Crear datos de prueba
        orderDto = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test Order")
                .orderFee(100.0)
                .orderStatus(OrderStatus.CREATED)
                .build();

        OrderDto orderDto2 = OrderDto.builder()
                .orderId(2)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test Order 2")
                .orderFee(200.0)
                .orderStatus(OrderStatus.ORDERED)
                .build();

        orderList = Arrays.asList(orderDto, orderDto2);
    }

    @Test
    void findAll_ShouldReturnAllOrders() throws Exception {
        // Given
        when(orderService.findAll()).thenReturn(orderList);

        // When & Then
        mockMvc.perform(get("/api/orders")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection.length()").value(2))
                .andExpect(jsonPath("$.collection[0].orderId").value(1))
                .andExpect(jsonPath("$.collection[0].orderDesc").value("Test Order"))
                .andExpect(jsonPath("$.collection[1].orderId").value(2))
                .andExpect(jsonPath("$.collection[1].orderDesc").value("Test Order 2"));

        verify(orderService, times(1)).findAll();
    }

    @Test
    void findById_WithValidId_ShouldReturnOrder() throws Exception {
        // Given
        when(orderService.findById(1)).thenReturn(orderDto);

        // When & Then
        mockMvc.perform(get("/api/orders/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.orderDesc").value("Test Order"))
                .andExpect(jsonPath("$.orderFee").value(100.0))
                .andExpect(jsonPath("$.orderStatus").value("CREATED"));

        verify(orderService, times(1)).findById(1);
    }

    @Test
    void save_WithValidOrder_ShouldCreateOrder() throws Exception {
        // Given
        OrderDto newOrder = OrderDto.builder()
                .orderDesc("New Order")
                .orderFee(150.0)
                .orderStatus(OrderStatus.CREATED)
                .build();

        OrderDto savedOrder = OrderDto.builder()
                .orderId(3)
                .orderDesc("New Order")
                .orderFee(150.0)
                .orderStatus(OrderStatus.CREATED)
                .build();

        when(orderService.save(any(OrderDto.class))).thenReturn(savedOrder);

        // When & Then
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newOrder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(3))
                .andExpect(jsonPath("$.orderDesc").value("New Order"))
                .andExpect(jsonPath("$.orderFee").value(150.0));

        verify(orderService, times(1)).save(any(OrderDto.class));
    }

    @Test
    void save_WithNullOrder_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("null"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_WithValidId_ShouldUpdateOrderStatus() throws Exception {
        // Given
        OrderDto updatedOrder = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test Order")
                .orderFee(100.0)
                .orderStatus(OrderStatus.ORDERED)
                .build();

        when(orderService.updateStatus(1)).thenReturn(updatedOrder);

        // When & Then
        mockMvc.perform(patch("/api/orders/1/status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.orderStatus").value("ORDERED"));

        verify(orderService, times(1)).updateStatus(1);
    }

    @Test
    void update_WithValidIdAndOrder_ShouldUpdateOrder() throws Exception {
        // Given
        OrderDto updateRequest = OrderDto.builder()
                .orderDesc("Updated Order")
                .orderFee(250.0)
                .orderStatus(OrderStatus.IN_PAYMENT)
                .build();

        OrderDto updatedOrder = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Updated Order")
                .orderFee(250.0)
                .orderStatus(OrderStatus.IN_PAYMENT)
                .build();

        when(orderService.update(anyInt(), any(OrderDto.class))).thenReturn(updatedOrder);

        // When & Then
        mockMvc.perform(put("/api/orders/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.orderDesc").value("Updated Order"))
                .andExpect(jsonPath("$.orderFee").value(250.0))
                .andExpect(jsonPath("$.orderStatus").value("IN_PAYMENT"));

        verify(orderService, times(1)).update(1, updateRequest);
    }
    
    @Test
    void update_WithNullOrder_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(put("/api/orders/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("null"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteById_WithValidId_ShouldDeleteOrder() throws Exception {
        // Given
        doNothing().when(orderService).deleteById(1);

        // When & Then
        mockMvc.perform(delete("/api/orders/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(orderService, times(1)).deleteById(1);
    }

    @Test
    void deleteById_WithValidStringId_ShouldDeleteOrder() throws Exception {
        // Given
        doNothing().when(orderService).deleteById(123);

        // When & Then
        mockMvc.perform(delete("/api/orders/123")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(orderService, times(1)).deleteById(123);
    }
}