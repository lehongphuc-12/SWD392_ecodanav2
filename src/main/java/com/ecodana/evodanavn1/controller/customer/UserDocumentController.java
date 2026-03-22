package com.ecodana.evodanavn1.controller.customer;

import com.ecodana.evodanavn1.model.User;
import com.ecodana.evodanavn1.model.UserDocument;
import com.ecodana.evodanavn1.service.UserDocumentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/documents")
public class UserDocumentController {

    @Autowired
    private UserDocumentService userDocumentService;

    /**
     * Upload document with OCR validation
     */
    @PostMapping("/upload")
    public String uploadDocument(
            @RequestParam("documentType") String documentType,
            @RequestParam("frontImage") MultipartFile frontImage,
            @RequestParam("backImage") MultipartFile backImage,
            @RequestParam(value = "documentNumber", required = false) String documentNumber,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "dob", required = false) String dob,
            @RequestParam(value = "issuedDate", required = false) String issuedDate,
            @RequestParam(value = "issuedPlace", required = false) String issuedPlace,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập!");
            return "redirect:/login";
        }

        try {
            // Validate file types
            if (!isValidImageFile(frontImage)) {
                redirectAttributes.addFlashAttribute("error", "Ảnh mặt trước không hợp lệ. Vui lòng chọn file ảnh (JPG, PNG).");
                return "redirect:/profile#documents";
            }

            if (!isValidImageFile(backImage)) {
                redirectAttributes.addFlashAttribute("error", "Ảnh mặt sau không hợp lệ. Vui lòng chọn file ảnh (JPG, PNG).");
                return "redirect:/profile#documents";
            }

            // Upload document with OCR validation
            userDocumentService.uploadDocument(
                currentUser,
                documentType,
                frontImage,
                backImage,
                documentNumber,
                fullName,
                dob,
                issuedDate,
                issuedPlace
            );

            String docTypeDisplay = "CitizenId".equals(documentType) ? "CCCD" : "Giấy phép lái xe";
            redirectAttributes.addFlashAttribute("success", 
                "Tải lên " + docTypeDisplay + " thành công! Thông tin đã được trích xuất tự động và xác minh.");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi tải ảnh: " + e.getMessage());
        }

        return "redirect:/profile#documents";
    }

    /**
     * Get user's documents (AJAX)
     */
    @GetMapping("/my-documents")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMyDocuments(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            List<UserDocument> documents = userDocumentService.getDocumentsByUserId(currentUser.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("documents", documents);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get document by type (AJAX)
     */
    @GetMapping("/type/{documentType}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDocumentByType(
            @PathVariable UserDocument.DocumentType documentType,
            HttpSession session) {
        
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            Optional<UserDocument> docOpt = userDocumentService.getDocumentByUserIdAndType(currentUser.getId(), documentType);
            
            if (docOpt.isPresent()) {
                UserDocument doc = docOpt.get();
                Map<String, Object> docData = new HashMap<>();
                docData.put("documentId", doc.getDocumentId());
                docData.put("documentType", doc.getDocumentType().name());
                docData.put("documentNumber", doc.getDocumentNumber());
                docData.put("fullName", doc.getFullName());
                docData.put("dob", doc.getDob());
                docData.put("issuedDate", doc.getIssuedDate());
                docData.put("issuedPlace", doc.getIssuedPlace());
                docData.put("frontImageUrl", doc.getFrontImageUrl());
                docData.put("backImageUrl", doc.getBackImageUrl());
                docData.put("isVerified", doc.isVerified());
                docData.put("createdDate", doc.getCreatedDate());
                
                return ResponseEntity.ok(Map.of("success", true, "document", docData));
            } else {
                return ResponseEntity.ok(Map.of("success", false, "message", "Document not found"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete document
     */
    @PostMapping("/delete/{documentId}")
    public String deleteDocument(
            @PathVariable String documentId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập!");
            return "redirect:/login";
        }

        try {
            userDocumentService.deleteDocument(documentId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa giấy tờ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi xóa giấy tờ: " + e.getMessage());
        }

        return "redirect:/profile#documents";
    }

    /**
     * Check if user has required documents (AJAX)
     */
    @GetMapping("/check-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkDocumentStatus(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            boolean hasRequired = userDocumentService.hasRequiredDocuments(currentUser.getId());
            boolean hasVerified = userDocumentService.hasVerifiedDocuments(currentUser.getId());
            
            Map<String, Object> status = new HashMap<>();
            status.put("hasRequiredDocuments", hasRequired);
            status.put("hasVerifiedDocuments", hasVerified);
            
            return ResponseEntity.ok(Map.of("success", true, "status", status));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update document information
     */
    @PostMapping("/{documentId}/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateDocumentInfo(
            @PathVariable String documentId,
            @RequestBody Map<String, String> updateData,
            HttpSession session) {
        
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            userDocumentService.updateDocumentInfo(
                documentId,
                updateData.get("documentNumber"),
                updateData.get("fullName"),
                updateData.get("dob"),
                updateData.get("issuedDate"),
                updateData.get("issuedPlace")
            );
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Document updated successfully"));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to update document"));
        }
    }

    /**
     * Validate if file is an image
     */
    private boolean isValidImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }
        
        return contentType.equals("image/jpeg") || 
               contentType.equals("image/jpg") || 
               contentType.equals("image/png");
    }
}

