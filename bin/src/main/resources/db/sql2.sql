-- =============================================
-- DATABASE: ecodanav2
-- HỆ QUẢN TRỊ: MySQL
-- PHIÊNBẢN TỐI ƯU HÓA (ĐÃ ĐỒNG BỘ ĐỂ CHẠY LẠI)
-- =============================================

-- USE ecodanangv2;

-- Vô hiệu hóa các kiểm tra để đảm bảo việc xóa và tạo bảng diễn ra suôn sẻ
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

-- =============================================
-- SECTION 1: CÁC BẢNG TRA CỨU (LOOKUP TABLES)
-- =============================================

DROP TABLE IF EXISTS `Roles`;
CREATE TABLE `Roles` (
                         `RoleId` char(36) NOT NULL,
                         `RoleName` varchar(50) DEFAULT NULL,
                         `NormalizedName` varchar(256) DEFAULT NULL,
                         PRIMARY KEY (`RoleId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `VehicleCategories`;
CREATE TABLE `VehicleCategories` (
                                     `CategoryId` int NOT NULL AUTO_INCREMENT,
                                     `CategoryName` varchar(100) NOT NULL,
                                     PRIMARY KEY (`CategoryId`),
                                     UNIQUE KEY `CategoryName` (`CategoryName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `TransmissionTypes`;
CREATE TABLE `TransmissionTypes` (
                                     `TransmissionTypeId` int NOT NULL AUTO_INCREMENT,
                                     `TransmissionTypeName` varchar(100) NOT NULL,
                                     PRIMARY KEY (`TransmissionTypeId`),
                                     UNIQUE KEY `TransmissionTypeName` (`TransmissionTypeName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =============================================
-- SECTION 2: CÁC BẢNG CỐT LÕI (CORE TABLES)
-- =============================================

DROP TABLE IF EXISTS `Users`;
CREATE TABLE `Users` (
                         `UserId` char(36) NOT NULL,
                         `Username` varchar(100) NOT NULL,
                         `FirstName` varchar(256) DEFAULT NULL,
                         `LastName` varchar(256) DEFAULT NULL,
                         `UserDOB` date DEFAULT NULL,
                         `PhoneNumber` varchar(15) DEFAULT NULL,
                         `AvatarUrl` varchar(255) DEFAULT NULL,
                         `Gender` ENUM('Male', 'Female', 'Other') DEFAULT NULL,
                         `Status` ENUM('Active', 'Inactive', 'Banned') NOT NULL DEFAULT 'Active',
                         `RoleId` char(36) NOT NULL,
                         `Email` varchar(100) NOT NULL,
                         `EmailVerifed` tinyint(1) NOT NULL DEFAULT '0',
                         `PasswordHash` varchar(255) NOT NULL,
                         `CreatedDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         `NormalizedUserName` varchar(256) DEFAULT NULL,
                         `NormalizedEmail` varchar(256) DEFAULT NULL,
                         `SecurityStamp` text,
                         `ConcurrencyStamp` text,
                         `TwoFactorEnabled` tinyint(1) NOT NULL DEFAULT '0',
                         `LockoutEnd` datetime DEFAULT NULL,
                         `LockoutEnabled` tinyint(1) NOT NULL DEFAULT '0',
                         `AccessFailedCount` int NOT NULL DEFAULT '0',
                         PRIMARY KEY (`UserId`),
                         UNIQUE KEY `Username` (`Username`),
                         UNIQUE KEY `Email` (`Email`),
                         KEY `RoleId` (`RoleId`),
                         KEY `idx_user_email` (`Email`),
                         KEY `idx_user_status` (`Status`),
                         CONSTRAINT `users_ibfk_1` FOREIGN KEY (`RoleId`) REFERENCES `Roles` (`RoleId`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `UserDocuments`;
CREATE TABLE `UserDocuments` (
                                 `DocumentId` char(36) NOT NULL,
                                 `UserId` char(36) NOT NULL,
                                 `DocumentType` ENUM('CitizenId', 'DriverLicense', 'Passport') NOT NULL,
                                 `DocumentNumber` varchar(50) NOT NULL,
                                 `FullName` varchar(100) DEFAULT NULL,
                                 `DOB` date DEFAULT NULL,
                                 `IssuedDate` date DEFAULT NULL,
                                 `IssuedPlace` varchar(100) DEFAULT NULL,
                                 `FrontImageUrl` varchar(500) DEFAULT NULL,
                                 `BackImageUrl` varchar(500) DEFAULT NULL,
                                 `IsVerified` tinyint(1) NOT NULL DEFAULT '0',
                                 `CreatedDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 PRIMARY KEY (`DocumentId`),
                                 KEY `UserId` (`UserId`),
                                 CONSTRAINT `userdocuments_ibfk_1` FOREIGN KEY (`UserId`) REFERENCES `Users` (`UserId`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `Vehicle`;
CREATE TABLE `Vehicle` (
                           `VehicleId` char(36) NOT NULL,
                           `VehicleModel` varchar(50) NOT NULL COMMENT 'Ví dụ: VF8, Feliz S',
                           `YearManufactured` int DEFAULT NULL,
                           `LicensePlate` varchar(20) NOT NULL,
                           `Seats` int NOT NULL,
                           `Odometer` int NOT NULL,
                           `RentalPrices` JSON DEFAULT NULL COMMENT 'Lưu giá dạng JSON: {"hourly": 50000, "daily": 500000, "monthly": 10000000}',
                           `Status` ENUM('Available', 'Rented', 'Maintenance', 'Unavailable') NOT NULL DEFAULT 'Available',
                           `Description` varchar(500) DEFAULT NULL,
                           `VehicleType` ENUM('ElectricCar', 'ElectricMotorcycle') NOT NULL,
                           `RequiresLicense` tinyint(1) NOT NULL DEFAULT '1',
                           `BatteryCapacity` decimal(10,2) DEFAULT NULL,
                           `CreatedDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           `LastUpdatedBy` char(36) DEFAULT NULL,
                           `CategoryId` int DEFAULT NULL,
                           `TransmissionTypeId` int DEFAULT NULL,
                           `MainImageUrl` varchar(255) DEFAULT NULL,
                           `ImageUrls` JSON DEFAULT NULL COMMENT 'Lưu dạng mảng JSON: ["url1", "url2"]',
                           `Features` JSON DEFAULT NULL COMMENT 'Lưu dạng mảng JSON: ["GPS", "Camera 360"]',
                           PRIMARY KEY (`VehicleId`),
                           KEY `LastUpdatedBy` (`LastUpdatedBy`),
                           KEY `CategoryId` (`CategoryId`),
                           KEY `TransmissionTypeId` (`TransmissionTypeId`),
                           KEY `idx_vehicle_type` (`VehicleType`),
                           KEY `idx_license_plate` (`LicensePlate`),
                           KEY `idx_vehicle_status` (`Status`),
                           CONSTRAINT `vehicle_ibfk_1` FOREIGN KEY (`LastUpdatedBy`) REFERENCES `Users` (`UserId`) ON DELETE SET NULL,
                           CONSTRAINT `vehicle_ibfk_2` FOREIGN KEY (`CategoryId`) REFERENCES `VehicleCategories` (`CategoryId`) ON DELETE SET NULL,
                           CONSTRAINT `vehicle_ibfk_3` FOREIGN KEY (`TransmissionTypeId`) REFERENCES `TransmissionTypes` (`TransmissionTypeId`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `Discount`;
CREATE TABLE `Discount` (
                            `DiscountId` char(36) NOT NULL,
                            `DiscountName` varchar(100) NOT NULL,
                            `Description` varchar(255) DEFAULT NULL,
                            `DiscountType` ENUM('Percentage', 'FixedAmount') NOT NULL,
                            `DiscountValue` decimal(10,2) NOT NULL,
                            `StartDate` date NOT NULL,
                            `EndDate` date NOT NULL,
                            `IsActive` tinyint(1) NOT NULL,
                            `CreatedDate` datetime NOT NULL,
                            `VoucherCode` varchar(20) DEFAULT NULL,
                            `MinOrderAmount` decimal(10,2) NOT NULL,
                            `MaxDiscountAmount` decimal(10,2) DEFAULT NULL,
                            `UsageLimit` int DEFAULT NULL,
                            `UsedCount` int NOT NULL,
                            `DiscountCategory` varchar(20) NOT NULL DEFAULT 'General',
                            PRIMARY KEY (`DiscountId`),
                            UNIQUE KEY `VoucherCode` (`VoucherCode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `Booking`;
CREATE TABLE `Booking` (
                           `BookingId` char(36) NOT NULL,
                           `UserId` char(36) NOT NULL,
                           `VehicleId` char(36) NOT NULL,
                           `HandledBy` char(36) DEFAULT NULL,
                           `PickupDateTime` datetime NOT NULL,
                           `ReturnDateTime` datetime NOT NULL,
                           `TotalAmount` decimal(10,2) NOT NULL,
                           `Status` ENUM('Pending', 'Approved', 'Rejected', 'Ongoing', 'Completed', 'Cancelled', 'AwaitingDeposit', 'Confirmed', 'RefundPending', 'LatePickup', 'NoShow') NOT NULL DEFAULT 'Pending',
                           `DiscountId` char(36) DEFAULT NULL,
                           `CreatedDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           `CancelReason` varchar(500) DEFAULT NULL,
                           `CancellationType` varchar(50) DEFAULT NULL,
                           `BookingCode` varchar(20) NOT NULL,
                           `ExpectedPaymentMethod` varchar(50) DEFAULT NULL,
                           `RentalType` ENUM('hourly', 'daily', 'monthly') NOT NULL DEFAULT 'daily',
                           `TermsAgreed` tinyint(1) NOT NULL DEFAULT '0',
                           `TermsAgreedAt` datetime DEFAULT NULL,
                           `TermsVersion` varchar(10) DEFAULT 'v1.0',
                           PRIMARY KEY (`BookingId`),
                           UNIQUE KEY `BookingCode` (`BookingCode`),
                           KEY `UserId` (`UserId`),
                           KEY `VehicleId` (`VehicleId`),
                           KEY `HandledBy` (`HandledBy`),
                           KEY `DiscountId` (`DiscountId`),
                           KEY `idx_booking_dates_status` (`PickupDateTime`, `ReturnDateTime`, `Status`),
                           CONSTRAINT `booking_ibfk_1` FOREIGN KEY (`UserId`) REFERENCES `Users` (`UserId`) ON DELETE CASCADE,
                           CONSTRAINT `booking_ibfk_2` FOREIGN KEY (`VehicleId`) REFERENCES `Vehicle` (`VehicleId`) ON DELETE RESTRICT,
                           CONSTRAINT `booking_ibfk_3` FOREIGN KEY (`HandledBy`) REFERENCES `Users` (`UserId`),
                           CONSTRAINT `booking_ibfk_4` FOREIGN KEY (`DiscountId`) REFERENCES `Discount` (`DiscountId`) ON DELETE SET NULL,
                           CONSTRAINT `CHK_Booking_Amount` CHECK ((`TotalAmount` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `Contract`;
CREATE TABLE `Contract` (
                            `ContractId` char(36) NOT NULL,
                            `ContractCode` varchar(30) NOT NULL,
                            `UserId` char(36) NOT NULL,
                            `BookingId` char(36) NOT NULL,
                            `CreatedDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            `SignedDate` datetime DEFAULT NULL,
                            `CompletedDate` datetime DEFAULT NULL,
                            `Status` ENUM('Draft', 'Signed', 'Completed', 'Cancelled') NOT NULL DEFAULT 'Draft',
                            `TermsAccepted` tinyint(1) NOT NULL DEFAULT '0',
                            `SignatureData` text,
                            `SignatureMethod` varchar(20) DEFAULT NULL,
                            `ContractPdfUrl` varchar(500) DEFAULT NULL,
                            `Notes` varchar(500) DEFAULT NULL,
                            `CancellationReason` varchar(500) DEFAULT NULL,
                            `CitizenIdSnapshotId` char(36) DEFAULT NULL,
                            `DriverLicenseSnapshotId` char(36) DEFAULT NULL,
                            PRIMARY KEY (`ContractId`),
                            UNIQUE KEY `ContractCode` (`ContractCode`),
                            KEY `UserId` (`UserId`),
                            KEY `BookingId` (`BookingId`),
                            KEY `idx_contract_status` (`Status`),
                            CONSTRAINT `contract_ibfk_1` FOREIGN KEY (`UserId`) REFERENCES `Users` (`UserId`) ON DELETE CASCADE,
                            CONSTRAINT `contract_ibfk_2` FOREIGN KEY (`BookingId`) REFERENCES `Booking` (`BookingId`),
                            CONSTRAINT `contract_ibfk_3` FOREIGN KEY (`CitizenIdSnapshotId`) REFERENCES `UserDocuments` (`DocumentId`) ON DELETE SET NULL,
                            CONSTRAINT `contract_ibfk_4` FOREIGN KEY (`DriverLicenseSnapshotId`) REFERENCES `UserDocuments` (`DocumentId`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `Payment`;
CREATE TABLE `Payment` (
                           `PaymentId` char(36) NOT NULL,
                           `BookingId` char(36) NOT NULL,
                           `ContractId` char(36) DEFAULT NULL,
                           `Amount` decimal(10,2) NOT NULL,
                           `PaymentMethod` varchar(50) NOT NULL,
                           `PaymentStatus` ENUM('Pending', 'Completed', 'Failed', 'Refunded') NOT NULL DEFAULT 'Pending',
                           `PaymentType` ENUM('Deposit', 'FinalPayment', 'Surcharge', 'Refund') NOT NULL DEFAULT 'Deposit',
                           `TransactionId` varchar(100) DEFAULT NULL,
                           `PaymentDate` datetime DEFAULT NULL,
                           `UserId` char(36) DEFAULT NULL,
                           `Notes` varchar(500) DEFAULT NULL,
                           `CreatedDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           PRIMARY KEY (`PaymentId`),
                           KEY `BookingId` (`BookingId`),
                           KEY `ContractId` (`ContractId`),
                           KEY `UserId` (`UserId`),
                           CONSTRAINT `payment_ibfk_1` FOREIGN KEY (`BookingId`) REFERENCES `Booking` (`BookingId`) ON DELETE CASCADE,
                           CONSTRAINT `payment_ibfk_2` FOREIGN KEY (`ContractId`) REFERENCES `Contract` (`ContractId`),
                           CONSTRAINT `payment_ibfk_3` FOREIGN KEY (`UserId`) REFERENCES `Users` (`UserId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `BookingApproval`;
CREATE TABLE `BookingApproval` (
                                   `ApprovalId` char(36) NOT NULL,
                                   `BookingId` char(36) NOT NULL,
                                   `StaffId` char(36) NOT NULL,
                                   `ApprovalStatus` ENUM('Approved', 'Rejected') NOT NULL,
                                   `ApprovalDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   `Note` varchar(500) DEFAULT NULL,
                                   `RejectionReason` varchar(500) DEFAULT NULL,
                                   PRIMARY KEY (`ApprovalId`),
                                   KEY `BookingId` (`BookingId`),
                                   KEY `StaffId` (`StaffId`),
                                   CONSTRAINT `bookingapproval_ibfk_1` FOREIGN KEY (`BookingId`) REFERENCES `Booking` (`BookingId`) ON DELETE CASCADE,
                                   CONSTRAINT `bookingapproval_ibfk_2` FOREIGN KEY (`StaffId`) REFERENCES `Users` (`UserId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `BookingSurcharges`;
CREATE TABLE `BookingSurcharges` (
                                     `SurchargeId` char(36) NOT NULL,
                                     `BookingId` char(36) NOT NULL,
                                     `SurchargeType` varchar(50) NOT NULL,
                                     `Amount` decimal(10,2) NOT NULL,
                                     `Description` varchar(255) DEFAULT NULL,
                                     `CreatedDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     `SurchargeCategory` varchar(50) DEFAULT NULL,
                                     `IsSystemGenerated` tinyint(1) NOT NULL DEFAULT '0',
                                     PRIMARY KEY (`SurchargeId`),
                                     KEY `BookingId` (`BookingId`),
                                     CONSTRAINT `bookingsurcharges_ibfk_1` FOREIGN KEY (`BookingId`) REFERENCES `Booking` (`BookingId`) ON DELETE CASCADE,
                                     CONSTRAINT `CHK_BookingSurcharges_Amount` CHECK ((`Amount` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =============================================
-- SECTION 3: CÁC BẢNG PHỤ TRỢ (SUPPORTING TABLES)
-- =============================================

DROP TABLE IF EXISTS `UserFeedback`;
CREATE TABLE `UserFeedback` (
                                `FeedbackId` char(36) NOT NULL,
                                `UserId` char(36) NOT NULL,
                                `VehicleId` char(36) DEFAULT NULL,
                                `BookingId` char(36) DEFAULT NULL,
                                `Rating` int NOT NULL,
                                `Content` varchar(4000) DEFAULT NULL,
                                `Reviewed` date NOT NULL,
                                `CreatedDate` datetime NOT NULL,
                                `StaffReply` text,
                                `ReplyDate` datetime DEFAULT NULL,
                                PRIMARY KEY (`FeedbackId`),
                                KEY `UserId` (`UserId`),
                                KEY `VehicleId` (`VehicleId`),
                                KEY `BookingId` (`BookingId`),
                                CONSTRAINT `userfeedback_ibfk_1` FOREIGN KEY (`UserId`) REFERENCES `Users` (`UserId`) ON DELETE CASCADE,
                                CONSTRAINT `userfeedback_ibfk_2` FOREIGN KEY (`VehicleId`) REFERENCES `Vehicle` (`VehicleId`) ON DELETE SET NULL,
                                CONSTRAINT `userfeedback_ibfk_3` FOREIGN KEY (`BookingId`) REFERENCES `Booking` (`BookingId`),
                                CONSTRAINT `CHK_Rating_Range` CHECK ((`Rating` BETWEEN 1 AND 5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `VehicleConditionLogs`;
CREATE TABLE `VehicleConditionLogs` (
                                        `LogId` char(36) NOT NULL,
                                        `BookingId` char(36) NOT NULL,
                                        `VehicleId` char(36) NOT NULL,
                                        `StaffId` char(36) DEFAULT NULL,
                                        `CheckType` ENUM('Pickup', 'Return') NOT NULL,
                                        `CheckTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        `Odometer` int DEFAULT NULL,
                                        `FuelLevel` varchar(20) DEFAULT NULL,
                                        `ConditionStatus` varchar(100) DEFAULT NULL,
                                        `ConditionDescription` varchar(1000) DEFAULT NULL,
                                        `DamageImages` JSON,
                                        `Note` varchar(255) DEFAULT NULL,
                                        PRIMARY KEY (`LogId`),
                                        KEY `BookingId` (`BookingId`),
                                        KEY `VehicleId` (`VehicleId`),
                                        KEY `StaffId` (`StaffId`),
                                        CONSTRAINT `vehicleconditionlogs_ibfk_1` FOREIGN KEY (`BookingId`) REFERENCES `Booking` (`BookingId`) ON DELETE CASCADE,
                                        CONSTRAINT `vehicleconditionlogs_ibfk_2` FOREIGN KEY (`VehicleId`) REFERENCES `Vehicle` (`VehicleId`) ON DELETE CASCADE,
                                        CONSTRAINT `vehicleconditionlogs_ibfk_3` FOREIGN KEY (`StaffId`) REFERENCES `Users` (`UserId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `BatteryLogs`;
CREATE TABLE `BatteryLogs` (
                               `LogId` char(36) NOT NULL,
                               `VehicleId` char(36) NOT NULL,
                               `BookingId` char(36) DEFAULT NULL,
                               `BatteryLevel` decimal(5,2) NOT NULL,
                               `CheckTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               `Note` varchar(255) DEFAULT NULL,
                               PRIMARY KEY (`LogId`),
                               KEY `VehicleId` (`VehicleId`),
                               KEY `BookingId` (`BookingId`),
                               CONSTRAINT `batterylogs_ibfk_1` FOREIGN KEY (`VehicleId`) REFERENCES `Vehicle` (`VehicleId`) ON DELETE CASCADE,
                               CONSTRAINT `batterylogs_ibfk_2` FOREIGN KEY (`BookingId`) REFERENCES `Booking` (`BookingId`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

# DROP TABLE IF EXISTS `EmailOTPVerification`;
# CREATE TABLE `EmailOTPVerification` (
#                                         `Id` char(36) NOT NULL,
#                                         `OTP` varchar(255) NOT NULL,
#                                         `ExpiryTime` datetime NOT NULL,
#                                         `IsUsed` tinyint(1) NOT NULL,
#                                         `UserId` char(36) NOT NULL,
#                                         `CreatedAt` datetime NOT NULL,
#                                         `ResendCount` int NOT NULL,
#                                         `LastResendTime` datetime DEFAULT NULL,
#                                         `ResendBlockUntil` datetime DEFAULT NULL,
#                                         PRIMARY KEY (`Id`),
#                                         KEY `UserId` (`UserId`),
#                                         CONSTRAINT `emailotpverification_ibfk_1` FOREIGN KEY (`UserId`) REFERENCES `Users` (`UserId`) ON DELETE CASCADE
# ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `PasswordResetTokens`;
CREATE TABLE `PasswordResetTokens` (
                                       `Id` char(36) NOT NULL,
                                       `Token` varchar(255) NOT NULL,
                                       `ExpiryTime` datetime NOT NULL,
                                       `IsUsed` tinyint(1) NOT NULL DEFAULT '0',
                                       `UserId` char(36) NOT NULL,
                                       `CreatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       PRIMARY KEY (`Id`),
                                       KEY `UserId` (`UserId`),
                                       CONSTRAINT `passwordresettokens_ibfk_1` FOREIGN KEY (`UserId`) REFERENCES `Users` (`UserId`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `UserFavoriteVehicles`;
CREATE TABLE `UserFavoriteVehicles` (
                                        `UserId` char(36) NOT NULL,
                                        `VehicleId` char(36) NOT NULL,
                                        PRIMARY KEY (`UserId`,`VehicleId`),
                                        KEY `VehicleId` (`VehicleId`),
                                        CONSTRAINT `userfavoritevehicles_ibfk_1` FOREIGN KEY (`UserId`) REFERENCES `Users` (`UserId`) ON DELETE CASCADE,
                                        CONSTRAINT `userfavoritevehicles_ibfk_2` FOREIGN KEY (`VehicleId`) REFERENCES `Vehicle` (`VehicleId`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `UserLogins`;
CREATE TABLE `UserLogins` (
                              `LoginProvider` varchar(128) NOT NULL,
                              `ProviderKey` varchar(128) NOT NULL,
                              `ProviderDisplayName` text,
                              `UserId` char(36) NOT NULL,
                              PRIMARY KEY (`LoginProvider`,`ProviderKey`),
                              KEY `UserId` (`UserId`),
                              CONSTRAINT `userlogins_ibfk_1` FOREIGN KEY (`UserId`) REFERENCES `Users` (`UserId`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `UserVoucherUsage`;
CREATE TABLE `UserVoucherUsage` (
                                    `UserId` char(36) NOT NULL,
                                    `DiscountId` char(36) NOT NULL,
                                    `UsedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    PRIMARY KEY (`UserId`,`DiscountId`),
                                    KEY `DiscountId` (`DiscountId`),
                                    CONSTRAINT `uservoucherusage_ibfk_1` FOREIGN KEY (`UserId`) REFERENCES `Users` (`UserId`) ON DELETE CASCADE,
                                    CONSTRAINT `uservoucherusage_ibfk_2` FOREIGN KEY (`DiscountId`) REFERENCES `Discount` (`DiscountId`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `Notification`;
CREATE TABLE `Notification` (
                                `NotificationId` char(36) NOT NULL,
                                `UserId` char(36) NOT NULL,
                                `Message` text NOT NULL,
                                `CreatedDate` datetime NOT NULL,
                                `IsRead` tinyint(1) NOT NULL,
                                `RelatedId` char(36) DEFAULT NULL COMMENT 'ID của booking, payment, etc.',
                                `NotificationType` varchar(50) DEFAULT NULL COMMENT '"BOOKING", "PAYMENT", "CONTRACT", etc.',
                                PRIMARY KEY (`NotificationId`),
                                KEY `UserId` (`UserId`),
                                CONSTRAINT `notification_ibfk_1` FOREIGN KEY (`UserId`) REFERENCES `Users` (`UserId`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `AccountDeletionLogs`;
CREATE TABLE `AccountDeletionLogs` (
                                       `LogId` char(36) NOT NULL,
                                       `UserId` char(36) NOT NULL,
                                       `DeletionReason` varchar(255) NOT NULL,
                                       `AdditionalComments` text,
                                       `Timestamp` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       PRIMARY KEY (`LogId`),
                                       KEY `UserId` (`UserId`),
                                       CONSTRAINT `accountdeletionlogs_ibfk_1` FOREIGN KEY (`UserId`) REFERENCES `Users` (`UserId`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `Terms`;
CREATE TABLE `Terms` (
                         `TermsId` char(36) NOT NULL,
                         `Version` varchar(10) NOT NULL,
                         `Title` varchar(200) NOT NULL,
                         `ShortContent` text,
                         `FullContent` text NOT NULL,
                         `EffectiveDate` date NOT NULL,
                         `IsActive` tinyint(1) NOT NULL DEFAULT '1',
                         `CreatedDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         PRIMARY KEY (`TermsId`),
                         UNIQUE KEY `Version` (`Version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `InappropriateWord`;
CREATE TABLE `InappropriateWord` (
                                     `Id` char(36) NOT NULL,
                                     `Word` varchar(255) NOT NULL,
                                     `Category` varchar(100) DEFAULT NULL,
                                     `Severity` ENUM('LOW', 'MEDIUM', 'HIGH') NOT NULL DEFAULT 'MEDIUM',
                                     `IsActive` tinyint(1) NOT NULL DEFAULT '1',
                                     `CreatedDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     PRIMARY KEY (`Id`),
                                     UNIQUE KEY `Word` (`Word`),
                                     KEY `idx_inappropriate_word_active` (`IsActive`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =============================================
-- SECTION 4: CÁC BẢNG BỔ SUNG (ĐÃ ĐỒNG BỘ)
-- =============================================

DROP TABLE IF EXISTS `BankAccount`;
DROP TABLE IF EXISTS `RefundRequest`;
DROP TABLE IF EXISTS `FeedbackReport`;


-- Khôi phục lại các thiết lập ban đầu
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- 1. Mở rộng trạng thái của bảng 'Booking' để theo dõi quy trình đặt cọc
ALTER TABLE `Booking`
    MODIFY COLUMN `Status` ENUM(
        'Pending',          -- Khách vừa tạo, chờ chủ xe duyệt
        'Approved',         -- Chủ xe đã duyệt (trạng thái trung gian)
        'AwaitingDeposit',  -- Đã duyệt, chờ khách thanh toán 20% cọc
        'Confirmed',        -- Khách đã thanh toán cọc, đơn đã chắc chắn
        'Rejected',         -- Chủ xe từ chối
        'Ongoing',          -- Đang trong quá trình thuê (đã nhận xe)
        'Completed',        -- Đã hoàn tất chuyến đi và thanh toán
        'Cancelled'         -- Đơn bị hủy
        ) NOT NULL DEFAULT 'Pending';


-- 2. Thêm cột 'OwnerId' vào bảng 'Vehicle' để xác định "người cho thuê"
-- Điều này rất quan trọng cho mô hình P2P (peer-to-peer)
ALTER TABLE `Vehicle`
    ADD COLUMN `OwnerId` CHAR(36) NULL COMMENT 'ID của User là chủ sở hữu xe' AFTER `LastUpdatedBy`,
    ADD CONSTRAINT `fk_vehicle_owner` FOREIGN KEY (`OwnerId`) REFERENCES `Users` (`UserId`) ON DELETE SET NULL;


-- 3. Thêm các cột theo dõi tiền cọc và số tiền còn lại vào bảng 'Booking'
ALTER TABLE `Booking`
    ADD COLUMN `DepositAmountRequired` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Số tiền cọc 20% cần thanh toán' AFTER `TotalAmount`,
    ADD COLUMN `RemainingAmount` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Số tiền 80% còn lại thanh toán khi nhận xe' AFTER `DepositAmountRequired`;

-- =============================================
-- CẬP NHẬT BẢNG BOOKING (THÊM PHÍ NỀN TẢNG)
-- =============================================
ALTER TABLE `Booking`
    ADD COLUMN `VehicleRentalFee` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Tiền thuê xe gốc' AFTER `ReturnDateTime`,
    ADD COLUMN `PlatformFee` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Phí nền tảng (cho EcoDana)' AFTER `VehicleRentalFee`,
    ADD COLUMN `OwnerPayout` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Tiền chủ xe nhận (RentalFee - PlatformFee)' AFTER `PlatformFee`,
    ADD COLUMN `PaymentConfirmedAt` DATETIME NULL COMMENT 'Thời điểm thanh toán cọc/toàn bộ thành công' AFTER `RemainingAmount`;

-- Add OrderCode column to Payment table
ALTER TABLE Payment ADD COLUMN OrderCode VARCHAR(100) NULL AFTER UserId;

-- Add index for better query performance
CREATE INDEX idx_payment_ordercode ON Payment(OrderCode);


-- Create BankAccount table
CREATE TABLE `BankAccount` (
                               `BankAccountId` VARCHAR(36) PRIMARY KEY,
                               `UserId` VARCHAR(36) NOT NULL,
                               `AccountNumber` VARCHAR(50) NOT NULL,
                               `AccountHolderName` VARCHAR(100) NOT NULL,
                               `BankName` VARCHAR(100) NOT NULL,
                               `BankCode` VARCHAR(20),
                               `QRCodeImagePath` VARCHAR(500),
                               `IsDefault` BOOLEAN NOT NULL DEFAULT FALSE,
                               `CreatedDate` DATETIME NOT NULL,
                               `UpdatedDate` DATETIME,
                               FOREIGN KEY (`UserId`) REFERENCES `Users` (`UserId`) ON DELETE CASCADE
);

-- Create RefundRequest table
CREATE TABLE `RefundRequest` (
                                 `RefundRequestId` VARCHAR(36) PRIMARY KEY,
                                 `BookingId` VARCHAR(36) NOT NULL,
                                 `UserId` VARCHAR(36) NOT NULL,
                                 `BankAccountId` VARCHAR(36) NOT NULL,
                                 `RefundAmount` DECIMAL(10,2) NOT NULL,
                                 `CancelReason` TEXT NOT NULL,
                                 `Status` VARCHAR(20) NOT NULL DEFAULT 'Pending',
                                 `AdminNotes` TEXT,
                                 `ProcessedBy` VARCHAR(36),
                                 `CreatedDate` DATETIME NOT NULL,
                                 `ProcessedDate` DATETIME,
                                 `IsWithinTwoHours` BOOLEAN NOT NULL DEFAULT FALSE,
                                 FOREIGN KEY (`BookingId`) REFERENCES `Booking` (`BookingId`),
                                 FOREIGN KEY (`UserId`) REFERENCES `Users` (`UserId`) ON DELETE CASCADE,
                                 FOREIGN KEY (`BankAccountId`) REFERENCES `BankAccount` (`BankAccountId`)
);

-- Create indexes for better performance
CREATE INDEX idx_bankaccount_userid ON BankAccount(UserId);
CREATE INDEX idx_bankaccount_isdefault ON BankAccount(IsDefault);
CREATE INDEX idx_refundrequest_bookingid ON RefundRequest(BookingId);
CREATE INDEX idx_refundrequest_userid ON RefundRequest(UserId);
CREATE INDEX idx_refundrequest_status ON RefundRequest(Status);
CREATE INDEX idx_refundrequest_createddate ON RefundRequest(CreatedDate);


-- Thêm dữ liệu cho loại hộp số
INSERT INTO `TransmissionTypes` (`TransmissionTypeId`, `TransmissionTypeName`)
VALUES
    (1, 'Automatic')
ON DUPLICATE KEY UPDATE TransmissionTypeName=VALUES(TransmissionTypeName);

-- Thêm dữ liệu cho các danh mục xe
INSERT INTO `VehicleCategories` (`CategoryId`, `CategoryName`)
VALUES
    (1, 'Electric Car'),
    (2, 'Electric Motorbike')
ON DUPLICATE KEY UPDATE CategoryName=VALUES(CategoryName);

ALTER TABLE Vehicle MODIFY COLUMN Status VARCHAR(20);

UPDATE Vehicle
SET Status = 'Available'
WHERE Status IS NULL OR Status = '' OR Status NOT IN ('PendingApproval', 'Available', 'Rented', 'Maintenance', 'Unavailable');

-- Thêm cột địa điểm giao xe
ALTER TABLE `Booking`
    ADD COLUMN `PickupLocation` VARCHAR(500) NULL COMMENT 'Địa điểm giao xe khách hàng đã chọn' AFTER `ReturnDateTime`;

create table `FeedbackReport`
(
    `ReportId`    varchar(36)                  not null
        primary key,
    `CreatedDate` datetime(6)                  not null,
    `Reason`      varchar(1000)                null,
    `Status`      enum ('Pending', 'Resolved') not null,
    `FeedbackId`  varchar(36)                  not null,
    `ReporterId`  varchar(36)                  not null,
    constraint `FK1griu1vmthuss7auxrqnxal4p`
        foreign key (`FeedbackId`) references `UserFeedback` (`FeedbackId`)
            on delete cascade,
    constraint `FK7s63wl5nvqjox2t68ma3ftqy9`
        foreign key (`ReporterId`) references `Users` (`UserId`)
            on delete cascade
);


ALTER TABLE Booking
    MODIFY COLUMN Status ENUM(
        'Pending',
        'Approved',
        'AwaitingDeposit',
        'Confirmed',
        'Rejected',
        'Ongoing',
        'Completed',
        'Cancelled',
        'NoShow',
        'RefundPending',
        'Refunded',
        'LatePickup'
        ) NOT NULL DEFAULT 'Pending';

ALTER TABLE RefundRequest
    MODIFY COLUMN Status ENUM(
    'Pending',
    'Approved',
    'Rejected',
    'Transferred',
    'Completed',
    'Refunded'
    ) NOT NULL DEFAULT 'Pending';


-- Add Transferred status to RefundRequest Status ENUM
ALTER TABLE RefundRequest MODIFY COLUMN Status ENUM('Pending', 'Approved', 'Rejected', 'Transferred', 'Completed', 'Refunded') NOT NULL;
-- Add transfer proof image field to RefundRequest table
ALTER TABLE RefundRequest ADD COLUMN TransferProofImagePath VARCHAR(500) NULL;


ALTER TABLE Booking
    ADD COLUMN return_notes VARCHAR(255) NULL;

ALTER TABLE Booking
    ADD COLUMN return_image_urls TEXT NULL;

ALTER TABLE Booking
    ADD COLUMN PaymentOption VARCHAR(20) NULL;

-- First, update any existing Approved/Transferred/Completed records to Refunded
UPDATE RefundRequest
SET Status = 'Refunded'
WHERE Status IN ('Approved', 'Transferred', 'Completed');

-- Then modify the ENUM to only include: Pending, Rejected, Refunded
ALTER TABLE RefundRequest
    MODIFY COLUMN Status ENUM('Pending', 'Rejected', 'Refunded') NOT NULL DEFAULT 'Pending';
