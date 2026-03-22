package com.ecodana.evodanavn1.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ecodana.evodanavn1.model.User;
import com.ecodana.evodanavn1.model.UserDocument;
import com.ecodana.evodanavn1.repository.UserDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserDocumentService {

    @Autowired
    private UserDocumentRepository userDocumentRepository;

    @Autowired
    private OCRService ocrService;

    @Value("${cloudinary.cloud_name:}")
    private String cloudName;

    @Value("${cloudinary.api_key:}")
    private String cloudApiKey;

    @Value("${cloudinary.api_secret:}")
    private String cloudApiSecret;

    public List<UserDocument> getDocumentsByUserId(String userId) {
        return userDocumentRepository.findByUserId(userId);
    }

    public Optional<UserDocument> getDocumentByUserIdAndType(String userId, UserDocument.DocumentType docType) {
        return userDocumentRepository.findByUserIdAndDocumentType(userId, docType);
    }

    public UserDocument saveDocument(UserDocument document) {
        return userDocumentRepository.save(document);
    }

    public void deleteDocument(String documentId) {
        userDocumentRepository.deleteById(documentId);
    }

    /**
     * Upload document with OCR validation
     */
    public UserDocument uploadDocument(
            User user,
            String documentType,
            MultipartFile frontImage,
            MultipartFile backImage,
            String documentNumber,
            String fullName,
            String dob,
            String issuedDate,
            String issuedPlace
    ) throws Exception {
        
        // Validate inputs
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (frontImage == null || frontImage.isEmpty()) {
            throw new IllegalArgumentException("Front image is required");
        }
        if (backImage == null || backImage.isEmpty()) {
            throw new IllegalArgumentException("Back image is required");
        }
        
        // Parse document type
        UserDocument.DocumentType docTypeEnum;
        try {
            docTypeEnum = UserDocument.DocumentType.valueOf(documentType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid document type: " + documentType);
        }

        // Perform OCR validation on both images (BOTH ARE REQUIRED)
        OCRService.OCRResult ocrResult = ocrService.performOCRWithBothImages(frontImage, backImage, documentType);
        
        if (!ocrResult.isValid()) {
            throw new IllegalArgumentException("OCR Validation Failed: " + ocrResult.getErrorMessage());
        }

        // Initialize Cloudinary
        Cloudinary cloudinary = getCloudinaryInstance();
        if (cloudinary == null) {
            throw new IllegalStateException("Cloudinary is not configured");
        }

        // Upload front image
        String frontImageUrl;
        try {
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                frontImage.getBytes(), 
                ObjectUtils.asMap(
                    "folder", "ecodana/documents/" + user.getId(),
                    "resource_type", "image"
                )
            );
            frontImageUrl = uploadResult.get("secure_url").toString();
        } catch (Exception e) {
            throw new Exception("Failed to upload front image: " + e.getMessage());
        }

        // Upload back image (if provided)
        String backImageUrl = null;
        if (backImage != null && !backImage.isEmpty()) {
            try {
                Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    backImage.getBytes(), 
                    ObjectUtils.asMap(
                        "folder", "ecodana/documents/" + user.getId(),
                        "resource_type", "image"
                    )
                );
                backImageUrl = uploadResult.get("secure_url").toString();
            } catch (Exception e) {
                // Back image is optional, log error but continue
                System.err.println("Failed to upload back image: " + e.getMessage());
            }
        }

        // Check if document already exists for this user and type
        Optional<UserDocument> existingDoc = getDocumentByUserIdAndType(user.getId(), docTypeEnum);
        
        UserDocument document;
        if (existingDoc.isPresent()) {
            // Update existing document
            document = existingDoc.get();
            document.setDocumentNumber(ocrResult.getDocumentNumber() != null ? ocrResult.getDocumentNumber() : documentNumber);
            document.setFullName(ocrResult.getFullName() != null ? ocrResult.getFullName() : fullName);
            document.setDob(ocrResult.getDateOfBirth());
            document.setIssuedDate(ocrResult.getIssuedDate());
            document.setIssuedPlace(ocrResult.getIssuedPlace() != null ? ocrResult.getIssuedPlace() : issuedPlace);
            document.setFrontImageUrl(frontImageUrl);
            if (backImageUrl != null) {
                document.setBackImageUrl(backImageUrl);
            }
            document.setVerified(true); // Auto-verify when re-uploaded
        } else {
            // Create new document
            document = new UserDocument();
            document.setDocumentId(UUID.randomUUID().toString());
            document.setUser(user);
            document.setDocumentType(docTypeEnum);
            document.setDocumentNumber(ocrResult.getDocumentNumber() != null ? ocrResult.getDocumentNumber() : documentNumber);
            document.setFullName(ocrResult.getFullName() != null ? ocrResult.getFullName() : fullName);
            document.setDob(ocrResult.getDateOfBirth());
            document.setIssuedDate(ocrResult.getIssuedDate());
            document.setIssuedPlace(ocrResult.getIssuedPlace() != null ? ocrResult.getIssuedPlace() : issuedPlace);
            document.setFrontImageUrl(frontImageUrl);
            document.setBackImageUrl(backImageUrl);
            document.setVerified(true); // Auto-verify when uploaded
            document.setCreatedDate(LocalDateTime.now());
        }

        return userDocumentRepository.save(document);
    }

    /**
     * Get Cloudinary instance
     */
    private Cloudinary getCloudinaryInstance() {
        if (cloudName != null && !cloudName.isBlank() && 
            cloudApiKey != null && !cloudApiKey.isBlank() && 
            cloudApiSecret != null && !cloudApiSecret.isBlank()) {
            return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", cloudApiKey,
                "api_secret", cloudApiSecret
            ));
        }
        return null;
    }


    /**
     * Check if user has uploaded required documents
     */
    public boolean hasRequiredDocuments(String userId) {
        Optional<UserDocument> citizenId = getDocumentByUserIdAndType(userId, UserDocument.DocumentType.CitizenId);
        Optional<UserDocument> driverLicense = getDocumentByUserIdAndType(userId, UserDocument.DocumentType.DriverLicense);
        
        return citizenId.isPresent() && driverLicense.isPresent();
    }

    /**
     * Check if user has verified documents (all documents are auto-verified now)
     */
    public boolean hasVerifiedDocuments(String userId) {
        return hasRequiredDocuments(userId);
    }


    /**
     * Get document by ID
     */
    public UserDocument getDocumentById(String documentId) {
        return userDocumentRepository.findById(documentId).orElse(null);
    }

    /**
     * Update document information
     */
    public UserDocument updateDocumentInfo(String documentId, String documentNumber, String fullName, 
                                         String dob, String issuedDate, String issuedPlace) {
        UserDocument document = getDocumentById(documentId);
        if (document == null) {
            throw new IllegalArgumentException("Document not found");
        }

        // Allow updating verified documents

        // Update fields if provided
        if (documentNumber != null && !documentNumber.trim().isEmpty()) {
            document.setDocumentNumber(documentNumber.trim());
        }
        
        if (fullName != null && !fullName.trim().isEmpty()) {
            document.setFullName(fullName.trim());
        }
        
        if (dob != null && !dob.trim().isEmpty()) {
            try {
                document.setDob(java.time.LocalDate.parse(dob));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid date format for date of birth");
            }
        }
        
        if (issuedDate != null && !issuedDate.trim().isEmpty()) {
            try {
                document.setIssuedDate(java.time.LocalDate.parse(issuedDate));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid date format for issued date");
            }
        }
        
        if (issuedPlace != null && !issuedPlace.trim().isEmpty()) {
            document.setIssuedPlace(issuedPlace.trim());
        }

        return userDocumentRepository.save(document);
    }
}