-- =============================================
-- DATABASE: ecodanav2
-- HỆ QUẢN TRỊ: SQL Server
-- PHIÊN BẢN TỐI ƯU HÓA (SCHEMA + DATA)
-- =============================================

-- =============================================
-- SECTION 1: CÁC BẢNG TRA CỨU (LOOKUP TABLES)
-- =============================================

IF OBJECT_ID('Roles', 'U') IS NOT NULL DROP TABLE Roles;
CREATE TABLE Roles (
    RoleId char(36) NOT NULL,
    RoleName varchar(50) DEFAULT NULL,
    NormalizedName varchar(256) DEFAULT NULL,
    PRIMARY KEY (RoleId)
);

IF OBJECT_ID('VehicleCategories', 'U') IS NOT NULL DROP TABLE VehicleCategories;
CREATE TABLE VehicleCategories (
    CategoryId int NOT NULL IDENTITY(1,1),
    CategoryName varchar(100) NOT NULL,
    PRIMARY KEY (CategoryId),
    CONSTRAINT UQ_VehicleCategories_CategoryName UNIQUE (CategoryName)
);

IF OBJECT_ID('TransmissionTypes', 'U') IS NOT NULL DROP TABLE TransmissionTypes;
CREATE TABLE TransmissionTypes (
    TransmissionTypeId int NOT NULL IDENTITY(1,1),
    TransmissionTypeName varchar(100) NOT NULL,
    PRIMARY KEY (TransmissionTypeId),
    CONSTRAINT UQ_TransmissionTypes_TransmissionTypeName UNIQUE (TransmissionTypeName)
);

-- =============================================
-- SECTION 2: CÁC BẢNG CỐT LÕI (CORE TABLES)
-- =============================================

IF OBJECT_ID('Users', 'U') IS NOT NULL DROP TABLE Users;
CREATE TABLE Users (
    UserId char(36) NOT NULL,
    Username varchar(100) NOT NULL,
    FirstName varchar(256) DEFAULT NULL,
    LastName varchar(256) DEFAULT NULL,
    UserDOB date DEFAULT NULL,
    PhoneNumber varchar(15) DEFAULT NULL,
    AvatarUrl varchar(255) DEFAULT NULL,
    Gender varchar(20) DEFAULT NULL CHECK (Gender IN ('Male', 'Female', 'Other')),
    Status varchar(20) NOT NULL DEFAULT 'Active' CHECK (Status IN ('Active', 'Inactive', 'Banned')),
    RoleId char(36) NOT NULL,
    Email varchar(100) NOT NULL,
    EmailVerifed bit NOT NULL DEFAULT 0,
    PasswordHash varchar(255) NOT NULL,
    CreatedDate datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    NormalizedUserName varchar(256) DEFAULT NULL,
    NormalizedEmail varchar(256) DEFAULT NULL,
    SecurityStamp nvarchar(max),
    ConcurrencyStamp nvarchar(max),
    TwoFactorEnabled bit NOT NULL DEFAULT 0,
    LockoutEnd datetime DEFAULT NULL,
    LockoutEnabled bit NOT NULL DEFAULT 0,
    AccessFailedCount int NOT NULL DEFAULT 0,
    PRIMARY KEY (UserId),
    CONSTRAINT UQ_Users_Username UNIQUE (Username),
    CONSTRAINT UQ_Users_Email UNIQUE (Email),
    CONSTRAINT FK_Users_Roles FOREIGN KEY (RoleId) REFERENCES Roles (RoleId) ON DELETE CASCADE
);
CREATE INDEX idx_user_email ON Users (Email);
CREATE INDEX idx_user_status ON Users (Status);

IF OBJECT_ID('UserDocuments', 'U') IS NOT NULL DROP TABLE UserDocuments;
CREATE TABLE UserDocuments (
    DocumentId char(36) NOT NULL,
    UserId char(36) NOT NULL,
    DocumentType varchar(20) NOT NULL CHECK (DocumentType IN ('CitizenId', 'DriverLicense', 'Passport')),
    DocumentNumber varchar(50) NOT NULL,
    FullName varchar(100) DEFAULT NULL,
    DOB date DEFAULT NULL,
    IssuedDate date DEFAULT NULL,
    IssuedPlace varchar(100) DEFAULT NULL,
    FrontImageUrl varchar(500) DEFAULT NULL,
    BackImageUrl varchar(500) DEFAULT NULL,
    IsVerified bit NOT NULL DEFAULT 0,
    CreatedDate datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (DocumentId),
    CONSTRAINT FK_UserDocuments_Users FOREIGN KEY (UserId) REFERENCES Users (UserId) ON DELETE CASCADE
);

IF OBJECT_ID('Vehicle', 'U') IS NOT NULL DROP TABLE Vehicle;
CREATE TABLE Vehicle (
    VehicleId char(36) NOT NULL,
    VehicleModel varchar(50) NOT NULL,
    YearManufactured int DEFAULT NULL,
    LicensePlate varchar(20) NOT NULL,
    Seats int NOT NULL,
    Odometer int NOT NULL,
    RentalPrices nvarchar(max) DEFAULT NULL, 
    Status varchar(20) NOT NULL DEFAULT 'Available',
    Description varchar(500) DEFAULT NULL,
    VehicleType varchar(20) NOT NULL CHECK (VehicleType IN ('ElectricCar', 'ElectricMotorcycle')),
    RequiresLicense bit NOT NULL DEFAULT 1,
    BatteryCapacity decimal(10,2) DEFAULT NULL,
    CreatedDate datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    LastUpdatedBy char(36) DEFAULT NULL,
    CategoryId int DEFAULT NULL,
    TransmissionTypeId int DEFAULT NULL,
    MainImageUrl varchar(255) DEFAULT NULL,
    ImageUrls nvarchar(max) DEFAULT NULL,
    Features nvarchar(max) DEFAULT NULL,
    OwnerId CHAR(36) NULL,
    PRIMARY KEY (VehicleId),
    CONSTRAINT FK_Vehicle_Updater FOREIGN KEY (LastUpdatedBy) REFERENCES Users (UserId) ON DELETE NO ACTION,
    CONSTRAINT FK_Vehicle_Category FOREIGN KEY (CategoryId) REFERENCES VehicleCategories (CategoryId) ON DELETE SET NULL,
    CONSTRAINT FK_Vehicle_Transmission FOREIGN KEY (TransmissionTypeId) REFERENCES TransmissionTypes (TransmissionTypeId) ON DELETE SET NULL,
    CONSTRAINT FK_Vehicle_Owner FOREIGN KEY (OwnerId) REFERENCES Users (UserId) ON DELETE NO ACTION
);
CREATE INDEX idx_vehicle_type ON Vehicle (VehicleType);
CREATE INDEX idx_license_plate ON Vehicle (LicensePlate);
CREATE INDEX idx_vehicle_status ON Vehicle (Status);

