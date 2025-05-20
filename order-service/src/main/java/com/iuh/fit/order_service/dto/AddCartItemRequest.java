package com.iuh.fit.order_service.dto;

// BigDecimal import removed as price field is removed
// import java.math.BigDecimal; 

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddCartItemRequest {
    
    // userId sẽ được controller set, không phải là một phần của request body từ client.
    // @NotNull(message = "ID người dùng không được để trống") 
    private Long userId;
    
    @NotNull(message = "ID sản phẩm không được để trống")
    private Long productId;
    
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity = 1;
    
    // Trường price đã được loại bỏ. Giá sẽ được lấy từ product-service.
    // private BigDecimal price;
    
    // Các trường này cũng không cần thiết cho request body
//    private String productName;
//    private String productImage;
} 