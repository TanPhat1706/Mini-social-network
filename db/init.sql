-- =============================================================
-- Init script: tạo database MiniSocialDB + login/user "pht"
-- Chạy bởi SA sau khi SQL Server khởi động trong Docker.
-- =============================================================

-- 1) Tạo database nếu chưa có
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'MiniSocialDB')
BEGIN
    CREATE DATABASE MiniSocialDB;
    PRINT 'Created database MiniSocialDB';
END
ELSE
    PRINT 'Database MiniSocialDB already exists';
GO

-- 2) Tạo login nếu chưa có (CHECK_POLICY OFF vì password đơn giản)
IF NOT EXISTS (SELECT name FROM sys.server_principals WHERE name = N'pht')
BEGIN
    CREATE LOGIN pht WITH PASSWORD = N'141204', CHECK_POLICY = OFF, CHECK_EXPIRATION = OFF;
    PRINT 'Created login pht';
END
ELSE
    PRINT 'Login pht already exists';
GO

-- 3) Tạo user trong database MiniSocialDB, gán quyền db_owner
USE MiniSocialDB;
GO

IF NOT EXISTS (SELECT name FROM sys.database_principals WHERE name = N'pht')
BEGIN
    CREATE USER pht FOR LOGIN pht;
    ALTER ROLE db_owner ADD MEMBER pht;
    PRINT 'Created user pht with db_owner';
END
ELSE
    PRINT 'User pht already exists';
GO
