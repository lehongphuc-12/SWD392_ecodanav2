package com.ecodana.evodanavn1.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
// import org.springframework.scheduling.annotation.Async; // Removed this import

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendOtpEmail(String to, String otp) throws MessagingException, UnsupportedEncodingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(new InternetAddress(fromEmail, "EcoDana Team"));
        helper.setTo(to);

        // ===== THAY ĐỔI ĐỂ TIÊU ĐỀ EMAIL LÀ DUY NHẤT =====
        // 1. Lấy thời gian hiện tại
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String timestamp = LocalTime.now().format(formatter);

        // 2. Tạo tiêu đề email độc nhất bằng cách thêm thời gian vào cuối
        String uniqueSubject = "Your EcoDana Account Verification OTP [" + timestamp + "]";
        helper.setSubject(uniqueSubject);
        // ===============================================

        String htmlContent = buildOtpHtmlContent(otp);
        helper.setText(htmlContent, true);

        javaMailSender.send(mimeMessage);
    }

    private String buildOtpHtmlContent(String otp) {
        // Phần code HTML này giữ nguyên, không cần thay đổi
        return "<!DOCTYPE html>"
                + "<html lang='vi'>"
                + "<head><style>"
                + "body {font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0;}"
                + ".container {max-width: 600px; margin: 20px auto; background-color: #ffffff; padding: 40px; border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.1); text-align: center;}"
                + ".header {font-size: 28px; color: #1E7E34; margin-bottom: 20px; font-weight: bold;}"
                + ".otp-code {font-size: 36px; font-weight: bold; color: #ffffff; background-color: #28A745; padding: 15px 25px; border-radius: 5px; letter-spacing: 5px; display: inline-block; margin: 20px 0;}"
                + ".message {font-size: 16px; color: #333333; line-height: 1.5;}"
                + ".footer {font-size: 12px; color: #888888; margin-top: 30px;}"
                + "</style></head>"
                + "<body>"
                + "<div class='container'>"
                + "<div class='header'>EcoDana Verification</div>"
                + "<p class='message'>Your One-Time Password (OTP) for account verification is:</p>"
                + "<div class='otp-code'>" + otp + "</div>"
                + "<p class='message'>This code is valid for 5 minutes. Please do not share this code with anyone.</p>"
                + "<p class='footer'>© 2025 EcoDana. All rights reserved.</p>"
                + "</div>"
                + "</body></html>";
    }


    // Removed @Async annotation to ensure synchronous execution after transaction commit
    public void sendPasswordResetEmail(String to, String token, String baseUrl) throws MessagingException, UnsupportedEncodingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(new InternetAddress(fromEmail, "EcoDana Team"));
        helper.setTo(to);
        helper.setSubject("EcoDana - Yêu cầu đặt lại mật khẩu của bạn");


        String resetUrl = baseUrl + "/reset-password?token=" + token;

        String htmlContent = buildPasswordResetHtmlContent(resetUrl);
        helper.setText(htmlContent, true);

        javaMailSender.send(mimeMessage);
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();
        return scheme + "://" + serverName + ":" + serverPort + contextPath;
    }

    private String buildPasswordResetHtmlContent(String resetUrl) {
        return "<!DOCTYPE html>"
                + "<html>" // ... (Nội dung HTML cho email reset, xem ví dụ bên dưới)
                + "<body style='font-family: Arial, sans-serif; text-align: center; color: #333;'>"
                + "<div style='max-width: 600px; margin: auto; border: 1px solid #ddd; padding: 20px;'>"
                + "<h2>Yêu cầu đặt lại mật khẩu</h2>"
                + "<p>Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu cho tài khoản EcoDana của bạn.</p>"
                + "<p>Vui lòng nhấp vào nút bên dưới để đặt lại mật khẩu của bạn. Liên kết này sẽ hết hạn sau 15 phút.</p>"
                + "<a href='" + resetUrl + "' style='background-color: #28a745; color: white; padding: 15px 25px; text-align: center; text-decoration: none; display: inline-block; border-radius: 5px; font-size: 16px; margin: 20px 0;'>Đặt lại mật khẩu</a>"
                + "<p>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.</p>"
                + "<hr><p style='font-size: 12px; color: #888;'> 2025 EcoDana. All rights reserved.</p>"
                + "</div></body></html>";
    }

    /**
     * Gửi email thông báo cho Owner khi có khách hàng đặt xe
     */
    @Async
    public void sendBookingNotificationToOwner(String ownerEmail, String ownerName, String bookingCode, 
                                                String vehicleName, String customerName, 
                                                String pickupDate, String returnDate) throws MessagingException, UnsupportedEncodingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(new InternetAddress(fromEmail, "EcoDana Team"));
        helper.setTo(ownerEmail);
        helper.setSubject("EcoDana - Yêu cầu đặt xe mới #" + bookingCode);

        String htmlContent = buildBookingNotificationHtml(ownerName, bookingCode, vehicleName, 
                                                          customerName, pickupDate, returnDate);
        helper.setText(htmlContent, true);

        javaMailSender.send(mimeMessage);
    }

    private String buildBookingNotificationHtml(String ownerName, String bookingCode, String vehicleName,
                                                 String customerName, String pickupDate, String returnDate) {
        return "<!DOCTYPE html>"
                + "<html lang='vi'>"
                + "<head><style>"
                + "body {font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0;}"
                + ".container {max-width: 600px; margin: 20px auto; background-color: #ffffff; padding: 30px; border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.1);}"
                + ".header {font-size: 24px; color: #1E7E34; margin-bottom: 20px; font-weight: bold; text-align: center;}"
                + ".info-box {background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;}"
                + ".info-row {margin: 10px 0; font-size: 14px;}"
                + ".label {font-weight: bold; color: #555;}"
                + ".value {color: #333;}"
                + ".footer {font-size: 12px; color: #888888; margin-top: 30px; text-align: center;}"
                + "</style></head>"
                + "<body>"
                + "<div class='container'>"
                + "<div class='header'> Yêu cầu đặt xe mới</div>"
                + "<p>Xin chào <strong>" + ownerName + "</strong>,</p>"
                + "<p>Bạn có một yêu cầu đặt xe mới cần xác nhận:</p>"
                + "<div class='info-box'>"
                + "<div class='info-row'><span class='label'>Mã đơn:</span> <span class='value'>" + bookingCode + "</span></div>"
                + "<div class='info-row'><span class='label'>Xe:</span> <span class='value'>" + vehicleName + "</span></div>"
                + "<div class='info-row'><span class='label'>Khách hàng:</span> <span class='value'>" + customerName + "</span></div>"
                + "<div class='info-row'><span class='label'>Ngày nhận xe:</span> <span class='value'>" + pickupDate + "</span></div>"
                + "<div class='info-row'><span class='label'>Ngày trả xe:</span> <span class='value'>" + returnDate + "</span></div>"
                + "</div>"
                + "<p>Vui lòng đăng nhập vào hệ thống để xem chi tiết và xác nhận đơn đặt xe.</p>"
                + "<p class='footer'> 2025 EcoDana. All rights reserved.</p>"
                + "</div>"
                + "</body></html>";
    }

    /**
     * Gửi email yêu cầu thanh toán cho Customer khi Owner chấp nhận booking
     */
    @Async
    public void sendPaymentRequestToCustomer(String customerEmail, String customerName, String bookingCode,
                                             String vehicleName, String totalAmount, String depositAmount,
                                             String paymentUrl) throws MessagingException, UnsupportedEncodingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(new InternetAddress(fromEmail, "EcoDana Team"));
        helper.setTo(customerEmail);
        helper.setSubject("EcoDana - Yêu cầu thanh toán đơn #" + bookingCode);

        String htmlContent = buildPaymentRequestHtml(customerName, bookingCode, vehicleName, 
                                                     totalAmount, depositAmount, paymentUrl);
        helper.setText(htmlContent, true);

        javaMailSender.send(mimeMessage);
    }

    private String buildPaymentRequestHtml(String customerName, String bookingCode, String vehicleName,
                                           String totalAmount, String depositAmount, String paymentUrl) {
        return "<!DOCTYPE html>"
                + "<html lang='vi'>"
                + "<head><style>"
                + "body {font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0;}"
                + ".container {max-width: 600px; margin: 20px auto; background-color: #ffffff; padding: 30px; border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.1);}"
                + ".header {font-size: 24px; color: #1E7E34; margin-bottom: 20px; font-weight: bold; text-align: center;}"
                + ".success-badge {background-color: #28a745; color: white; padding: 10px 20px; border-radius: 5px; display: inline-block; margin: 15px 0;}"
                + ".info-box {background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;}"
                + ".info-row {margin: 10px 0; font-size: 14px;}"
                + ".label {font-weight: bold; color: #555;}"
                + ".value {color: #333;}"
                + ".btn {background-color: #28a745; color: white; padding: 15px 30px; text-align: center; text-decoration: none; display: inline-block; border-radius: 5px; font-size: 16px; margin: 20px 0;}"
                + ".footer {font-size: 12px; color: #888888; margin-top: 30px; text-align: center;}"
                + "</style></head>"
                + "<body>"
                + "<div class='container'>"
                + "<div class='header'> Đơn đặt xe đã được chấp nhận</div>"
                + "<p>Xin chào <strong>" + customerName + "</strong>,</p>"
                + "<p>Chúc mừng! Đơn đặt xe của bạn đã được chủ xe chấp nhận.</p>"
                + "<div class='info-box'>"
                + "<div class='info-row'><span class='label'>Mã đơn:</span> <span class='value'>" + bookingCode + "</span></div>"
                + "<div class='info-row'><span class='label'>Xe:</span> <span class='value'>" + vehicleName + "</span></div>"
                + "<div class='info-row'><span class='label'>Tổng tiền:</span> <span class='value'>" + totalAmount + " VNĐ</span></div>"
                + "<div class='info-row'><span class='label'>Tiền cọc (20%):</span> <span class='value'>" + depositAmount + " VNĐ</span></div>"
                + "</div>"
                + "<p><strong>Vui lòng thanh toán để hoàn tất đơn đặt xe.</strong></p>"
                + "<div style='text-align: center;'>"
                + "<a href='" + paymentUrl + "' class='btn'>Thanh toán ngay</a>"
                + "</div>"
                + "<p style='font-size: 13px; color: #666;'>Lưu ý: Đơn đặt xe sẽ bị hủy nếu bạn không thanh toán trong thời gian quy định.</p>"
                + "<p class='footer'> 2025 EcoDana. All rights reserved.</p>"
                + "</div>"
                + "</body></html>";
    }

    /**
     * Gửi email xác nhận thanh toán cho Owner khi Customer đã thanh toán
     */
    @Async
    public void sendPaymentConfirmationToOwner(String ownerEmail, String ownerName, String bookingCode,
                                               String vehicleName, String customerName, String amount,
                                               String pickupDate) throws MessagingException, UnsupportedEncodingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(new InternetAddress(fromEmail, "EcoDana Team"));
        helper.setTo(ownerEmail);
        helper.setSubject("EcoDana - Đơn #" + bookingCode + " đã được thanh toán");

        String htmlContent = buildPaymentConfirmationHtml(ownerName, bookingCode, vehicleName, 
                                                          customerName, amount, pickupDate);
        helper.setText(htmlContent, true);

        javaMailSender.send(mimeMessage);
    }

    private String buildPaymentConfirmationHtml(String ownerName, String bookingCode, String vehicleName,
                                                String customerName, String amount, String pickupDate) {
        return "<!DOCTYPE html>"
                + "<html lang='vi'>"
                + "<head><style>"
                + "body {font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0;}"
                + ".container {max-width: 600px; margin: 20px auto; background-color: #ffffff; padding: 30px; border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.1);}"
                + ".header {font-size: 24px; color: #1E7E34; margin-bottom: 20px; font-weight: bold; text-align: center;}"
                + ".success-badge {background-color: #28a745; color: white; padding: 10px 20px; border-radius: 5px; display: inline-block; margin: 15px 0; text-align: center;}"
                + ".info-box {background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;}"
                + ".info-row {margin: 10px 0; font-size: 14px;}"
                + ".label {font-weight: bold; color: #555;}"
                + ".value {color: #333;}"
                + ".highlight {background-color: #d4edda; border-left: 4px solid #28a745; padding: 15px; margin: 15px 0;}"
                + ".footer {font-size: 12px; color: #888888; margin-top: 30px; text-align: center;}"
                + "</style></head>"
                + "<body>"
                + "<div class='container'>"
                + "<div class='header'> Thanh toán thành công</div>"
                + "<p>Xin chào <strong>" + ownerName + "</strong>,</p>"
                + "<div class='success-badge'> Khách hàng đã thanh toán</div>"
                + "<p>Đơn đặt xe của bạn đã được khách hàng thanh toán thành công.</p>"
                + "<div class='info-box'>"
                + "<div class='info-row'><span class='label'>Mã đơn:</span> <span class='value'>" + bookingCode + "</span></div>"
                + "<div class='info-row'><span class='label'>Xe:</span> <span class='value'>" + vehicleName + "</span></div>"
                + "<div class='info-row'><span class='label'>Khách hàng:</span> <span class='value'>" + customerName + "</span></div>"
                + "<div class='info-row'><span class='label'>Số tiền:</span> <span class='value'>" + amount + " VNĐ</span></div>"
                + "<div class='info-row'><span class='label'>Ngày nhận xe:</span> <span class='value'>" + pickupDate + "</span></div>"
                + "</div>"
                + "<div class='highlight'>"
                + "<strong> Hành động tiếp theo:</strong><br>"
                + "Vui lòng chuẩn bị xe và liên hệ với khách hàng để sắp xếp việc giao xe."
                + "</div>"
                + "<p class='footer'> 2025 EcoDana. All rights reserved.</p>"
                + "</div>"
                + "</body></html>";
    }

    /**
     * Gửi email thông báo cho user khi được duyệt trở thành Owner
     */
    @Async
    public void sendOwnerApprovalNotification(String userEmail, String userName) throws MessagingException, UnsupportedEncodingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(new InternetAddress(fromEmail, "EcoDana Team"));
        helper.setTo(userEmail);
        helper.setSubject("EcoDana - Chúc mừng! Bạn đã trở thành Owner");

        String htmlContent = buildOwnerApprovalHtml(userName);
        helper.setText(htmlContent, true);

        javaMailSender.send(mimeMessage);
    }

    private String buildOwnerApprovalHtml(String userName) {
        return "<!DOCTYPE html>"
                + "<html lang='vi'>"
                + "<head><style>"
                + "body {font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0;}"
                + ".container {max-width: 600px; margin: 20px auto; background-color: #ffffff; padding: 30px; border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.1);}"
                + ".header {font-size: 28px; color: #1E7E34; margin-bottom: 20px; font-weight: bold; text-align: center;}"
                + ".success-icon {font-size: 60px; text-align: center; margin: 20px 0;}"
                + ".success-badge {background-color: #28a745; color: white; padding: 15px 30px; border-radius: 5px; display: inline-block; margin: 20px 0; font-size: 18px; text-align: center;}"
                + ".message {font-size: 16px; color: #333; line-height: 1.6; margin: 15px 0;}"
                + ".benefits {background-color: #f8f9fa; padding: 20px; border-radius: 5px; margin: 20px 0;}"
                + ".benefit-item {margin: 10px 0; padding-left: 25px; position: relative;}"
                + ".benefit-item:before {content: '✓'; position: absolute; left: 0; color: #28a745; font-weight: bold; font-size: 18px;}"
                + ".btn {background-color: #28a745; color: white; padding: 15px 30px; text-align: center; text-decoration: none; display: inline-block; border-radius: 5px; font-size: 16px; margin: 20px 0;}"
                + ".footer {font-size: 12px; color: #888888; margin-top: 30px; text-align: center;}"
                + "</style></head>"
                + "<body>"
                + "<div class='container'>"
                + "<div class='success-icon'>🎉</div>"
                + "<div class='header'>Chúc mừng bạn!</div>"
                + "<div style='text-align: center;'>"
                + "<div class='success-badge'>✅ Bạn đã trở thành Owner</div>"
                + "</div>"
                + "<p class='message'>Xin chào <strong>" + userName + "</strong>,</p>"
                + "<p class='message'>Chúc mừng! Tài khoản của bạn đã được quản trị viên phê duyệt và nâng cấp lên vai trò <strong>Owner</strong> trên nền tảng EcoDana.</p>"
                + "<div class='benefits'>"
                + "<p style='font-weight: bold; margin-bottom: 15px; color: #1E7E34;'>Với vai trò Owner, bạn có thể:</p>"
                + "<div class='benefit-item'>Đăng ký và quản lý xe của bạn trên hệ thống</div>"
                + "<div class='benefit-item'>Nhận và xử lý yêu cầu đặt xe từ khách hàng</div>"
                + "<div class='benefit-item'>Theo dõi doanh thu và thống kê chi tiết</div>"
                + "<div class='benefit-item'>Quản lý lịch trình và tình trạng xe</div>"
                + "<div class='benefit-item'>Tương tác trực tiếp với khách hàng</div>"
                + "</div>"
                + "<p class='message'>Hãy đăng nhập vào tài khoản của bạn và bắt đầu đăng ký xe để cho thuê ngay hôm nay!</p>"
                + "<div style='text-align: center;'>"
                + "<a href='https://ecodanav2.onrender.com/owner/dashboard' class='btn'>Truy cập Owner Dashboard</a>"
                + "</div>"
                + "<p class='message' style='font-size: 14px; color: #666;'>Nếu bạn có bất kỳ câu hỏi nào, vui lòng liên hệ với đội ngũ hỗ trợ của chúng tôi.</p>"
                + "<p class='footer'>© 2025 EcoDana. All rights reserved.</p>"
                + "</div>"
                + "</body></html>";
    }
}