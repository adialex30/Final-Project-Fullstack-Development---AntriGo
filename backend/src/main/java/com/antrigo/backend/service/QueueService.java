package com.antrigo.backend.service;

import com.antrigo.backend.repository.OrderRepository;
import com.antrigo.backend.repository.StoreSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * QueueService — menghitung nomor antrean & estimasi waktu tunggu dari beban dapur saat ini.
 * "Hari operasional" (business date) dipakai sebagai basis reset nomor antrean setiap hari,
 * bukan tengah malam UTC, supaya konsisten dengan jam operasional warung di Asia/Jakarta.
 */
@Service
@RequiredArgsConstructor
public class QueueService {

    private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Jakarta");
    private static final int DEFAULT_AVG_PREP_MINUTES = 4;

    private final OrderRepository orderRepository;
    private final StoreSettingRepository storeSettingRepository;

    public LocalDate resolveBusinessDate() {
        return LocalDate.now(STORE_ZONE);
    }

    public int nextQueueNumber(LocalDate businessDate) {
        return orderRepository.findMaxQueueNumberForDate(businessDate) + 1;
    }

    public int avgPrepTimeMinutes() {
        return storeSettingRepository.findBySettingKey("avg_prep_time_minutes")
                .map(s -> Integer.parseInt(s.getSettingValue()))
                .orElse(DEFAULT_AVG_PREP_MINUTES);
    }

    /** Estimasi = (jumlah pesanan aktif di depan + pesanan ini sendiri) x rata-rata waktu proses. */
    public int estimateWaitMinutes(LocalDate businessDate, int queueNumber) {
        long ahead = orderRepository.countActiveAheadOf(businessDate, queueNumber);
        return (int) ((ahead + 1) * avgPrepTimeMinutes());
    }
}
