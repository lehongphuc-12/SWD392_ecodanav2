package com.ecodana.evodanavn1.controller.admin;

import com.ecodana.evodanavn1.model.BankAccount;
import com.ecodana.evodanavn1.model.RefundRequest;
import com.ecodana.evodanavn1.model.User;
import com.ecodana.evodanavn1.repository.BankAccountRepository;
import com.ecodana.evodanavn1.repository.RefundRequestRepository;
import com.ecodana.evodanavn1.service.RefundRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;

@Controller
@RequestMapping("/admin/refund-requests")
public class RefundRequestController {

    @Autowired
    private RefundRequestRepository refundRequestRepository;

    @Autowired
    private RefundRequestService refundRequestService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    /**
     * Redirect to admin dashboard with refund-requests tab
     */
    @GetMapping
    public String listRefundRequests() {
        return "redirect:/admin?tab=refund-requests";
    }

    /**
     * View refund request details
     */
    @GetMapping("/{id}")
    public String viewRefundRequest(
            @PathVariable String id,
            Model model,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        Optional<RefundRequest> refundRequest = refundRequestRepository.findById(id);
        if (refundRequest.isEmpty()) {
            return "redirect:/admin/refund-requests?error=Refund request not found";
        }

        model.addAttribute("refundRequest", refundRequest.get());
        return "admin/refund-request-detail";
    }

    /**
     * Approve refund request
     */
    @PostMapping("/{id}/approve")
    public String approveRefundRequest(
            @PathVariable String id,
            @RequestParam(required = false) String adminNotes,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            refundRequestService.approveRefundRequest(id, currentUser.getId(), adminNotes != null ? adminNotes : "");
            redirectAttributes.addFlashAttribute("success", "Refund request approved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error approving refund request: " + e.getMessage());
        }

        return "redirect:/admin?tab=refund-requests";
    }

    /**
     * Reject refund request
     */
    @PostMapping("/{id}/reject")
    public String rejectRefundRequest(
            @PathVariable String id,
            @RequestParam(required = false) String adminNotes,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            refundRequestService.rejectRefundRequest(id, currentUser.getId(), adminNotes != null ? adminNotes : "");
            redirectAttributes.addFlashAttribute("success", "Refund request rejected successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error rejecting refund request: " + e.getMessage());
        }

        return "redirect:/admin?tab=refund-requests";
    }

    /**
     * Update bank account for refund request
     */
    @PostMapping("/{id}/update-bank-account")
    public String updateBankAccount(
            @PathVariable String id,
            @RequestParam String bankAccountId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            Optional<RefundRequest> refundRequestOpt = refundRequestRepository.findById(id);
            if (refundRequestOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Refund request not found!");
                return "redirect:/admin?tab=refund-requests";
            }

            Optional<BankAccount> bankAccountOpt = bankAccountRepository.findById(bankAccountId);
            if (bankAccountOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Bank account not found!");
                return "redirect:/admin?tab=refund-requests";
            }

            RefundRequest refundRequest = refundRequestOpt.get();
            refundRequest.setBankAccount(bankAccountOpt.get());
            refundRequestRepository.save(refundRequest);

            redirectAttributes.addFlashAttribute("success", "Bank account updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating bank account: " + e.getMessage());
        }

        return "redirect:/admin?tab=refund-requests";
    }
}
