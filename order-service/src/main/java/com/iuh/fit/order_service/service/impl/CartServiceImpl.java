package com.iuh.fit.order_service.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iuh.fit.order_service.dto.AddCartItemRequest;
import com.iuh.fit.order_service.dto.CartDTO;
import com.iuh.fit.order_service.dto.CartItemDTO;
import com.iuh.fit.order_service.dto.ProductDetailDTO;
import com.iuh.fit.order_service.dto.ProductServiceResponse;
import com.iuh.fit.order_service.dto.UpdateCartItemRequest;
import com.iuh.fit.order_service.entity.Cart;
import com.iuh.fit.order_service.entity.CartItem;
import com.iuh.fit.order_service.exception.ResourceNotFoundException;
import com.iuh.fit.order_service.exception.ServiceUnavailableException;
import com.iuh.fit.order_service.mapper.CartMapper;
import com.iuh.fit.order_service.repository.CartItemRepository;
import com.iuh.fit.order_service.repository.CartRepository;
import com.iuh.fit.order_service.service.CartService;
import com.iuh.fit.order_service.service.ProductServiceClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;
    private final ProductServiceClient productServiceClient;

    // Phương thức helper để lấy chi tiết sản phẩm từ product-service
    private ProductDetailDTO fetchProductDetails(Long productId) {
        try {
            log.info("Gọi product-service để lấy thông tin sản phẩm ID: {} cho việc làm giàu DTO", productId);
            ProductServiceResponse<ProductDetailDTO> response = productServiceClient.getProductById(productId);
            if (response != null && response.getData() != null && response.getStatusCode() < 400) {
                return response.getData();
            }
            log.warn("Không lấy được thông tin chi tiết cho sản phẩm ID: {} từ product-service. Response: {}", productId, response);
        } catch (Exception e) {
            log.error("Lỗi khi gọi product-service để lấy thông tin sản phẩm ID: {}: {}. Sẽ bỏ qua việc làm giàu tên/ảnh.", productId, e.getMessage());
        }
        return null; 
    }

    // Phương thức helper để xây dựng CartDTO đã được làm giàu
    private CartDTO buildEnrichedCartDTO(Cart cart) {
        CartDTO cartDTO = new CartDTO();
        cartDTO.setId(cart.getId());
        cartDTO.setUserId(cart.getUserId());
        cartDTO.setCreatedAt(cart.getCreatedAt());
        cartDTO.setUpdatedAt(cart.getUpdatedAt());

        List<CartItemDTO> itemDTOs = new ArrayList<>();
        if (cart.getCartItems() != null) {
            for (CartItem cartItem : cart.getCartItems()) {
                ProductDetailDTO productDetail = fetchProductDetails(cartItem.getProductId());

                CartItemDTO itemDTO = new CartItemDTO();
                itemDTO.setId(cartItem.getId());
                itemDTO.setProductId(cartItem.getProductId());
                itemDTO.setQuantity(cartItem.getQuantity());
                itemDTO.setPrice(cartItem.getPrice()); 

                if (productDetail != null) {
                    itemDTO.setProductName(productDetail.getName());
                    itemDTO.setProductImage(productDetail.getImage());
                } else {
                    // Giữ nguyên giá trị null nếu không lấy được, hoặc đặt giá trị mặc định
                    itemDTO.setProductName("N/A"); // Hoặc null
                    itemDTO.setProductImage(null);
                }
                itemDTOs.add(itemDTO);
            }
        }
        cartDTO.setItems(itemDTOs);
        // cartDTO.setTotalAmount(); // TotalAmount được tính trong CartDTO getter
        return cartDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public CartDTO getCartByUserId(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return buildEnrichedCartDTO(cart);
    }

    @Override
    @Transactional
    public CartDTO addItemToCart(AddCartItemRequest request) {
        Cart cart = getOrCreateCart(request.getUserId());

        ProductDetailDTO productDetailFromService = fetchProductDetails(request.getProductId());
        
        if (productDetailFromService == null || productDetailFromService.getPrice() == null) {
            log.error("Không thể lấy thông tin giá hợp lệ cho sản phẩm ID: {} từ product-service.", request.getProductId());
            throw new ResourceNotFoundException("Sản phẩm không tồn tại hoặc không có thông tin giá.", "id", request.getProductId());
        }

        BigDecimal productPrice = BigDecimal.valueOf(productDetailFromService.getPrice());
        
        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();
        
        CartItem savedCartItem;
        if (existingItem.isPresent()) {
            CartItem cartItem = existingItem.get();
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
            cartItem.setPrice(productPrice); 
            savedCartItem = cartItemRepository.save(cartItem);
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setProductId(request.getProductId());
            cartItem.setQuantity(request.getQuantity());
            cartItem.setPrice(productPrice); 
            cart.addCartItem(cartItem); // addCartItem sẽ set cart cho cartItem
            // cartItemRepository.save(cartItem) sẽ được thực hiện qua cascade khi cart được lưu, hoặc lưu cartItem riêng lẻ nếu cần ID ngay
            // Để đơn giản, chúng ta dựa vào việc cart sẽ được lưu ngay sau đó.
        }
        
        cart.setUpdatedAt(LocalDateTime.now());
        Cart savedCart = cartRepository.save(cart);
        
        log.info("Đã thêm/cập nhật sản phẩm ID {} vào giỏ hàng của người dùng ID {}. Giá: {}, Số lượng: {}", 
                request.getProductId(), request.getUserId(), productPrice, request.getQuantity());
        
        // Trả về CartDTO đã được làm giàu
        return buildEnrichedCartDTO(savedCart);
    }

    @Override
    @Transactional
    public CartDTO updateCartItem(Long cartItemId, UpdateCartItemRequest request) {
        // request.getUserId() là cần thiết để xác định giỏ hàng
        Cart cart = getCartEntityByUserId(request.getUserId());
        
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", cartItemId));
        
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            log.warn("CartItem ID {} không thuộc giỏ hàng ID {} của người dùng ID {}", cartItemId, cart.getId(), request.getUserId());
            throw new IllegalArgumentException("CartItem không thuộc giỏ hàng của người dùng này");
        }
        
        cartItem.setQuantity(request.getQuantity());
        // Giá không đổi khi chỉ cập nhật số lượng, vì giá gốc đã được lưu khi thêm vào giỏ
        cartItemRepository.save(cartItem);
        
        cart.setUpdatedAt(LocalDateTime.now());
        Cart savedCart = cartRepository.save(cart);
        
        log.info("Đã cập nhật số lượng cho CartItem ID {} thành {} trong giỏ hàng của người dùng ID {}", 
                cartItemId, request.getQuantity(), request.getUserId());
        return buildEnrichedCartDTO(savedCart);
    }

    @Override
    @Transactional
    public void removeCartItem(Long cartItemId, Long userId) {
        Cart cart = getCartEntityByUserId(userId);
        
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", cartItemId));
        
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            log.warn("CartItem ID {} không thuộc giỏ hàng ID {} của người dùng ID {}", cartItemId, cart.getId(), userId);
            throw new IllegalArgumentException("CartItem không thuộc giỏ hàng của người dùng này");
        }
        
        cart.removeCartItem(cartItem); // Quan trọng để JPA xử lý orphanRemoval và mối quan hệ
        // cartItemRepository.delete(cartItem); // Không cần thiết nếu orphanRemoval = true và cascade hoạt động đúng
        
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart); // Lưu cart sẽ kích hoạt cascade và orphan removal
        log.info("Đã xóa CartItem ID {} khỏi giỏ hàng của người dùng ID {}", cartItemId, userId);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getCartEntityByUserId(userId);
        
        // Với orphanRemoval = true trong Cart entity, chỉ cần clear collection và save Cart
        cart.getCartItems().clear();

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart); 
        
        log.info("Đã xóa toàn bộ giỏ hàng của người dùng: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Cart getCartEntityByUserId(Long userId) {
        return getOrCreateCart(userId);
    }
    
    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            log.info("Tạo giỏ hàng mới cho người dùng ID: {}", userId);
            Cart newCart = new Cart();
            newCart.setUserId(userId);
            LocalDateTime now = LocalDateTime.now();
            newCart.setCreatedAt(now);
            newCart.setUpdatedAt(now);
            return cartRepository.save(newCart);
        });
    }
} 