IF OBJECT_ID('Discount', 'U') IS NOT NULL DROP TABLE Discount;
CREATE TABLE Discount (
    DiscountId char(36) NOT NULL,
    DiscountName varchar(100) NOT NULL,
    Description varchar(255) DEFAULT NULL,
    DiscountType varchar(20) NOT NULL CHECK (DiscountType IN ('Percentage', 'FixedAmount')),
    DiscountValue decimal(10,2) NOT NULL,
    StartDate date NOT NULL,
    EndDate date NOT NULL,
    IsActive bit NOT NULL,
    CreatedDate datetime NOT NULL,
    VoucherCode varchar(20) DEFAULT NULL,
    MinOrderAmount decimal(10,2) NOT NULL,
    MaxDiscountAmount decimal(10,2) DEFAULT NULL,
    UsageLimit int DEFAULT NULL,
    UsedCount int NOT NULL,
    DiscountCategory varchar(20) NOT NULL DEFAULT 'General',
    PRIMARY KEY (DiscountId),
    CONSTRAINT UQ_Discount_VoucherCode UNIQUE (VoucherCode)
);

IF OBJECT_ID('Booking', 'U') IS NOT NULL DROP TABLE Booking;
CREATE TABLE Booking (
    BookingId char(36) NOT NULL,
    UserId char(36) NOT NULL,
    VehicleId char(36) NOT NULL,
    HandledBy char(36) DEFAULT NULL,
    PickupDateTime datetime NOT NULL,
    ReturnDateTime datetime NOT NULL,
    PickupLocation VARCHAR(500) NULL,
    VehicleRentalFee DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    PlatformFee DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    OwnerPayout DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    TotalAmount decimal(10,2) NOT NULL,
    DepositAmountRequired DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    RemainingAmount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    PaymentConfirmedAt DATETIME NULL,
    Status varchar(20) NOT NULL DEFAULT 'Pending' CHECK (Status IN ('Pending', 'Approved', 'AwaitingDeposit', 'Confirmed', 'Rejected', 'Ongoing', 'Completed', 'Cancelled', 'NoShow', 'RefundPending', 'Refunded', 'LatePickup')),
    DiscountId char(36) DEFAULT NULL,
    CreatedDate datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CancelReason varchar(500) DEFAULT NULL,
    CancellationType varchar(50) DEFAULT NULL,
    BookingCode varchar(20) NOT NULL,
    ExpectedPaymentMethod varchar(50) DEFAULT NULL,
    RentalType varchar(20) NOT NULL DEFAULT 'daily' CHECK (RentalType IN ('hourly', 'daily', 'monthly')),
    TermsAgreed bit NOT NULL DEFAULT 0,
    TermsAgreedAt datetime DEFAULT NULL,
    TermsVersion varchar(10) DEFAULT 'v1.0',
    return_notes VARCHAR(255) NULL,
    return_image_urls nvarchar(max) NULL,
    PaymentOption VARCHAR(20) NULL,
    PRIMARY KEY (BookingId),
    CONSTRAINT UQ_Booking_BookingCode UNIQUE (BookingCode),
    CONSTRAINT FK_Booking_User FOREIGN KEY (UserId) REFERENCES Users (UserId) ON DELETE CASCADE,
    CONSTRAINT FK_Booking_Vehicle FOREIGN KEY (VehicleId) REFERENCES Vehicle (VehicleId),
    CONSTRAINT FK_Booking_Staff FOREIGN KEY (HandledBy) REFERENCES Users (UserId),
    CONSTRAINT FK_Booking_Discount FOREIGN KEY (DiscountId) REFERENCES Discount (DiscountId),
    CONSTRAINT CHK_Booking_Amount CHECK (TotalAmount >= 0)
);
CREATE INDEX idx_booking_dates_status ON Booking (PickupDateTime, ReturnDateTime, Status);

IF OBJECT_ID('Contract', 'U') IS NOT NULL DROP TABLE Contract;
CREATE TABLE Contract (
    ContractId char(36) NOT NULL,
    ContractCode varchar(30) NOT NULL,
    UserId char(36) NOT NULL,
    BookingId char(36) NOT NULL,
    CreatedDate datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    SignedDate datetime DEFAULT NULL,
    CompletedDate datetime DEFAULT NULL,
    Status varchar(20) NOT NULL DEFAULT 'Draft' CHECK (Status IN ('Draft', 'Signed', 'Completed', 'Cancelled')),
    TermsAccepted bit NOT NULL DEFAULT 0,
    SignatureData nvarchar(max),
    SignatureMethod varchar(20) DEFAULT NULL,
    ContractPdfUrl varchar(500) DEFAULT NULL,
    Notes varchar(500) DEFAULT NULL,
    CancellationReason varchar(500) DEFAULT NULL,
    CitizenIdSnapshotId char(36) DEFAULT NULL,
    DriverLicenseSnapshotId char(36) DEFAULT NULL,
    PRIMARY KEY (ContractId),
    CONSTRAINT UQ_Contract_ContractCode UNIQUE (ContractCode),
    CONSTRAINT FK_Contract_User FOREIGN KEY (UserId) REFERENCES Users (UserId) ON DELETE CASCADE,
    CONSTRAINT FK_Contract_Booking FOREIGN KEY (BookingId) REFERENCES Booking (BookingId),
    CONSTRAINT FK_Contract_CitizenId FOREIGN KEY (CitizenIdSnapshotId) REFERENCES UserDocuments (DocumentId),
    CONSTRAINT FK_Contract_DriverLicense FOREIGN KEY (DriverLicenseSnapshotId) REFERENCES UserDocuments (DocumentId)
);
CREATE INDEX idx_contract_status ON Contract (Status);

