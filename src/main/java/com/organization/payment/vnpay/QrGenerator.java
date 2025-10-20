package com.organization.payment.vnpay;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.image.BufferedImage;
import java.util.Hashtable; // Cần thiết cho QR Code hint

public class QrGenerator {

    /**
     * Tạo BufferedImage của QR Code từ Payment URL
     * @param data Chuỗi cần mã hóa (Payment URL VNPAY)
     * @param size Kích thước vuông của QR Code (pixels)
     * @return BufferedImage của QR Code
     * @throws Exception Lỗi trong quá trình tạo QR Code
     */
    public static BufferedImage generateQrCode(String data, int size) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();

        // Thiết lập Encoding và Error Correction
        Hashtable<com.google.zxing.EncodeHintType, Object> hints = new Hashtable<>();
        hints.put(com.google.zxing.EncodeHintType.CHARACTER_SET, "UTF-8");
        // Độ chính xác (L = Low, M = Medium, Q = Quality, H = High)
        hints.put(com.google.zxing.EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H);

        BitMatrix bitMatrix = writer.encode(
                data,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints // Sử dụng hints
        );

        // Chuyển đổi BitMatrix thành BufferedImage (Sử dụng Zxing-javase)
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }
}