package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.CartNotFoundException;
import com.selimhorri.app.exception.wrapper.UserNotFoundException;
import com.selimhorri.app.helper.CartMappingHelper;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.service.CartService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

	private final CartRepository cartRepository;
	@LoadBalanced
	private final RestTemplate restTemplate;

	@Override
	public List<CartDto> findAll() {
		log.info("*** CartDto List, service; fetch all carts *");
		return this.cartRepository.findAll()
				.stream()
				.map(CartMappingHelper::map)
				.map(c -> {
					c.setUserDto(this.restTemplate.getForObject(
							AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + c.getUserDto().getUserId(),
							UserDto.class));
					return c;
				})
				.distinct()
				.collect(Collectors.toUnmodifiableList());
	}

	@Override
	public CartDto findById(final Integer cartId) {
		log.info("*** CartDto, service; fetch cart by id *");
		return this.cartRepository.findById(cartId)
				.map(CartMappingHelper::map)
				.map(c -> {
					c.setUserDto(this.restTemplate.getForObject(
							AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + c.getUserDto().getUserId(),
							UserDto.class));
					return c;
				})
				.orElseThrow(() -> new CartNotFoundException(String
						.format("Cart with id: %d not found", cartId)));
	}

	@Override
	public CartDto save(final CartDto cartDto) {
		log.info("*** CartDto, service; save cart *");

		if (cartDto.getUserId() == null) {
			throw new IllegalArgumentException("UserId must not be null when saving a cart");
		}

		try {
			final String url = AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + cartDto.getUserId();
			UserDto userDto = this.restTemplate.getForObject(url, UserDto.class);

			if (userDto == null) {
				throw new UserNotFoundException(String.format("User with id %d not found", cartDto.getUserId()));
			}

			cartDto.setUserDto(userDto);
		} catch (HttpClientErrorException.NotFound ex) {
			throw new UserNotFoundException(String.format("User with id %d not found", cartDto.getUserId()));
		} catch (RestClientException ex) {
			throw new RuntimeException("Error verifying user existence: " + ex.getMessage(), ex);
		}

		cartDto.setCartId(null);
		cartDto.setOrderDtos(null);
		return CartMappingHelper.map(this.cartRepository.save(CartMappingHelper.map(cartDto)));
	}

	@Override
	public void deleteById(final Integer cartId) {
		log.info("*** Void, service; delete cart by id *");
		this.cartRepository.deleteById(cartId);
	}

}