IF OBJECT_ID('Payment', 'U') IS NOT NULL DROP TABLE Payment;
CREATE TABLE Payment (
    PaymentId char(36) NOT NULL,
    BookingId char(36) NOT NULL,
    ContractId char(36) DEFAULT NULL,
    Amount decimal(10,2) NOT NULL,
    PaymentMethod varchar(50) NOT NULL,
    PaymentStatus varchar(20) NOT NULL DEFAULT 'Pending' CHECK (PaymentStatus IN ('Pending', 'Completed', 'Failed', 'Refunded')),
    PaymentType varchar(20) NOT NULL DEFAULT 'Deposit' CHECK (PaymentType IN ('Deposit', 'FinalPayment', 'Surcharge', 'Refund')),
    TransactionId varchar(100) DEFAULT NULL,
    PaymentDate datetime DEFAULT NULL,
    UserId char(36) DEFAULT NULL,
    OrderCode VARCHAR(100) NULL,
    Notes varchar(500) DEFAULT NULL,
    CreatedDate datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (PaymentId),
    CONSTRAINT FK_Payment_Booking FOREIGN KEY (BookingId) REFERENCES Booking (BookingId) ON DELETE CASCADE,
    CONSTRAINT FK_Payment_Contract FOREIGN KEY (ContractId) REFERENCES Contract (ContractId),
    CONSTRAINT FK_Payment_User FOREIGN KEY (UserId) REFERENCES Users (UserId)
);
CREATE INDEX idx_payment_ordercode ON Payment (OrderCode);

IF OBJECT_ID('BookingApproval', 'U') IS NOT NULL DROP TABLE BookingApproval;
CREATE TABLE BookingApproval (
    ApprovalId char(36) NOT NULL,
    BookingId char(36) NOT NULL,
    StaffId char(36) NOT NULL,
    ApprovalStatus varchar(20) NOT NULL CHECK (ApprovalStatus IN ('Approved', 'Rejected')),
    ApprovalDate datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    Note varchar(500) DEFAULT NULL,
    RejectionReason varchar(500) DEFAULT NULL,
    PRIMARY KEY (ApprovalId),
    CONSTRAINT FK_BookingApproval_Booking FOREIGN KEY (BookingId) REFERENCES Booking (BookingId) ON DELETE CASCADE,
    CONSTRAINT FK_BookingApproval_Staff FOREIGN KEY (StaffId) REFERENCES Users (UserId)
);

IF OBJECT_ID('BookingSurcharges', 'U') IS NOT NULL DROP TABLE BookingSurcharges;
CREATE TABLE BookingSurcharges (
    SurchargeId char(36) NOT NULL,
    BookingId char(36) NOT NULL,
    SurchargeType varchar(50) NOT NULL,
    Amount decimal(10,2) NOT NULL,
    Description varchar(255) DEFAULT NULL,
    CreatedDate datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    SurchargeCategory varchar(50) DEFAULT NULL,
    IsSystemGenerated bit NOT NULL DEFAULT 0,
    PRIMARY KEY (SurchargeId),
    CONSTRAINT FK_BookingSurcharges_Booking FOREIGN KEY (BookingId) REFERENCES Booking (BookingId) ON DELETE CASCADE,
    CONSTRAINT CHK_BookingSurcharges_Amount CHECK (Amount >= 0)
);

-- Indices for performance (moving to Section 3)


-- =============================================
-- SECTION 3: CÁC BẢNG PHỤ TRỢ (SUPPORTING TABLES)
-- =============================================

IF OBJECT_ID('UserFeedback', 'U') IS NOT NULL DROP TABLE UserFeedback;
CREATE TABLE UserFeedback (
    FeedbackId char(36) NOT NULL,
    UserId char(36) NOT NULL,
    VehicleId char(36) DEFAULT NULL,
    BookingId char(36) DEFAULT NULL,
    Rating int NOT NULL,
    Content varchar(4000) DEFAULT NULL,
    Reviewed date NOT NULL,
    CreatedDate datetime NOT NULL,
    StaffReply nvarchar(max),
    ReplyDate datetime DEFAULT NULL,
    PRIMARY KEY (FeedbackId),
    CONSTRAINT FK_UserFeedback_User FOREIGN KEY (UserId) REFERENCES Users (UserId) ON DELETE CASCADE,
    CONSTRAINT FK_UserFeedback_Vehicle FOREIGN KEY (VehicleId) REFERENCES Vehicle (VehicleId),
    CONSTRAINT FK_UserFeedback_Booking FOREIGN KEY (BookingId) REFERENCES Booking (BookingId),
    CONSTRAINT CHK_Rating_Range CHECK (Rating BETWEEN 1 AND 5)
);

IF OBJECT_ID('VehicleConditionLogs', 'U') IS NOT NULL DROP TABLE VehicleConditionLogs;
CREATE TABLE VehicleConditionLogs (
    LogId char(36) NOT NULL,
    BookingId char(36) NOT NULL,
    VehicleId char(36) NOT NULL,
    StaffId char(36) DEFAULT NULL,
    CheckType varchar(20) NOT NULL CHECK (CheckType IN ('Pickup', 'Return')),
    CheckTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    Odometer int DEFAULT NULL,
    FuelLevel varchar(20) DEFAULT NULL,
    ConditionStatus varchar(100) DEFAULT NULL,
    ConditionDescription varchar(1000) DEFAULT NULL,
    DamageImages nvarchar(max),
    Note varchar(255) DEFAULT NULL,
    PRIMARY KEY (LogId),
    CONSTRAINT FK_VehicleConditionLogs_Booking FOREIGN KEY (BookingId) REFERENCES Booking (BookingId) ON DELETE CASCADE,
    CONSTRAINT FK_VehicleConditionLogs_Vehicle FOREIGN KEY (VehicleId) REFERENCES Vehicle (VehicleId) ON DELETE CASCADE,
    CONSTRAINT FK_VehicleConditionLogs_Staff FOREIGN KEY (StaffId) REFERENCES Users (UserId)
);

IF OBJECT_ID('BatteryLogs', 'U') IS NOT NULL DROP TABLE BatteryLogs;
CREATE TABLE BatteryLogs (
    LogId char(36) NOT NULL,
    VehicleId char(36) NOT NULL,
    BookingId char(36) DEFAULT NULL,
    BatteryLevel decimal(5,2) NOT NULL,
    CheckTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    Note varchar(255) DEFAULT NULL,
    PRIMARY KEY (LogId),
    CONSTRAINT FK_BatteryLogs_Vehicle FOREIGN KEY (VehicleId) REFERENCES Vehicle (VehicleId) ON DELETE CASCADE,
    CONSTRAINT FK_BatteryLogs_Booking FOREIGN KEY (BookingId) REFERENCES Booking (BookingId)
);

