package com.ecodana.evodanavn1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OCRService {

    @Value("${ocr.space.api.key:}")
    private String ocrSpaceApiKey;
    
    private static final String OCR_SPACE_API_URL = "https://api.ocr.space/parse/image";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * OCR Result containing extracted information
     */
    public static class OCRResult {
        private boolean valid;
        private String documentType; // "CitizenId" or "DriverLicense"
        private String documentNumber;
        private String fullName;
        private LocalDate dateOfBirth;
        private LocalDate issuedDate;
        private String issuedPlace;
        private String extractedText;
        private String errorMessage;

        // Getters and Setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }
        public String getDocumentNumber() { return documentNumber; }
        public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public LocalDate getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
        public LocalDate getIssuedDate() { return issuedDate; }
        public void setIssuedDate(LocalDate issuedDate) { this.issuedDate = issuedDate; }
        public String getIssuedPlace() { return issuedPlace; }
        public void setIssuedPlace(String issuedPlace) { this.issuedPlace = issuedPlace; }
        public String getExtractedText() { return extractedText; }
        public void setExtractedText(String extractedText) { this.extractedText = extractedText; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    /**
     * Perform OCR on uploaded image using Google Vision API
     */
    public OCRResult performOCR(MultipartFile file, String expectedDocType) {
        OCRResult result = new OCRResult();
        
        try {
            // Validate file
            if (file == null || file.isEmpty()) {
                result.setValid(false);
                result.setErrorMessage("File is empty");
                return result;
            }

            // Validate file size (max 10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                result.setValid(false);
                result.setErrorMessage("File size exceeds 10MB limit");
                return result;
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("image/"))) {
                result.setValid(false);
                result.setErrorMessage("Invalid file type. Only images are allowed.");
                return result;
            }

            // Perform OCR using Google Vision API
            String extractedText = extractTextFromImage(file);
            result.setExtractedText(extractedText);

            if (extractedText == null || extractedText.trim().isEmpty()) {
                result.setValid(false);
                result.setErrorMessage("Could not extract text from image. Please ensure the image is clear.");
                return result;
            }

            // Parse extracted text based on document type
            if ("CitizenId".equalsIgnoreCase(expectedDocType)) {
                parseVietnameseCitizenId(extractedText, result);
            } else if ("DriverLicense".equalsIgnoreCase(expectedDocType)) {
                parseVietnameseDriverLicense(extractedText, result);
            } else {
                result.setValid(false);
                result.setErrorMessage("Unsupported document type");
                return result;
            }

            return result;

        } catch (Exception e) {
            result.setValid(false);
            result.setErrorMessage("OCR processing failed: " + e.getMessage());
            System.err.println("OCR Error: " + e.getMessage());
            e.printStackTrace();
            return result;
        }
    }

    /**
     * Perform OCR on both front and back images - BOTH IMAGES ARE REQUIRED
     */
    public OCRResult performOCRWithBothImages(MultipartFile frontImage, MultipartFile backImage, String expectedDocType) {
        OCRResult result = new OCRResult();
        
        try {
            // Validate both images are required
            if (frontImage == null || frontImage.isEmpty()) {
                result.setValid(false);
                result.setErrorMessage("Front image is required");
                return result;
            }
            
            if (backImage == null || backImage.isEmpty()) {
                result.setValid(false);
                result.setErrorMessage("Back image is required");
                return result;
            }

            // Perform OCR on front image
            OCRResult frontResult = performOCR(frontImage, expectedDocType);
            
            // If front image OCR failed, return the error
            if (!frontResult.isValid()) {
                return frontResult;
            }

            // Perform OCR on back image
            OCRResult backResult = performOCR(backImage, expectedDocType); 

            // Copy front result to main result
            result.setValid(true);
            result.setDocumentType(frontResult.getDocumentType());
            result.setDocumentNumber(frontResult.getDocumentNumber());
            result.setFullName(frontResult.getFullName());
            result.setDateOfBirth(frontResult.getDateOfBirth());
            result.setIssuedDate(frontResult.getIssuedDate());
            result.setIssuedPlace(frontResult.getIssuedPlace());
            result.setExtractedText(frontResult.getExtractedText());

            // Combine information from both images
            combineOCRResults(result, backResult);

            return result;

        } catch (Exception e) {
            result.setValid(false);
            result.setErrorMessage("OCR processing failed: " + e.getMessage());
            System.err.println("OCR Error: " + e.getMessage());
            e.printStackTrace();
            return result;
        }
    }

    /**
     * Combine OCR results from front and back images
     */
    private void combineOCRResults(OCRResult mainResult, OCRResult backResult) {
        // If main result is missing some fields, try to get them from back result
        if (mainResult.getDocumentNumber() == null && backResult.getDocumentNumber() != null) {
            mainResult.setDocumentNumber(backResult.getDocumentNumber());
        }
        
        if (mainResult.getFullName() == null && backResult.getFullName() != null) {
            mainResult.setFullName(backResult.getFullName());
        }
        
        if (mainResult.getDateOfBirth() == null && backResult.getDateOfBirth() != null) {
            mainResult.setDateOfBirth(backResult.getDateOfBirth());
        }
        
        if (mainResult.getIssuedDate() == null && backResult.getIssuedDate() != null) {
            mainResult.setIssuedDate(backResult.getIssuedDate());
        }
        
        if (mainResult.getIssuedPlace() == null && backResult.getIssuedPlace() != null) {
            mainResult.setIssuedPlace(backResult.getIssuedPlace());
        }

        // Combine extracted text
        if (backResult.getExtractedText() != null && !backResult.getExtractedText().trim().isEmpty()) {
            String combinedText = mainResult.getExtractedText() + "\n\n--- BACK IMAGE ---\n\n" + backResult.getExtractedText();
            mainResult.setExtractedText(combinedText);
        }
    }

    /**
     * Extract text from image using OCR.space API with OCR Engine 2
     */
    private String extractTextFromImage(MultipartFile file) {
        try {
            // Check API key
            if (ocrSpaceApiKey == null || ocrSpaceApiKey.isEmpty()) {
                System.err.println("OCR.space API key is not configured");
                return null;
            }

            // Prepare multipart request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("apikey", ocrSpaceApiKey);

            // Create multipart body
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // Add file as ByteArrayResource
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            
            body.add("file", fileResource);
            body.add("language", "vnm"); // Vietnamese language
            body.add("OCREngine", "5"); // OCR Engine 2 - better for Vietnamese
            body.add("isTable", "false");
            body.add("scale", "true"); // Auto-scale image for better OCR
            body.add("detectOrientation", "true"); // Auto-detect image orientation
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // Make API call
            ResponseEntity<String> response = restTemplate.exchange(
                OCR_SPACE_API_URL,
                HttpMethod.POST,
                requestEntity,
                String.class
            );
            
            // Parse response
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                
                // Check for errors
                if (jsonResponse.has("IsErroredOnProcessing") && 
                    jsonResponse.get("IsErroredOnProcessing").asBoolean()) {
                    String errorMessage = jsonResponse.has("ErrorMessage") ? 
                        jsonResponse.get("ErrorMessage").get(0).asText() : "Unknown error";
                    System.err.println("OCR.space API Error: " + errorMessage);
                    return null;
                }
                
                // Extract text from ParsedResults
                if (jsonResponse.has("ParsedResults") && jsonResponse.get("ParsedResults").isArray()) {
                    JsonNode parsedResults = jsonResponse.get("ParsedResults").get(0);
                    if (parsedResults.has("ParsedText")) {
                        String extractedText = parsedResults.get("ParsedText").asText();
                        
                        System.out.println("=== OCR Extracted Text ===");
                        System.out.println(extractedText);
                        System.out.println("=== End OCR Text ===");
                        
                        return extractedText;
                    }
                }
                
                System.err.println("No text found in OCR response");
                return null;
            }
            
            System.err.println("OCR.space API returned error status: " + response.getStatusCode());
            return null;
            
        } catch (Exception e) {
            System.err.println("OCR.space API Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parse Vietnamese Citizen ID card text
     */
    private void parseVietnameseCitizenId(String text, OCRResult result) {
        result.setDocumentType("CitizenId");

        // Normalize text - OCR.space may have line breaks differently
        text = normalizeOCRText(text);
        
        // Common keywords in Vietnamese ID cards
        boolean hasCitizenIdKeywords = text.contains("CAN CUOC CONG DAN") || 
                                        text.contains("CITIZEN IDENTITY CARD");

        // Extract ID number - pattern for "SO / NO.: 049204012627"
        Pattern idPattern = Pattern.compile("(?:SO / NO\\.|SO/NO\\.|SO|NO\\.)[:\\s]+(\\d{9,12})");
        Matcher idMatcher = idPattern.matcher(text);
        if (idMatcher.find()) {
            result.setDocumentNumber(idMatcher.group(1));
        }

        // Extract full name - pattern for "HO VA TEN / FULL NAME: ĐANG NGOC HAI"
        Pattern namePattern = Pattern.compile("(?:HO VA TEN / FULL NAME|HO VA TEN|FULL NAME)[:\\s]+([A-ZÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ\\s]+?)(?=\\s*(?:NGAY SINH|DATE OF BIRTH|GIOI TINH|SEX|QUOC TICH|NATIONALITY|$))");
        Matcher nameMatcher = namePattern.matcher(text);
        if (nameMatcher.find()) {
            String name = nameMatcher.group(1).trim();
            // Remove common suffixes like NAM, NU (Male/Female)
            name = name.replaceAll("\\s+(NAM|NU|MALE|FEMALE)$", "");
            result.setFullName(name.trim());
        } 

        // Extract date of birth - pattern for "NGAY SINH / DATE OF BIRTH: 11/10/2004"
        Pattern dobPattern = Pattern.compile("(?:NGAY SINH / DATE OF BIRTH|NGAY SINH|DATE OF BIRTH)[:\\s]+(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})");
        Matcher dobMatcher = dobPattern.matcher(text);
        if (dobMatcher.find()) {
            result.setDateOfBirth(parseDate(dobMatcher.group(1)));
        }

        // Extract issued date - look for "NGAY, THANG, NAM / DATE, MONTH, YEAR: 28/09/2021" format
        Pattern issuedDatePattern = Pattern.compile("(?:NGAY, THANG, NAM / DATE, MONTH, YEAR|NGAY, THANG, NAM|DATE, MONTH, YEAR)[:\\s]+(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})");
        Matcher issuedDateMatcher = issuedDatePattern.matcher(text);
        if (issuedDateMatcher.find()) {
            result.setIssuedDate(parseDate(issuedDateMatcher.group(1)));
        } 

        // Extract issued place - look for "NOI THUONG TRU / PLACE OF RESIDENCE:" or "QUE QUAN / PLACE OF ORIGIN:"
        Pattern issuedPlacePattern = Pattern.compile("(?:NOI THUONG TRU / PLACE OF RESIDENCE|NOI THUONG TRU|PLACE OF RESIDENCE|QUE QUAN / PLACE OF ORIGIN|QUE QUAN|PLACE OF ORIGIN)[:\\s]+([A-ZÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ\\s,]+?)(?=\\s*(?:CO GIA TRI|NGAY|DATE|$))");
        Matcher issuedPlaceMatcher = issuedPlacePattern.matcher(text);
        if (issuedPlaceMatcher.find()) {
            result.setIssuedPlace(issuedPlaceMatcher.group(1).trim());
        }

        // Validate: must have at least ID number and one other field
        if (result.getDocumentNumber() != null && 
            (result.getFullName() != null || result.getDateOfBirth() != null || hasCitizenIdKeywords)) {
            result.setValid(true);
        } else {
            result.setValid(false);
            result.setErrorMessage("Could not extract enough information from Citizen ID. Please ensure image is clear and shows the front side.");
        }
    }

    /**
     * Parse Vietnamese Driver License text
     */
    private void parseVietnameseDriverLicense(String text, OCRResult result) {
        result.setDocumentType("DriverLicense");
        
        // Normalize text
        text = normalizeOCRText(text);
        
        // Common keywords in Vietnamese Driver License
        boolean hasLicenseKeywords = text.contains("GIAY PHEP LAI XE") ||
                                     text.contains("DRIVER'S LICENSE") ||
                                     text.contains("LAI XE") ||
                                     text.contains("BO GTVT");

        // Extract license number - pattern for "SO/NO: 791149379530"
        Pattern licensePattern = Pattern.compile("(?:SO/NO|SO|NO)[:\\s]+(\\d{8,12})");
        Matcher licenseMatcher = licensePattern.matcher(text);
        if (licenseMatcher.find()) {
            result.setDocumentNumber(licenseMatcher.group(1));
        }

        // Extract full name - pattern for "HO TEN/FULL NAME: ĐANG NGOC TRAM"
        Pattern namePattern = Pattern.compile("(?:HO TEN/FULL NAME|HO TEN|FULL NAME)[:\\s]+([A-ZÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ\\s]+?)(?=\\s*(?:NGAY SINH|DATE OF BIRTH|QUOC TICH|NATIONALITY|NOI CU TRU|ADDRESS|$))");
        Matcher nameMatcher = namePattern.matcher(text);
        if (nameMatcher.find()) {
            String name = nameMatcher.group(1).trim();
            // Remove common suffixes
            name = name.replaceAll("\\s+(NAM|NU|MALE|FEMALE)$", "");
            result.setFullName(name.trim());
        }

        // Extract date of birth - pattern for "NGAY SINH DATE OF BIRTH: 19/02/1993"
        Pattern dobPattern = Pattern.compile("(?:NGAY SINH DATE OF BIRTH|NGAY SINH|DATE OF BIRTH)[:\\s]+(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})");
        Matcher dobMatcher = dobPattern.matcher(text);
        if (dobMatcher.find()) {
            result.setDateOfBirth(parseDate(dobMatcher.group(1)));
        }

        // Extract issued date - look for "NGAY TRUNG TUYEN BEGINNING DATE 23/06/2020" format
        Pattern issuedDatePattern = Pattern.compile("(?:NGAY TRUNG TUYEN BEGINNING DATE|NGAY TRUNG TUYEN|BEGINNING DATE)[:\\s]+(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})");
        Matcher issuedDateMatcher = issuedDatePattern.matcher(text);
        if (issuedDateMatcher.find()) {
            result.setIssuedDate(parseDate(issuedDateMatcher.group(1)));
        }

        // Extract issued place - look for "TP. HO CHI MINH"
        Pattern issuedPlacePattern = Pattern.compile("(?:TP\\. HO CHI MINH|TP HO CHI MINH|HO CHI MINH|TP\\. HCM|TP HCM)");
        Matcher issuedPlaceMatcher = issuedPlacePattern.matcher(text);
        if (issuedPlaceMatcher.find()) {
            result.setIssuedPlace("TP. Hồ Chí Minh");
        }

        // Validate
        if (result.getDocumentNumber() != null && 
            (result.getFullName() != null || result.getDateOfBirth() != null || hasLicenseKeywords)) {
            result.setValid(true);
        } else {
            result.setValid(false);
            result.setErrorMessage("Could not extract enough information from Driver License. Please ensure image is clear and shows the front side.");
        }
    }

    /**
     * Normalize OCR text from OCR.space
     * OCR.space uses \r\n for line breaks, we need to normalize them
     */
    private String normalizeOCRText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Replace various line break formats with single space
        text = text.replace("\r\n", " ");
        text = text.replace("\n", " ");
        text = text.replace("\r", " ");
        
        // Replace multiple spaces with single space
        text = text.replaceAll("\\s+", " ");
        
        // Convert to uppercase for easier matching
        text = text.toUpperCase();
        text = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
       .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
       .toUpperCase(Locale.ROOT);
        return text.trim();
    }

    /**
     * Parse date string to LocalDate
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Try different formats
            dateStr = dateStr.replace("/", "-");
            
            // Try DD-MM-YYYY
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-M-yyyy");
            return LocalDate.parse(dateStr, formatter);
        } catch (Exception e) {
            System.err.println("Could not parse date: " + dateStr);
            return null;
        }
    }

    /**
     * Validate image quality (basic check)
     */
    public boolean validateImageQuality(MultipartFile file) {
        try {
            // Check minimum size (at least 100KB)
            if (file.getSize() < 100 * 1024) {
                return false;
            }
            
            // Check if file is actually an image
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

