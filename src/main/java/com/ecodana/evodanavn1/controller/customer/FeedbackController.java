package com.ecodana.evodanavn1.controller.customer;

import com.ecodana.evodanavn1.model.Booking;
import com.ecodana.evodanavn1.model.UserFeedback;
import com.ecodana.evodanavn1.service.FeedbackReportService;
import com.ecodana.evodanavn1.service.NotificationService;
import com.ecodana.evodanavn1.model.User;
import com.ecodana.evodanavn1.service.BookingService;
import com.ecodana.evodanavn1.service.UserFeedbackService;
import com.ecodana.evodanavn1.service.InappropriateWordService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/feedback")
public class FeedbackController {

    @Autowired
    private UserFeedbackService userFeedbackService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private FeedbackReportService feedbackReportService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private InappropriateWordService inappropriateWordService;

    @PostMapping("/submit/{bookingId}")
    public String submitFeedback(@PathVariable String bookingId,
                                @RequestParam int rating,
                                @RequestParam(required = false) String content,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để đánh giá");
            return "redirect:/login";
        }

        Booking booking = bookingService.getBookingById(bookingId);
        if (booking == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn đặt xe");
            return "redirect:/booking/my-bookings";
        }

        // Check if booking belongs to current user and is completed
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền đánh giá đơn đặt xe này");
            return "redirect:/booking/my-bookings";
        }

        if (booking.getStatus() != Booking.BookingStatus.Completed) {
            redirectAttributes.addFlashAttribute("error", "Chỉ có thể đánh giá các đơn đặt xe đã hoàn thành");
            return "redirect:/booking/my-bookings";
        }

        // Check if feedback already exists
        if (userFeedbackService.hasFeedbackForBooking(booking)) {
            redirectAttributes.addFlashAttribute("error", "Bạn đã đánh giá đơn đặt xe này rồi");
            return "redirect:/booking/my-bookings";
        }

        try {
            UserFeedback feedback = userFeedbackService.createFeedback(currentUser, booking, rating, content);

            // Kiểm tra từ ngữ không chuẩn mực và tự động báo cáo + thông báo admin
            try {
                java.util.Set<String> matches = inappropriateWordService.findMatches(content);
                if (matches != null && !matches.isEmpty()) {
                    String reason = "Từ ngữ không chuẩn mực: " + String.join(", ", matches);
                    feedbackReportService.createReport(currentUser, feedback, reason);
                    try {
                        notificationService.createNotificationForAllAdmins(
                                "Feedback có từ ngữ không chuẩn mực cho đơn #" + booking.getBookingCode(),
                                feedback.getFeedbackId(),
                                "FEEDBACK_FLAGGED"
                        );
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}

            redirectAttributes.addFlashAttribute("success", "Cảm ơn bạn đã đánh giá! Đánh giá của bạn đã được gửi thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi gửi đánh giá. Vui lòng thử lại.");
        }

        return "redirect:/booking/my-bookings";
    }

    @PostMapping("/report/{feedbackId}")
    @ResponseBody
    public java.util.Map<String, Object> reportFeedback(@PathVariable String feedbackId,
                                                        @RequestParam(required = false) String reason,
                                                        HttpSession session) {
        java.util.Map<String, Object> res = new java.util.HashMap<>();
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            res.put("success", false);
            res.put("message", "Vui lòng đăng nhập để báo cáo đánh giá");
            return res;
        }

        UserFeedback feedback = userFeedbackService.getFeedbackById(feedbackId);
        if (feedback == null) {
            res.put("success", false);
            res.put("message", "Không tìm thấy đánh giá");
            return res;
        }

        try {
            feedbackReportService.createReport(currentUser, feedback, reason != null ? reason : "Nội dung không phù hợp");
            try {
                notificationService.createNotificationForAllAdmins(
                        "Có báo cáo đánh giá mới cho xe: " + (feedback.getVehicle() != null ? feedback.getVehicle().getVehicleModel() : "") ,
                        feedback.getFeedbackId(),
                        "FEEDBACK_REPORT"
                );
            } catch (Exception ignored) {}
            res.put("success", true);
            res.put("message", "Đã gửi báo cáo tới quản trị viên. Cảm ơn bạn!");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Có lỗi xảy ra: " + e.getMessage());
        }
        return res;
    }
}
