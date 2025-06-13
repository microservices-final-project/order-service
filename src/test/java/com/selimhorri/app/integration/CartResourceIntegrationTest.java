package com.selimhorri.app.integration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.CartNotFoundException;
import com.selimhorri.app.exception.wrapper.UserNotFoundException;
import com.selimhorri.app.service.CartService;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class CartResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @MockBean
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldFetchAllCarts() throws Exception {
        // Mock data
        UserDto userDto = UserDto.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();

        CartDto cartDto1 = CartDto.builder()
                .cartId(1)
                .userDto(userDto)
                .build();

        CartDto cartDto2 = CartDto.builder()
                .cartId(2)
                .userDto(userDto)
                .build();

        List<CartDto> cartDtos = List.of(cartDto1, cartDto2);

        // Mock service call
        when(cartService.findAll()).thenReturn(cartDtos);

        // Perform request and verify
        mockMvc.perform(get("/api/carts")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection.length()").value(2))
                .andExpect(jsonPath("$.collection[0].cartId").value(1))
                .andExpect(jsonPath("$.collection[1].cartId").value(2));

        verify(cartService, times(1)).findAll();
    }

    @Test
    void shouldFetchCartById() throws Exception {
        // Mock data
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

        // Mock service call
        when(cartService.findById(anyInt())).thenReturn(cartDto);

        // Perform request and verify
        mockMvc.perform(get("/api/carts/{cartId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(1))
                .andExpect(jsonPath("$.user.userId").value(1));

        verify(cartService, times(1)).findById(1);
    }

    @Test
    void shouldReturnNotFoundWhenCartDoesNotExist() throws Exception {
        // Mock service to throw exception
        when(cartService.findById(anyInt()))
                .thenThrow(new CartNotFoundException("Cart with id: 999 not found"));

        // Perform request and verify
        mockMvc.perform(get("/api/carts/{cartId}", 999)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(cartService, times(1)).findById(999);
    }

    @Test
    void shouldSaveCart() throws Exception {
        // Mock data
        UserDto userDto = UserDto.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();

        CartDto inputCartDto = CartDto.builder()
                .userId(1)
                .build();

        CartDto savedCartDto = CartDto.builder()
                .cartId(1)
                .userDto(userDto)
                .build();

        // Mock service calls
        when(cartService.save(any(CartDto.class))).thenReturn(savedCartDto);
        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/1"), 
                eq(UserDto.class)))
                .thenReturn(userDto);

        // Perform request and verify
        mockMvc.perform(post("/api/carts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputCartDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(1))
                .andExpect(jsonPath("$.user.userId").value(1));

        verify(cartService, times(1)).save(any(CartDto.class));
    }

    @Test
    void shouldDeleteCart() throws Exception {
        // Mock service (deleteById is void)
        doNothing().when(cartService).deleteById(anyInt());

        // Perform request and verify
        mockMvc.perform(delete("/api/carts/{cartId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(cartService, times(1)).deleteById(1);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistingCart() throws Exception {
        // Mock service to throw exception
        doThrow(new CartNotFoundException("Cart with id: 999 not found"))
                .when(cartService).deleteById(999);

        // Perform request and verify
        mockMvc.perform(delete("/api/carts/{cartId}", 999)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(cartService, times(1)).deleteById(999);
    }

    // Test adicional para verificar la validación
    @Test
    void shouldReturnBadRequestWhenCartIdIsBlank() throws Exception {
        mockMvc.perform(get("/api/carts/{cartId}", " ")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).findById(anyInt());
    }

    @Test
    void shouldReturnBadRequestWhenSaveWithNullCart() throws Exception {
        mockMvc.perform(post("/api/carts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("null"))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).save(any());
    }
}