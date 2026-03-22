package com.ecodana.evodanavn1.controller.owner;

import com.ecodana.evodanavn1.model.User;
import com.ecodana.evodanavn1.model.UserFeedback;
import com.ecodana.evodanavn1.service.UserFeedbackService;
import com.ecodana.evodanavn1.service.UserService;
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
@RequestMapping("/owner/feedback")
public class FeedbackOwnerController {

    @Autowired
    private UserFeedbackService userFeedbackService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String feedbackManagement(HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null || !userService.isOwner(user)) {
            return "redirect:/login";
        }

        List<UserFeedback> ownerFeedback = userFeedbackService.getFeedbackForOwner(user);
        List<UserFeedback> feedbackWithReplies = ownerFeedback.stream()
                .filter(f -> f.getStaffReply() != null && !f.getStaffReply().trim().isEmpty())
                .toList();
        List<UserFeedback> feedbackWithoutReplies = ownerFeedback.stream()
                .filter(f -> f.getStaffReply() == null || f.getStaffReply().trim().isEmpty())
                .toList();

        model.addAttribute("ownerFeedback", ownerFeedback);
        model.addAttribute("feedbackWithReplies", feedbackWithReplies);
        model.addAttribute("feedbackWithoutReplies", feedbackWithoutReplies);
        model.addAttribute("currentUser", user);
        model.addAttribute("currentPage", "feedback");

        return "owner/owner-feedback";
    }

    @PostMapping("/reply/{feedbackId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> replyToFeedback(@PathVariable String feedbackId,
                                                               @RequestParam String reply,
                                                               HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        User user = (User) session.getAttribute("currentUser");
        if (user == null || !userService.isOwner(user)) {
            response.put("success", false);
            response.put("message", "Không có quyền thực hiện thao tác này");
            return ResponseEntity.status(403).body(response);
        }

        // Verify that the feedback is for a vehicle owned by this user
        UserFeedback feedback = userFeedbackService.getFeedbackById(feedbackId);
        if (feedback == null || !feedback.getVehicle().getOwnerId().equals(user.getId())) {
            response.put("success", false);
            response.put("message", "Không có quyền phản hồi đánh giá này");
            return ResponseEntity.status(403).body(response);
        }

        // Check if feedback already has a reply
        if (feedback.getStaffReply() != null && !feedback.getStaffReply().trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Đánh giá này đã có phản hồi rồi");
            return ResponseEntity.status(400).body(response);
        }

        try {
            UserFeedback updatedFeedback = userFeedbackService.replyToFeedback(feedbackId, reply);
            if (updatedFeedback != null) {
                response.put("success", true);
                response.put("message", "Phản hồi đánh giá thành công");
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy đánh giá để phản hồi");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi phản hồi đánh giá: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}
