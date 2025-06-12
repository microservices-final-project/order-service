package com.selimhorri.app.resource;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

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
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.exception.ApiExceptionHandler;
import com.selimhorri.app.exception.wrapper.CartNotFoundException;
import com.selimhorri.app.service.CartService;

@ExtendWith(MockitoExtension.class)
class CartResourceTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartResource cartResource;

    private CartDto cartDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cartResource)
                .setControllerAdvice(new ApiExceptionHandler()) // Añade tu manejador de excepciones global
                .build();
        objectMapper = new ObjectMapper();

        cartDto = CartDto.builder()
                .cartId(1)
                .userId(1)
                .build();
    }

    @Test
    void findAll_ShouldReturnAllCarts() throws Exception {
        // Arrange
        when(cartService.findAll()).thenReturn(List.of(cartDto));

        // Act & Assert
        mockMvc.perform(get("/api/carts")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].cartId").value(1))
                .andDo(print());

        verify(cartService, times(1)).findAll();
    }

    @Test
    void findById_ShouldReturnCartWhenExists() throws Exception {
        // Arrange
        when(cartService.findById(anyInt())).thenReturn(cartDto);

        // Act & Assert
        mockMvc.perform(get("/api/carts/{cartId}", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(1))
                .andDo(print());

        verify(cartService, times(1)).findById(anyInt());
    }

    @Test
    void findById_ShouldReturnNotFoundWhenCartNotExists() throws Exception {
        // Arrange
        when(cartService.findById(anyInt())).thenThrow(new CartNotFoundException("Not found"));

        // Act & Assert
        mockMvc.perform(get("/api/carts/{cartId}", "999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andDo(print());

        verify(cartService, times(1)).findById(anyInt());
    }


    @Test
    void save_ShouldSaveCart() throws Exception {
        // Arrange
        when(cartService.save(any(CartDto.class))).thenReturn(cartDto);

        // Act & Assert
        mockMvc.perform(post("/api/carts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cartDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(1))
                .andDo(print());

        verify(cartService, times(1)).save(any(CartDto.class));
    }

    @Test
    void save_ShouldReturnBadRequestWhenInputIsNull() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/carts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("null"))
                .andExpect(status().isBadRequest())
                .andDo(print());

        verify(cartService, never()).save(any(CartDto.class));
    }

    @Test
    void deleteById_ShouldDeleteCart() throws Exception {
        // Arrange
        doNothing().when(cartService).deleteById(anyInt());

        // Act & Assert
        mockMvc.perform(delete("/api/carts/{cartId}", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true))
                .andDo(print());

        verify(cartService, times(1)).deleteById(anyInt());
    }

    @Test
    void deleteById_ShouldReturnNotFoundWhenCartNotExists() throws Exception {
        // Arrange
        doThrow(new CartNotFoundException("Not found")).when(cartService).deleteById(anyInt());

        // Act & Assert
        mockMvc.perform(delete("/api/carts/{cartId}", "999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andDo(print());

        verify(cartService, times(1)).deleteById(anyInt());
    }
}