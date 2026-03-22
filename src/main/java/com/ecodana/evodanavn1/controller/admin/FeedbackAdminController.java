package com.ecodana.evodanavn1.controller.admin;

import com.ecodana.evodanavn1.model.User;
import com.ecodana.evodanavn1.model.FeedbackReport;
import com.ecodana.evodanavn1.model.UserFeedback;
import com.ecodana.evodanavn1.service.UserFeedbackService;
import com.ecodana.evodanavn1.service.UserService;
import com.ecodana.evodanavn1.service.FeedbackReportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/feedback")
public class FeedbackAdminController {

    @Autowired
    private UserFeedbackService userFeedbackService;

    @Autowired
    private UserService userService;

    @Autowired
    private FeedbackReportService feedbackReportService;

    @GetMapping
    public String feedbackManagement(HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null || !userService.isAdmin(user)) {
            return "redirect:/login";
        }

        List<UserFeedback> allFeedback = userFeedbackService.getAllFeedback();
        List<UserFeedback> feedbackWithReplies = allFeedback.stream()
                .filter(f -> f.getStaffReply() != null && !f.getStaffReply().trim().isEmpty())
                .toList();
        List<UserFeedback> feedbackWithoutReplies = allFeedback.stream()
                .filter(f -> f.getStaffReply() == null || f.getStaffReply().trim().isEmpty())
                .toList();

        model.addAttribute("allFeedback", allFeedback);
        model.addAttribute("feedbackWithReplies", feedbackWithReplies);
        model.addAttribute("feedbackWithoutReplies", feedbackWithoutReplies);
        model.addAttribute("reports", feedbackReportService.getAllReports());
        model.addAttribute("currentUser", user);

        return "admin/admin-feedback";
    }

    @PostMapping("/reports/resolve/{reportId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resolveReport(@PathVariable String reportId, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        User user = (User) session.getAttribute("currentUser");
        if (user == null || !userService.isAdmin(user)) {
            response.put("success", false);
            response.put("message", "Không có quyền thực hiện thao tác này");
            return ResponseEntity.status(403).body(response);
        }

        try {
            FeedbackReport updated = feedbackReportService.resolveReport(reportId);
            if (updated != null) {
                response.put("success", true);
                response.put("message", "Đã đánh dấu báo cáo là đã xử lý");
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy báo cáo");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/delete/{feedbackId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteFeedback(@PathVariable String feedbackId, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        User user = (User) session.getAttribute("currentUser");
        if (user == null || !userService.isAdmin(user)) {
            response.put("success", false);
            response.put("message", "Không có quyền thực hiện thao tác này");
            return ResponseEntity.status(403).body(response);
        }

        try {
            boolean deleted = userFeedbackService.deleteFeedback(feedbackId);
            if (deleted) {
                response.put("success", true);
                response.put("message", "Xóa đánh giá thành công");
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy đánh giá để xóa");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi xóa đánh giá: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reply/{feedbackId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> replyToFeedback(@PathVariable String feedbackId,
                                                               @RequestParam String reply,
                                                               HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        User user = (User) session.getAttribute("currentUser");
        if (user == null || !userService.isAdmin(user)) {
            response.put("success", false);
            response.put("message", "Không có quyền thực hiện thao tác này");
            return ResponseEntity.status(403).body(response);
        }

        // Admin không được phản hồi feedback, chỉ owner mới được
        response.put("success", false);
        response.put("message", "Admin không được phản hồi feedback. Chỉ chủ xe mới có thể phản hồi.");
        return ResponseEntity.status(403).body(response);
    }
}
