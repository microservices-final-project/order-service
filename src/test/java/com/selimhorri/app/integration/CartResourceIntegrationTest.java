package com.selimhorri.app.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.UserNotFoundException;
import com.selimhorri.app.helper.CartMappingHelper;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.service.impl.CartServiceImpl;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CartServiceImpl cartService;

    private CartDto cartDto;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        userDto = UserDto.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .build();

        cartDto = CartDto.builder()
                .userId(1)
                .userDto(userDto)
                .build();
    }

    @Test
    void testSave_Success() {
        // Configurar mock para RestTemplate
        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/1"), 
                eq(UserDto.class)))
            .thenReturn(userDto);

        // Configurar mock para Repository
        when(cartRepository.save(any(Cart.class)))
            .thenReturn(CartMappingHelper.map(cartDto));

        CartDto result = cartService.save(cartDto);

        assertNotNull(result);
        assertEquals(1, result.getUserId());
        verify(restTemplate).getForObject(anyString(), eq(UserDto.class));
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void testSave_UserNotFound() {
        // Configurar mock para RestTemplate que lance excepción
        when(restTemplate.getForObject(anyString(), eq(UserDto.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThrows(UserNotFoundException.class, () -> {
            cartService.save(cartDto);
        });

        verify(restTemplate).getForObject(anyString(), eq(UserDto.class));
        verifyNoInteractions(cartRepository);
    }

    @Test
    void testFindById_Success() {
        // Configurar mock para Repository
        Cart cart = new Cart();
        cart.setCartId(1);
        cart.setUserId(1);
        when(cartRepository.findByCartIdAndIsActiveTrue(1))
            .thenReturn(java.util.Optional.of(cart));

        // Configurar mock para RestTemplate
        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/1"), 
                eq(UserDto.class)))
            .thenReturn(userDto);

        CartDto result = cartService.findById(1);

        assertNotNull(result);
        assertEquals(1, result.getCartId());
        assertEquals(1, result.getUserId());
        assertNotNull(result.getUserDto());
    }
}