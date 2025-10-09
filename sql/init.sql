CREATE DATABASE HRManagement;
GO
USE HRManagement;

CREATE TABLE meals (
    id INT PRIMARY KEY IDENTITY(1,1),
    name NVARCHAR(100) NOT NULL,
    price DECIMAL(10,2) NOT NULL
);

INSERT INTO meals (name, price) VALUES
(N'Phở bò', 45000),
(N'Cơm gà', 50000),
(N'Bún chả', 40000);
