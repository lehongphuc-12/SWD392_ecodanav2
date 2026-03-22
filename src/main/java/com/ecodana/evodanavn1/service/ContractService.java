package com.ecodana.evodanavn1.service;

import com.ecodana.evodanavn1.model.Booking;
import com.ecodana.evodanavn1.model.Contract;
import com.ecodana.evodanavn1.model.Contract.ContractStatus;
import com.ecodana.evodanavn1.model.User;
import com.ecodana.evodanavn1.repository.ContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ContractService {

    @Autowired
    private ContractRepository contractRepository;

    public List<Contract> getAllContracts() {
        return contractRepository.findAllWithDetails();
    }

    public Optional<Contract> getContractById(String contractId) {
        return contractRepository.findByIdWithDetails(contractId);
    }

    public Optional<Contract> getContractByCode(String contractCode) {
        return contractRepository.findByContractCode(contractCode);
    }

    public Optional<Contract> getContractByBookingId(String bookingId) {
        return contractRepository.findByBooking_BookingId(bookingId);
    }

    public List<Contract> getContractsByUserId(String userId) {
        return contractRepository.findByUser_Id(userId);
    }

    public List<Contract> getContractsByStatus(ContractStatus status) {
        return contractRepository.findByStatus(status);
    }

    public List<Contract> searchContracts(String searchTerm) {
        return contractRepository.searchContracts(searchTerm);
    }

    public Contract createContract(User user, Booking booking) {
        Contract contract = new Contract();
        contract.setContractId(UUID.randomUUID().toString());
        contract.setContractCode(generateContractCode());
        contract.setUser(user);
        contract.setBooking(booking);
        contract.setCreatedDate(LocalDateTime.now());
        contract.setStatus(ContractStatus.Draft);
        contract.setTermsAccepted(false);
        
        return contractRepository.save(contract);
    }

    public Contract saveContract(Contract contract) {
        if (contract.getContractId() == null || contract.getContractId().isEmpty()) {
            contract.setContractId(UUID.randomUUID().toString());
        }
        if (contract.getContractCode() == null || contract.getContractCode().isEmpty()) {
            contract.setContractCode(generateContractCode());
        }
        if (contract.getCreatedDate() == null) {
            contract.setCreatedDate(LocalDateTime.now());
        }
        return contractRepository.save(contract);
    }

    public Contract updateContract(Contract contract) {
        return contractRepository.save(contract);
    }

    public void deleteContract(String contractId) {
        contractRepository.deleteById(contractId);
    }

    public Contract signContract(String contractId, String signatureData, String signatureMethod) {
        Optional<Contract> contractOpt = contractRepository.findById(contractId);
        if (contractOpt.isPresent()) {
            Contract contract = contractOpt.get();
            contract.setSignatureData(signatureData);
            contract.setSignatureMethod(signatureMethod);
            contract.setSignedDate(LocalDateTime.now());
            contract.setStatus(ContractStatus.Signed);
            contract.setTermsAccepted(true);
            return contractRepository.save(contract);
        }
        throw new RuntimeException("Contract not found with ID: " + contractId);
    }

    public Contract completeContract(String contractId) {
        Optional<Contract> contractOpt = contractRepository.findById(contractId);
        if (contractOpt.isPresent()) {
            Contract contract = contractOpt.get();
            contract.setCompletedDate(LocalDateTime.now());
            contract.setStatus(ContractStatus.Completed);
            return contractRepository.save(contract);
        }
        throw new RuntimeException("Contract not found with ID: " + contractId);
    }

    public Contract cancelContract(String contractId, String reason) {
        Optional<Contract> contractOpt = contractRepository.findById(contractId);
        if (contractOpt.isPresent()) {
            Contract contract = contractOpt.get();
            contract.setCancellationReason(reason);
            contract.setStatus(ContractStatus.Cancelled);
            return contractRepository.save(contract);
        }
        throw new RuntimeException("Contract not found with ID: " + contractId);
    }

    public long countContractsByStatus(ContractStatus status) {
        return contractRepository.countByStatus(status);
    }

    public List<Contract> getContractsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return contractRepository.findByCreatedDateBetween(startDate, endDate);
    }

    public List<Contract> getSignedContractsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return contractRepository.findBySignedDateBetween(startDate, endDate);
    }

    private String generateContractCode() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "CT-" + timestamp + "-" + randomPart;
    }

    public long getTotalContracts() {
        return contractRepository.count();
    }

    public long getDraftContracts() {
        return countContractsByStatus(ContractStatus.Draft);
    }

    public long getSignedContracts() {
        return countContractsByStatus(ContractStatus.Signed);
    }

    public long getCompletedContracts() {
        return countContractsByStatus(ContractStatus.Completed);
    }

    public long getCancelledContracts() {
        return countContractsByStatus(ContractStatus.Cancelled);
    }
}