IF OBJECT_ID('PasswordResetTokens', 'U') IS NOT NULL DROP TABLE PasswordResetTokens;
CREATE TABLE PasswordResetTokens (
    Id char(36) NOT NULL,
    Token varchar(255) NOT NULL,
    ExpiryTime datetime NOT NULL,
    IsUsed bit NOT NULL DEFAULT 0,
    UserId char(36) NOT NULL,
    CreatedAt datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (Id),
    CONSTRAINT FK_PasswordResetTokens_User FOREIGN KEY (UserId) REFERENCES Users (UserId) ON DELETE CASCADE
);

IF OBJECT_ID('UserFavoriteVehicles', 'U') IS NOT NULL DROP TABLE UserFavoriteVehicles;
CREATE TABLE UserFavoriteVehicles (
    UserId char(36) NOT NULL,
    VehicleId char(36) NOT NULL,
    PRIMARY KEY (UserId, VehicleId),
    CONSTRAINT FK_UserFavoriteVehicles_User FOREIGN KEY (UserId) REFERENCES Users (UserId) ON DELETE CASCADE,
    CONSTRAINT FK_UserFavoriteVehicles_Vehicle FOREIGN KEY (VehicleId) REFERENCES Vehicle (VehicleId) ON DELETE CASCADE
);

IF OBJECT_ID('UserLogins', 'U') IS NOT NULL DROP TABLE UserLogins;
CREATE TABLE UserLogins (
    LoginProvider varchar(128) NOT NULL,
    ProviderKey varchar(128) NOT NULL,
    ProviderDisplayName nvarchar(max),
    UserId char(36) NOT NULL,
    PRIMARY KEY (LoginProvider, ProviderKey),
    CONSTRAINT FK_UserLogins_User FOREIGN KEY (UserId) REFERENCES Users (UserId) ON DELETE CASCADE
);

IF OBJECT_ID('UserVoucherUsage', 'U') IS NOT NULL DROP TABLE UserVoucherUsage;
CREATE TABLE UserVoucherUsage (
    UserId char(36) NOT NULL,
    DiscountId char(36) NOT NULL,
    UsedAt datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (UserId, DiscountId),
    CONSTRAINT FK_UserVoucherUsage_User FOREIGN KEY (UserId) REFERENCES Users (UserId) ON DELETE CASCADE,
    CONSTRAINT FK_UserVoucherUsage_Discount FOREIGN KEY (DiscountId) REFERENCES Discount (DiscountId) ON DELETE CASCADE
);

IF OBJECT_ID('Notification', 'U') IS NOT NULL DROP TABLE Notification;
CREATE TABLE Notification (
    NotificationId char(36) NOT NULL,
    UserId char(36) NOT NULL,
    Message nvarchar(max) NOT NULL,
    CreatedDate datetime NOT NULL,
    IsRead bit NOT NULL,
    RelatedId char(36) DEFAULT NULL,
    NotificationType varchar(50) DEFAULT NULL,
    PRIMARY KEY (NotificationId),
    CONSTRAINT FK_Notification_User FOREIGN KEY (UserId) REFERENCES Users (UserId) ON DELETE CASCADE
);

IF OBJECT_ID('AccountDeletionLogs', 'U') IS NOT NULL DROP TABLE AccountDeletionLogs;
CREATE TABLE AccountDeletionLogs (
    LogId char(36) NOT NULL,
    UserId char(36) NOT NULL,
    DeletionReason varchar(255) NOT NULL,
    AdditionalComments nvarchar(max),
    Timestamp datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (LogId),
    CONSTRAINT FK_AccountDeletionLogs_User FOREIGN KEY (UserId) REFERENCES Users (UserId) ON DELETE CASCADE
);

IF OBJECT_ID('Terms', 'U') IS NOT NULL DROP TABLE Terms;
CREATE TABLE Terms (
    TermsId char(36) NOT NULL,
    Version varchar(10) NOT NULL,
    Title varchar(200) NOT NULL,
    ShortContent nvarchar(max),
    FullContent nvarchar(max) NOT NULL,
    EffectiveDate date NOT NULL,
    IsActive bit NOT NULL DEFAULT 1,
    CreatedDate datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (TermsId),
    CONSTRAINT UQ_Terms_Version UNIQUE (Version)
);

IF OBJECT_ID('InappropriateWord', 'U') IS NOT NULL DROP TABLE InappropriateWord;
CREATE TABLE InappropriateWord (
    Id char(36) NOT NULL,
    Word varchar(255) NOT NULL,
    Category varchar(100) DEFAULT NULL,
    Severity varchar(20) NOT NULL DEFAULT 'MEDIUM' CHECK (Severity IN ('LOW', 'MEDIUM', 'HIGH')),
    IsActive bit NOT NULL DEFAULT 1,
    CreatedDate datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (Id),
    CONSTRAINT UQ_InappropriateWord_Word UNIQUE (Word)
);
CREATE INDEX idx_inappropriate_word_active ON InappropriateWord (IsActive);

IF OBJECT_ID('BankAccount', 'U') IS NOT NULL DROP TABLE BankAccount;
CREATE TABLE BankAccount (
    BankAccountId char(36) PRIMARY KEY,
    UserId char(36) NOT NULL,
    AccountNumber VARCHAR(50) NOT NULL,

    AccountHolderName VARCHAR(100) NOT NULL,
    BankName VARCHAR(100) NOT NULL,
    BankCode VARCHAR(20),
    QRCodeImagePath VARCHAR(500),
    IsDefault bit NOT NULL DEFAULT 0,
    CreatedDate DATETIME NOT NULL,
    UpdatedDate DATETIME,
    CONSTRAINT FK_BankAccount_User FOREIGN KEY (UserId) REFERENCES Users (UserId) ON DELETE CASCADE
);
CREATE INDEX idx_bankaccount_userid ON BankAccount(UserId);
CREATE INDEX idx_bankaccount_isdefault ON BankAccount(IsDefault);


