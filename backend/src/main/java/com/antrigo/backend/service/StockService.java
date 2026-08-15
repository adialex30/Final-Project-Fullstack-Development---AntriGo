package com.antrigo.backend.service;

import com.antrigo.backend.domain.entity.Product;
import com.antrigo.backend.domain.entity.StockMovement;
import com.antrigo.backend.domain.entity.User;
import com.antrigo.backend.domain.enums.StockMovementType;
import com.antrigo.backend.dto.request.StockAdjustmentRequest;
import com.antrigo.backend.dto.response.LowStockResponse;
import com.antrigo.backend.exception.InsufficientStockException;
import com.antrigo.backend.exception.ResourceNotFoundException;
import com.antrigo.backend.repository.ProductRepository;
import com.antrigo.backend.repository.StockMovementRepository;
import com.antrigo.backend.repository.StoreSettingRepository;
import com.antrigo.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * StockService — stok adalah ledger (stock_movements), kolom products.stock hanya cache
 * yang direkonsiliasi setiap kali ada movement. Semua perubahan stok WAJIB lewat service ini
 * supaya selalu tercatat dan bisa ditelusuri.
 */
@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StoreSettingRepository storeSettingRepository;
    private final UserRepository userRepository;

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    @Transactional
    @CacheEvict(value = {"products", "reports"}, allEntries = true)
    public void adjust(Long productId, StockAdjustmentRequest request, String actorEmail) {
        // Lock baris produk supaya adjustment manual tidak bertabrakan dengan checkout yang sedang berjalan.
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produk tidak ditemukan: " + productId));

        int newStock = product.getStock() + request.quantityChange();
        if (newStock < 0) {
            throw new InsufficientStockException(
                    "Penyesuaian stok membuat stok " + product.getName() + " menjadi negatif");
        }

        product.setStock(newStock);
        productRepository.save(product);

        User actor = actorEmail == null ? null :
                userRepository.findByEmailAndActiveTrue(actorEmail).orElse(null);

        StockMovement movement = StockMovement.builder()
                .product(product)
                .type(request.quantityChange() >= 0 ? StockMovementType.IN : StockMovementType.ADJUSTMENT)
                .quantityChange(request.quantityChange())
                .referenceType("MANUAL")
                .note(request.note())
                .createdBy(actor)
                .build();
        stockMovementRepository.save(movement);
    }

    public List<LowStockResponse> findLowStock() {
        int threshold = storeSettingRepository.findBySettingKey("low_stock_threshold")
                .map(s -> Integer.parseInt(s.getSettingValue()))
                .orElse(DEFAULT_LOW_STOCK_THRESHOLD);

        return productRepository.findLowStock(threshold).stream()
                .map(p -> new LowStockResponse(p.getId(), p.getName(), p.getStock(), threshold))
                .collect(Collectors.toList());
    }
}
