package com.ecodana.evodanavn1.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ecodana.evodanavn1.model.BankAccount;
import com.ecodana.evodanavn1.repository.BankAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class BankAccountService {

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private Cloudinary cloudinary;

    @Transactional(readOnly = true)
    public List<BankAccount> getBankAccountsByUserId(String userId) {
        return bankAccountRepository.findByUserIdOrderByCreatedDateDesc(userId);
    }

    public Optional<BankAccount> getDefaultBankAccount(String userId) {
        return bankAccountRepository.findDefaultBankAccountByUserId(userId);
    }

    @Transactional
    public BankAccount saveBankAccount(BankAccount bankAccount, MultipartFile qrCodeFile) throws IOException {
        // Generate ID if new
        if (bankAccount.getBankAccountId() == null) {
            bankAccount.setBankAccountId(UUID.randomUUID().toString());
        }

        // Handle QR code file upload
        if (qrCodeFile != null && !qrCodeFile.isEmpty()) {
            String fileName = saveQRCodeFile(qrCodeFile, bankAccount.getBankAccountId());
            bankAccount.setQrCodeImagePath(fileName);
        }

        // If this is set as default, unset other defaults
        if (bankAccount.isDefault()) {
            unsetDefaultBankAccounts(bankAccount.getUser().getId());
        }

        bankAccount.setUpdatedDate(LocalDateTime.now());
        return bankAccountRepository.save(bankAccount);
    }

    @Transactional
    public void setAsDefault(String bankAccountId, String userId) {
        // First unset all defaults for this user
        unsetDefaultBankAccounts(userId);
        
        // Set the specified account as default
        Optional<BankAccount> bankAccountOpt = bankAccountRepository.findById(bankAccountId);
        if (bankAccountOpt.isPresent()) {
            BankAccount bankAccount = bankAccountOpt.get();
            if (bankAccount.getUser().getId().equals(userId)) {
                bankAccount.setDefault(true);
                bankAccount.setUpdatedDate(LocalDateTime.now());
                bankAccountRepository.save(bankAccount);
            }
        }
    }
    
    @Transactional
    public void unsetAsDefault(String bankAccountId, String userId) {
        Optional<BankAccount> bankAccountOpt = bankAccountRepository.findById(bankAccountId);
        if (bankAccountOpt.isPresent()) {
            BankAccount bankAccount = bankAccountOpt.get();
            // Ensure the user owns this account
            if (bankAccount.getUser().getId().equals(userId)) {
                bankAccount.setDefault(false);
                bankAccount.setUpdatedDate(LocalDateTime.now());
                bankAccountRepository.save(bankAccount);
            } else {
                throw new SecurityException("User does not have permission to modify this bank account.");
            }
        } else {
            throw new IllegalArgumentException("Bank account not found with ID: " + bankAccountId);
        }
    }

    private void unsetDefaultBankAccounts(String userId) {
        List<BankAccount> userBankAccounts = bankAccountRepository.findByUserIdOrderByCreatedDateDesc(userId);
        for (BankAccount account : userBankAccounts) {
            if (account.isDefault()) {
                account.setDefault(false);
                account.setUpdatedDate(LocalDateTime.now());
                bankAccountRepository.save(account);
            }
        }
    }

    private String saveQRCodeFile(MultipartFile file, String bankAccountId) throws IOException {
        try {
            // Upload to Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "public_id", "bank-qr-" + bankAccountId + "-" + System.currentTimeMillis(),
                "folder", "ecodana/bank-qr-codes",
                "resource_type", "auto"
            ));
            
            // Return the secure URL
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            System.err.println("Error uploading QR code to Cloudinary: " + e.getMessage());
            throw new IOException("Failed to upload QR code to Cloudinary", e);
        }
    }

    public void deleteBankAccount(String bankAccountId, String userId) {
        Optional<BankAccount> bankAccountOpt = bankAccountRepository.findById(bankAccountId);
        if (bankAccountOpt.isPresent()) {
            BankAccount bankAccount = bankAccountOpt.get();
            if (bankAccount.getUser().getId().equals(userId)) {
                // Note: Cloudinary files are automatically managed by Cloudinary
                // No need to manually delete from local storage
                bankAccountRepository.delete(bankAccount);
            }
        }
    }

    public Optional<BankAccount> getBankAccountById(String bankAccountId) {
        return bankAccountRepository.findById(bankAccountId);
    }
}