IF OBJECT_ID('RefundRequest', 'U') IS NOT NULL DROP TABLE RefundRequest;
CREATE TABLE RefundRequest (
    RefundRequestId char(36) PRIMARY KEY,
    BookingId char(36) NOT NULL,
    UserId char(36) NOT NULL,
    BankAccountId char(36) NOT NULL,
    RefundAmount DECIMAL(10,2) NOT NULL,
    CancelReason nvarchar(max) NOT NULL,

    Status varchar(20) NOT NULL DEFAULT 'Pending' CHECK (Status IN ('Pending', 'Rejected', 'Refunded')),
    AdminNotes nvarchar(max),
    ProcessedBy char(36),
    CreatedDate DATETIME NOT NULL,

    ProcessedDate DATETIME,
    IsWithinTwoHours bit NOT NULL DEFAULT 0,
    TransferProofImagePath VARCHAR(500) NULL,
    CONSTRAINT FK_RefundRequest_Booking FOREIGN KEY (BookingId) REFERENCES Booking (BookingId),
    CONSTRAINT FK_RefundRequest_User FOREIGN KEY (UserId) REFERENCES Users (UserId) ON DELETE CASCADE,
    CONSTRAINT FK_RefundRequest_Bank FOREIGN KEY (BankAccountId) REFERENCES BankAccount (BankAccountId)
);
CREATE INDEX idx_refundrequest_bookingid ON RefundRequest(BookingId);
CREATE INDEX idx_refundrequest_userid ON RefundRequest(UserId);
CREATE INDEX idx_refundrequest_status ON RefundRequest(Status);
CREATE INDEX idx_refundrequest_createddate ON RefundRequest(CreatedDate);


IF OBJECT_ID('FeedbackReport', 'U') IS NOT NULL DROP TABLE FeedbackReport;
CREATE TABLE FeedbackReport (
    ReportId char(36) not null primary key,
    CreatedDate datetime NOT NULL,
    Reason varchar(1000) null,
    Status varchar(20) not null CHECK (Status IN ('Pending', 'Resolved')),
    FeedbackId char(36) not null,
    ReporterId char(36) not null,
    CONSTRAINT FK_FeedbackReport_Feedback foreign key (FeedbackId) references UserFeedback (FeedbackId) ON DELETE NO ACTION,
    CONSTRAINT FK_FeedbackReport_Reporter foreign key (ReporterId) references Users (UserId) ON DELETE NO ACTION
);

-- =============================================
-- SECTION 4: DỮ LIỆU BAN ĐẦU (INITIAL DATA)
-- =============================================

-- 1. Roles
INSERT INTO Roles (RoleId, RoleName, NormalizedName)
VALUES ('550e8400-e29b-41d4-a716-446655440001', 'Admin', 'ADMIN'),
       ('550e8400-e29b-41d4-a716-446655440002', 'Staff', 'STAFF'),
       ('550e8400-e29b-41d4-a716-446655440003', 'Owner', 'OWNER'),
       ('550e8400-e29b-41d4-a716-446655440004', 'Customer', 'CUSTOMER');

-- 2. TransmissionTypes
SET IDENTITY_INSERT TransmissionTypes ON;
INSERT INTO TransmissionTypes (TransmissionTypeId, TransmissionTypeName) VALUES (1, 'Automatic');
SET IDENTITY_INSERT TransmissionTypes OFF;

-- 3. VehicleCategories
SET IDENTITY_INSERT VehicleCategories ON;
INSERT INTO VehicleCategories (CategoryId, CategoryName) VALUES (1, 'Electric Car'), (2, 'Electric Motorbike');
SET IDENTITY_INSERT VehicleCategories OFF;

-- 4. Users
INSERT INTO Users (UserId, Username, Email, PasswordHash, PhoneNumber, FirstName, LastName, Status, RoleId, CreatedDate,
                   NormalizedUserName, NormalizedEmail, EmailVerifed, SecurityStamp, ConcurrencyStamp, TwoFactorEnabled,
                   LockoutEnabled, AccessFailedCount)
VALUES ('550e8400-e29b-41d4-a716-446655440100', 'admin', 'admin@ecodana.com',
        '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '0123456789', 'Admin', 'EvoDana', 'Active',
        '550e8400-e29b-41d4-a716-446655440001', GETDATE(), 'ADMIN', 'ADMIN@ECODANA.COM', 1, 'admin-security-stamp',
        'admin-concurrency-stamp', 0, 0, 0),
       ('550e8400-e29b-41d4-a716-446655440101', 'staff', 'staff@ecodana.com',
        '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '0987654321', 'Staff', 'EvoDana', 'Active',
        '550e8400-e29b-41d4-a716-446655440002', GETDATE(), 'STAFF', 'STAFF@ECODANA.COM', 1, 'staff-security-stamp',
        'staff-concurrency-stamp', 0, 0, 0),
       ('550e8400-e29b-41d4-a716-446655440102', 'owner', 'owner@ecodana.com',
        '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '0912345678', 'Owner', 'EvoDana', 'Active',
        '550e8400-e29b-41d4-a716-446655440003', GETDATE(), 'OWNER', 'OWNER@ECODANA.COM', 1, 'owner-security-stamp',
        'owner-concurrency-stamp', 0, 0, 0),
       ('550e8400-e29b-41d4-a716-446655440103', 'customer', 'customer@ecodana.com',
        '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '0901234567', 'Customer', 'EvoDana', 'Active',
        '550e8400-e29b-41d4-a716-446655440004', GETDATE(), 'CUSTOMER', 'CUSTOMER@ECODANA.COM', 1, 'customer-security-stamp',
        'customer-concurrency-stamp', 0, 0, 0);

