// // package com.antrigo.backend.payment;

// // import com.antrigo.backend.domain.entity.Order;
// // import org.springframework.beans.factory.annotation.Value;
// // import org.springframework.stereotype.Service;

// // import java.time.LocalDateTime;
// // import java.util.UUID;

// // /**
// //  * Implementasi dummy — TIDAK memanggil api.midtrans.com. Dipakai supaya alur checkout QRIS punya
// //  * bentuk yang sama seperti produksi (transaction_id, qr_string, waktu kedaluwarsa 15 menit —
// //  * default expiry QRIS Midtrans yang sesungguhnya) tanpa butuh kredensial merchant.
// //  *
// //  * qr_payload di sini SENGAJA bukan format EMVCo QRIS asli (bukan kode yang bisa dipindai e-wallet
// //  * sungguhan) — diberi prefix jelas "MIDTRANS-DUMMY" supaya tidak berpotensi disalahartikan sebagai
// //  * kode pembayaran nyata kalau ter-screenshot/tersebar.
// //  *
// //  * Untuk pasang Midtrans sungguhan nanti: buat implementasi baru dari {@link MidtransGatewayService}
// //  * (mis. pakai SDK `midtrans-java`, panggil endpoint charge dengan server key dari
// //  * MIDTRANS_SERVER_KEY), tandai @Primary, kelas ini otomatis tidak lagi dipakai.
// //  */
// // @Service
// // public class DummyMidtransGatewayService implements MidtransGatewayService {

// //     private final int expiryMinutes;

// //     public DummyMidtransGatewayService(@Value("${app.qris.expiry-minutes:15}") int expiryMinutes) {
// //         this.expiryMinutes = expiryMinutes;
// //     }

// //     @Override
// //     public QrisChargeResult createQrisCharge(Order order) {
// //         String transactionId = UUID.randomUUID().toString();
// //         LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes);

// //         String qrPayload = "MIDTRANS-DUMMY|order=" + order.getOrderNumber()
// //                 + "|amount=" + order.getTotalAmount().longValue()
// //                 + "|txn=" + transactionId;

// //         return new QrisChargeResult(transactionId, qrPayload, expiresAt);
// //     }
// // }

// package com.antrigo.backend.payment;

// import com.antrigo.backend.domain.entity.Order;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
// import org.springframework.stereotype.Service;

// import java.time.LocalDateTime;
// import java.util.UUID;

// /**
//  * Implementasi dummy — TIDAK memanggil api.midtrans.com. Aktif HANYA kalau MIDTRANS_SERVER_KEY
//  * belum diisi (lihat @ConditionalOnExpression) — begitu server key diisi, bean ini otomatis
//  * digantikan {@link RealMidtransGatewayService} tanpa perlu ubah kode lain sama sekali.
//  *
//  * qr_payload di sini SENGAJA bukan format EMVCo QRIS asli (bukan kode yang bisa dipindai e-wallet
//  * sungguhan) — diberi prefix jelas "MIDTRANS-DUMMY" supaya tidak berpotensi disalahartikan sebagai
//  * kode pembayaran nyata kalau ter-screenshot/tersebar.
//  *
//  * Untuk pasang Midtrans sungguhan nanti: buat implementasi baru dari {@link MidtransGatewayService}
//  * (mis. pakai SDK `midtrans-java`, panggil endpoint charge dengan server key dari
//  * MIDTRANS_SERVER_KEY), tandai @Primary, kelas ini otomatis tidak lagi dipakai.
//  */
// @Service
// @ConditionalOnExpression("'${midtrans.server-key:}'.isEmpty()")
// public class DummyMidtransGatewayService implements MidtransGatewayService {

//     private final int expiryMinutes;

//     public DummyMidtransGatewayService(@Value("${app.qris.expiry-minutes:15}") int expiryMinutes) {
//         this.expiryMinutes = expiryMinutes;
//     }

//     @Override
//     public QrisChargeResult createQrisCharge(Order order) {
//         String transactionId = UUID.randomUUID().toString();
//         LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes);

//         String qrPayload = "MIDTRANS-DUMMY|order=" + order.getOrderNumber()
//                 + "|amount=" + order.getTotalAmount().longValue()
//                 + "|txn=" + transactionId;

//         return new QrisChargeResult(transactionId, qrPayload, expiresAt);
//     }
// }

package com.antrigo.backend.payment;

import com.antrigo.backend.domain.entity.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@ConditionalOnExpression("'${midtrans.server-key:}'.isEmpty()")
public class DummyMidtransGatewayService implements MidtransGatewayService {

    private final int expiryMinutes;

    public DummyMidtransGatewayService(@Value("${app.qris.expiry-minutes:15}") int expiryMinutes) {
        this.expiryMinutes = expiryMinutes;
    }

    @Override
    public QrisChargeResult createQrisCharge(Order order) {
        String transactionId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes);

        String qrPayload = "MIDTRANS-DUMMY|order=" + order.getOrderNumber()
                + "|amount=" + order.getTotalAmount().longValue()
                + "|txn=" + transactionId;

        return new QrisChargeResult(transactionId, qrPayload, expiresAt);
    }
}
