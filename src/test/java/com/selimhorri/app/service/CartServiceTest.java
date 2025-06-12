package com.selimhorri.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.CartNotFoundException;
import com.selimhorri.app.exception.wrapper.UserNotFoundException;
import com.selimhorri.app.helper.CartMappingHelper;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.service.impl.CartServiceImpl;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CartServiceImpl cartService;

    private CartDto cartDto;
    private Cart cart;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        userDto = UserDto.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();

        cartDto = CartDto.builder()
                .cartId(1)
                .userId(1)
                .userDto(userDto)
                .build();

        cart = CartMappingHelper.map(cartDto);
    }

    @Test
    void findById_ShouldReturnCartWhenFound() {
        // Arrange
        when(cartRepository.findByCartIdAndIsActiveTrue(anyInt())).thenReturn(Optional.of(cart));
        when(restTemplate.getForObject(anyString(), eq(UserDto.class))).thenReturn(userDto);

        // Act
        CartDto result = cartService.findById(1);

        // Assert
        assertNotNull(result);
        assertEquals(cartDto.getCartId(), result.getCartId());
        assertNotNull(result.getUserDto());
        verify(cartRepository, times(1)).findByCartIdAndIsActiveTrue(anyInt());
    }

    @Test
    void findById_ShouldThrowExceptionWhenNotFound() {
        // Arrange
        when(cartRepository.findByCartIdAndIsActiveTrue(anyInt())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CartNotFoundException.class, () -> cartService.findById(1));
    }

    @Test
    void save_ShouldSaveCartWithValidUser() {
        // Arrange
        CartDto newCartDto = CartDto.builder()
                .userId(1)
                .build();

        when(restTemplate.getForObject(
                AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/1", 
                UserDto.class))
                .thenReturn(userDto);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        CartDto result = cartService.save(newCartDto);

        // Assert
        assertNotNull(result);
        assertEquals(cartDto.getCartId(), result.getCartId());
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void save_ShouldThrowExceptionWhenUserNotFound() {
        // Arrange
        CartDto newCartDto = CartDto.builder()
                .userId(999) // Non-existent user
                .build();

        when(restTemplate.getForObject(anyString(), eq(UserDto.class)))
                .thenThrow(HttpClientErrorException.NotFound.class);

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> cartService.save(newCartDto));
    }

    @Test
    void save_ShouldThrowExceptionWhenUserIdIsNull() {
        // Arrange
        CartDto newCartDto = CartDto.builder()
                .userId(null)
                .build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> cartService.save(newCartDto));
    }

    @Test
    void deleteById_ShouldSoftDeleteCart() {
        // Arrange
        Cart cartToDelete = Cart.builder()
                .cartId(1)
                .isActive(true)
                .build();

        when(cartRepository.findById(anyInt())).thenReturn(Optional.of(cartToDelete));
        when(cartRepository.save(any(Cart.class))).thenReturn(cartToDelete);

        // Act
        cartService.deleteById(1);

        // Assert
        assertFalse(cartToDelete.isActive());
        verify(cartRepository, times(1)).save(cartToDelete);
    }

    @Test
    void deleteById_ShouldThrowExceptionWhenCartNotFound() {
        // Arrange
        when(cartRepository.findById(anyInt())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CartNotFoundException.class, () -> cartService.deleteById(1));
    }
}