-- 5. Vehicles
INSERT INTO Vehicle (VehicleId, VehicleModel, YearManufactured, LicensePlate, Seats, Odometer, RentalPrices, Status, Description, VehicleType, RequiresLicense, BatteryCapacity, CreatedDate, LastUpdatedBy, CategoryId, TransmissionTypeId, MainImageUrl, ImageUrls, Features)
VALUES 
('a1b2c3d4-vin-0001-vf3','VinFast VF 3',2024,'43A-301.11',4,1500,'{"daily": 800000, "hourly": 90000, "monthly": 18000000}','Available','Mẫu mini-eSUV nhỏ gọn, cá tính, lý tưởng để di chuyển linh hoạt trong thành phố Đà Nẵng. Phạm vi di chuyển khoảng 210km.','ElectricCar',1,37.23,'2025-10-23 15:51:33','550e8400-e29b-41d4-a716-446655440102',1,1,'https://res.cloudinary.com/ddsypvnqg/image/upload/v1761209575/ecodana/vehicles/n2kzk1tudstf7l9w4fja.jpg','["https://res.cloudinary.com/ddsypvnqg/image/upload/v1761209577/ecodana/vehicles/auxiliary/off7xblfwnkadjn2d4ft.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761209578/ecodana/vehicles/auxiliary/bwr9bx1abpcgny6nmiaq.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761209580/ecodana/vehicles/auxiliary/uu8akqwex7vpeaevr87b.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761209582/ecodana/vehicles/auxiliary/v3lz3f52qs00bgaxp9kh.jpg"]','["GPS Navigation", "Power Windows", "Central Locking", "Parking Sensors", "Cruise Control", "Leather Seats", "Heated Seats", "Airbags", "LED Headlights", "Auto Climate Control"]'),
('a1b2c3d4-vin-0002-vf5','VinFast VF 5 Plus',2023,'43A-302.22',5,8000,'{"daily": 900000, "hourly": 100000, "monthly": 20000000}','Available','Mẫu A-SUV cỡ nhỏ, phù hợp cho gia đình trẻ hoặc nhóm bạn khám phá các cung đường ven biển. Phạm vi di chuyển hơn 300km.','ElectricCar',1,37.23,'2025-10-23 15:53:51','550e8400-e29b-41d4-a716-446655440102',1,1,'https://res.cloudinary.com/ddsypvnqg/image/upload/v1761209660/ecodana/vehicles/v90cwqqie3fl9v8w2x8o.webp','["https://res.cloudinary.com/ddsypvnqg/image/upload/v1761209662/ecodana/vehicles/auxiliary/tvmyfsiurn0xegkcgpkr.webp", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761209664/ecodana/vehicles/auxiliary/wfg055oqdlc1akbhyzmn.webp", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761209666/ecodana/vehicles/auxiliary/valbjydhmql29qqvb0gn.webp", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761209669/ecodana/vehicles/auxiliary/ll4firz1bgxrucnudphs.webp"]','["Central Locking", "Parking Sensors", "Leather Seats", "Heated Seats", "ABS Brakes", "LED Headlights"]'),
('a1b2c3d4-vin-0003-e34','VinFast VF e34',2022,'43A-303.33',5,25000,'{"daily": 1000000, "hourly": 120000, "monthly": 22000000}','Available','Chiếc C-SUV thuần điện đầu tiên của VinFast, vận hành êm ái, trang bị trợ lý ảo thông minh. Phạm vi di chuyển khoảng 285km.','ElectricCar',1,42.00,'2025-10-23 15:55:08','550e8400-e29b-41d4-a716-446655440102',1,1,'https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210561/ecodana/vehicles/t938m6eekldeqndfbqii.jpg','["https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210563/ecodana/vehicles/auxiliary/uwkyyr7bdwb83ir0o4il.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210565/ecodana/vehicles/auxiliary/f2fqu3m8e4z4vuehdj9u.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210567/ecodana/vehicles/auxiliary/yg9vew2nabbz47or085q.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210569/ecodana/vehicles/auxiliary/et8qtlmqwp6bxszugoih.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210573/ecodana/vehicles/auxiliary/kaoxov36uefwez1lxqgy.jpg"]','["Power Windows", "USB Charging Ports", "Backup Camera", "Parking Sensors", "Sunroof", "Leather Seats", "Heated Seats"]'),
('a1b2c3d4-vin-0004-vf6','VinFast VF 6 Plus',2023,'43A-304.44',5,11000,'{"daily": 1100000, "hourly": 140000, "monthly": 24000000}','Available','Mẫu B-SUV với thiết kế hiện đại từ studio Torino Design. Nội thất rộng rãi, phạm vi di chuyển lên tới 381km.','ElectricCar',1,59.60,'2025-10-23 15:55:08','550e8400-e29b-41d4-a716-446655440102',1,1,'https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210161/ecodana/vehicles/myo73ybpi59xvw7whrus.jpg','["https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210162/ecodana/vehicles/auxiliary/gnqmrgkwgssnm8gpjggf.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210164/ecodana/vehicles/auxiliary/fbrifktlmodyjdf1skdz.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210167/ecodana/vehicles/auxiliary/j7bhzxodvje7rdzqvky8.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210169/ecodana/vehicles/auxiliary/pgjxbwq7fdpqbkdlsujh.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210171/ecodana/vehicles/auxiliary/ejo220yfhqcoq3mkccui.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210172/ecodana/vehicles/auxiliary/xkshieswsr2dl3wgxcu2.jpg"]','["Backup Camera", "Parking Sensors", "Cruise Control", "Heated Seats", "Automatic Transmission", "ABS Brakes"]'),
('a1b2c3d4-vin-0005-vf7','VinFast VF 7 Plus',2024,'43A-305.55',5,5500,'{"daily": 1300000, "hourly": 160000, "monthly": 27000000}','Available','Thiết kế C-SUV mang phong cách tương lai, hiệu suất vận hành mạnh mẽ. Phạm vi di chuyển ấn tượng lên đến 431km.','ElectricCar',1,75.30,'2025-10-23 15:55:08','550e8400-e29b-41d4-a716-446655440102',1,1,'https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210259/ecodana/vehicles/idqadxtaia3wmvpit7rp.webp','["https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210261/ecodana/vehicles/auxiliary/bqkekzeaygaczxlhtsol.webp", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210264/ecodana/vehicles/auxiliary/kioaimindfqyclwpqhel.webp", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210266/ecodana/vehicles/auxiliary/sxnoy0ht5jliqthqwbf7.webp", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210269/ecodana/vehicles/auxiliary/jnrbmftuqpmdmbrqbgmz.webp", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210274/ecodana/vehicles/auxiliary/kazcxaesxrmkj2bvi3gv.webp"]','["Power Windows", "Parking Sensors", "Cruise Control", "Sunroof", "Leather Seats", "Heated Seats"]'),
('a1b2c3d4-vin-0006-vf8','VinFast VF 8 Plus',2023,'43A-306.66',5,18000,'{"daily": 1500000, "hourly": 170000, "monthly": 30000000}','Available','SUV điện 5 chỗ hạng D sang trọng, phù hợp cho các chuyến công tác hoặc du lịch gia đình. Phạm vi di chuyển lên tới 447km.','ElectricCar',1,87.70,'2025-10-23 15:55:08','550e8400-e29b-41d4-a716-446655440102',1,1,'https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210337/ecodana/vehicles/t20gy6nk0rhjqxxnclid.webp','["https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210339/ecodana/vehicles/auxiliary/zjqkxclxl35xlx7peqkn.webp", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210340/ecodana/vehicles/auxiliary/o8wkmal5hezv9fyiw1vj.webp", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210342/ecodana/vehicles/auxiliary/tckoldsfcjfgqixkpny0.webp", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210344/ecodana/vehicles/auxiliary/ngwhvhrbgza13helc7xm.webp", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210347/ecodana/vehicles/auxiliary/ybl8meuhrbfdqnnr3rmm.webp", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210349/ecodana/vehicles/auxiliary/bjc0m5pebkmyvaxmkptl.webp"]','["Power Windows", "Central Locking", "USB Charging Ports", "Backup Camera", "ABS Brakes", "Airbags", "LED Headlights"]'),
('a1b2c3d4-vin-0007-vf9','VinFast VF 9',2023,'43A-307.77',6,9500,'{"daily": 2200000, "hourly": 250000, "monthly": 45000000}','Available','Mẫu E-SUV full-size hạng sang với 6 chỗ ngồi và hàng ghế cơ trưởng. Lựa chọn đẳng cấp cho những chuyến đi đặc biệt. Phạm vi 423km.','ElectricCar',1,92.00,'2025-10-23 15:55:08','550e8400-e29b-41d4-a716-446655440102',1,1,'https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210382/ecodana/vehicles/xuc2gidehqe2djyscq9k.webp','["https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210384/ecodana/vehicles/auxiliary/n0zhfdzorty2lkvwdnho.webp", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210389/ecodana/vehicles/auxiliary/ifzqsqdxioyenizspott.webp", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210391/ecodana/vehicles/auxiliary/kqrgrve9vws9urwhagv9.webp", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210395/ecodana/vehicles/auxiliary/dwdtttfxgr2fy291lqzj.webp", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761210397/ecodana/vehicles/auxiliary/xzxd1gmxttbqqg6savls.webp"]','["Power Windows", "Parking Sensors", "Cruise Control", "Keyless Entry", "Sunroof", "Leather Seats", "LED Headlights"]'),
('a1b2c3d4-vin-0008-evoL','VinFast Evo 200 Lite',2023,'43-AB-001.11',2,3500,'{"daily": 140000, "hourly": 18000, "monthly": 2200000}','Available','Phiên bản giới hạn tốc độ, phù hợp cho học sinh, sinh viên và người không cần bằng lái. Di chuyển tới 205km mỗi lần sạc.','ElectricMotorcycle',1,3.50,'2025-10-24 21:15:53','550e8400-e29b-41d4-a716-446655440102',2,1,'https://res.cloudinary.com/ddsypvnqg/image/upload/v1761315399/ecodana/vehicles/rplvh2pv4s4ohpp5illp.jpg','["https://res.cloudinary.com/ddsypvnqg/image/upload/v1761315401/ecodana/vehicles/auxiliary/czr40hpyrxlqfqg5xjjj.jpg"]','["LED Headlights", "Anti-lock Braking System", "Storage Compartment", "Helmet Lock", "Center Stand", "Speedometer"]'),
('a1b2c3d4-vin-0009-evo','VinFast Evo 200',2023,'43-AC-002.22',2,4200,'{"daily": 150000, "hourly": 20000, "monthly": 2500000}','Available','Mẫu xe phổ thông với quãng đường di chuyển vượt trội, thiết kế thời trang và vận hành bền bỉ. Quãng đường ~203km.','ElectricMotorcycle',1,3.50,'2025-10-24 18:21:02','550e8400-e29b-41d4-a716-446655440102',2,1,'https://res.cloudinary.com/ddsypvnqg/image/upload/v1761313571/ecodana/vehicles/oarzfbxefoy9kmxhpk7q.jpg','["https://res.cloudinary.com/ddsypvnqg/image/upload/v1761313574/ecodana/vehicles/auxiliary/gvoq1hrlolkeppgygcxg.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761313577/ecodana/vehicles/auxiliary/pvprr9dhpexmpuiub4m8.jpg"]','["GPS Navigation", "Digital Display", "Keyless Start", "Windshield", "Speedometer"]'),
('a1b2c3d4-vin-0010-feliz','VinFast Feliz S',2022,'43-AD-003.33',2,6800,'{"daily": 150000, "hourly": 20000, "monthly": 2500000}','Available','Xe máy điện nhỏ gọn, cốp rộng, phù hợp để dạo quanh thành phố Đà Nẵng. Quãng đường di chuyển ~198km.','ElectricMotorcycle',1,3.50,'2025-10-24 18:21:02','550e8400-e29b-41d4-a716-446655440102',2,1,'https://res.cloudinary.com/ddsypvnqg/image/upload/v1761313596/ecodana/vehicles/rzmkbke7zzv41jyb1ybm.jpg','["https://res.cloudinary.com/ddsypvnqg/image/upload/v1761313598/ecodana/vehicles/auxiliary/pk4iz1beneakrfi0byjm.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761313601/ecodana/vehicles/auxiliary/y7lkvbcskgxhnppm3s49.jpg"]','["LED Headlights", "Phone Mount", "Helmet Lock", "Side Stand", "Center Stand", "Horn", "Speedometer", "Battery Indicator"]'),
('a1b2c3d4-vin-0011-klara','VinFast Klara S (2022)',2022,'43-AE-004.44',2,7100,'{"daily": 160000, "hourly": 22000, "monthly": 2700000}','Available','Thiết kế Ý sang trọng, vận hành thông minh và thân thiện với môi trường. Quãng đường di chuyển lên tới 194km.','ElectricMotorcycle',1,3.50,'2025-10-24 18:21:02','550e8400-e29b-41d4-a716-446655440102',2,1,'https://res.cloudinary.com/ddsypvnqg/image/upload/v1761313621/ecodana/vehicles/svothyxw8a5w11y0eogn.png','["https://res.cloudinary.com/ddsypvnqg/image/upload/v1761313624/ecodana/vehicles/auxiliary/pscn6cduuvh9v7wra9ox.png", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761313628/ecodana/vehicles/auxiliary/gwvh5etobwhjqtuuuf1h.png"]','["LED Headlights", "Anti-lock Braking System", "Phone Mount", "Helmet Lock", "Side Stand", "Horn", "Speedometer", "Battery Indicator", "Eco Mode"]'),
('a1b2c3d4-vin-0012-vento','VinFast Vento S',2022,'43-AF-005.55',2,5300,'{"daily": 180000, "hourly": 25000, "monthly": 3000000}','Available','Xe tay ga cao cấp với công nghệ PAAK (Phone As A Key) hiện đại, thiết kế lịch lãm, mạnh mẽ. Quãng đường ~160km.','ElectricMotorcycle',1,3.50,'2025-10-24 18:21:02','550e8400-e29b-41d4-a716-446655440102',2,1,'https://res.cloudinary.com/ddsypvnqg/image/upload/v1761314916/ecodana/vehicles/oggkohbdi2csk9z6hiqg.png','["https://res.cloudinary.com/ddsypvnqg/image/upload/v1761315106/ecodana/vehicles/auxiliary/pcfszhdsavvkr81tyqpj.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761315108/ecodana/vehicles/auxiliary/xmtjsduljlowi7uzp4ck.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761315111/ecodana/vehicles/auxiliary/i1enzex30hemufpkn1bp.jpg"]','["GPS Navigation", "LED Headlights", "Anti-lock Braking System", "Storage Compartment", "Phone Mount", "Helmet Lock", "Rear View Mirrors", "Turn Signals", "Speedometer"]'),
('a1b2c3d4-vin-0013-theon','VinFast Theon S',2022,'43-AG-006.66',2,4900,'{"daily": 200000, "hourly": 28000, "monthly": 3500000}','Available','Mẫu xe máy điện cao cấp nhất, hiệu suất vượt trội, công nghệ thông minh PAAK và thiết kế đậm chất thể thao. Quãng đường ~150km.','ElectricMotorcycle',1,3.50,'2025-10-24 18:21:02','550e8400-e29b-41d4-a716-446655440102',2,1,'https://res.cloudinary.com/ddsypvnqg/image/upload/v1761315293/ecodana/vehicles/ivv48txxjjyib00wcgew.jpg','["https://res.cloudinary.com/ddsypvnqg/image/upload/v1761315296/ecodana/vehicles/auxiliary/c0txmwmolnhg560iiifv.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761315298/ecodana/vehicles/auxiliary/ywikcvkoiiywjwp8zrk6.jpg"]','["GPS Navigation", "USB Charging Port", "LED Headlights", "Storage Compartment", "Phone Mount", "Helmet Lock", "Rear View Mirrors", "Turn Signals", "Speedometer"]'),
('cb446a8c-ee06-471f-be67-03ebe3ad78fd','VF6',2020,'43A-305.33',5,1000,'{"daily": 600000, "hourly": 100000, "monthly": 23600000}','Rented','a','ElectricCar',1,100.00,'2025-10-26 09:00:31','550e8400-e29b-41d4-a716-446655440102',1,1,'https://res.cloudinary.com/ddsypvnqg/image/upload/v1761469233/ecodana/vehicles/gmketoiy2jn7jmmdzd25.jpg','["https://res.cloudinary.com/ddsypvnqg/image/upload/v1761469235/ecodana/vehicles/auxiliary/hbe4f1amgpz1lu48eqhi.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761469237/ecodana/vehicles/auxiliary/eosksudv2vv9jgvmklht.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761469241/ecodana/vehicles/auxiliary/h0fp3rexd9xqwmwcxsgm.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761469243/ecodana/vehicles/auxiliary/wqtpijqcvn40lstgtxjg.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761469245/ecodana/vehicles/auxiliary/z0hw7pvt75ubaufubrgs.jpg"]','["GPS Navigation", "Power Windows", "Backup Camera"]'),
('dd6c8ef5-de59-42e0-9783-b33720d385c3','VF6s',2024,'43A-305.51',4,100,'{"daily": 500000, "hourly": 10000, "monthly": 1450000}','Rented','a','ElectricCar',0,100.00,'2025-10-26 08:49:59','550e8400-e29b-41d4-a716-446655440102',1,1,'https://res.cloudinary.com/ddsypvnqg/image/upload/v1761468601/ecodana/vehicles/btmlqpv5w9ntumsnqenl.jpg','["https://res.cloudinary.com/ddsypvnqg/image/upload/v1761468604/ecodana/vehicles/auxiliary/vwi1qbmees0vo1bgkgza.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761468606/ecodana/vehicles/auxiliary/axegmxwpgyrssjmrxiaf.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761468609/ecodana/vehicles/auxiliary/rc6shworukb7osdzki1r.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761468612/ecodana/vehicles/auxiliary/ywymgjinc1jt7pgvu8yg.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761468614/ecodana/vehicles/auxiliary/pydjc4d7jhthkge3xc7a.jpg"]','["GPS Navigation", "Backup Camera"]'),
('fddbcb16-d8e0-4034-87e6-875fb8f70d5b','VF7',2024,'43A-303.31',5,1000,'{"daily": 10000000, "hourly": 100000, "monthly": 10000000}','Unavailable','a','ElectricCar',1,1000.00,'2025-10-26 13:25:32','550e8400-e29b-41d4-a716-446655440102',1,1,'https://res.cloudinary.com/ddsypvnqg/image/upload/v1761485135/ecodana/vehicles/cnmnevtax9q4xxepiycj.jpg','["https://res.cloudinary.com/ddsypvnqg/image/upload/v1761485143/ecodana/vehicles/auxiliary/c2p7gse7ecqhx0ae4ayo.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761485152/ecodana/vehicles/auxiliary/efsfdxjeq6kkjku6u2rj.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761485157/ecodana/vehicles/auxiliary/cicxhbmhegcs9zle1tgp.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761485162/ecodana/vehicles/auxiliary/hj29jsrnmbgndk2zvy9n.jpg", "https://res.cloudinary.com/ddsypvnqg/image/upload/v1761485174/ecodana/vehicles/auxiliary/vhhswbmuymbdjspt3lna.jpg"]','["Keyless Entry", "Heated Seats", "Airbags"]');
