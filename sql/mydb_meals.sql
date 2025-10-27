-- MySQL dump 10.13  Distrib 8.0.43, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: mydb
-- ------------------------------------------------------
-- Server version 8.0.43

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Cấu trúc bảng cho bảng `categories`
--

DROP TABLE IF EXISTS `categories`;
CREATE TABLE `categories` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data cho bảng `categories`
--
LOCK TABLES `categories` WRITE;
/*!40000 ALTER TABLE `categories` DISABLE KEYS */;
INSERT INTO `categories` (`id`, `name`) VALUES 
(1, 'Món nước'),
(2, 'Món cơm'),
-- (3, 'Đồ uống') đã bị loại bỏ
(4, 'Món ăn kèm'),
(5, 'Đồ uống & Thuốc lá');    -- Đã đổi tên từ 'Bia & Thuốc lá'
-- (6, 'Cocktail cổ điển') đã bị loại bỏ
/*!40000 ALTER TABLE `categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Cấu trúc bảng cho bảng `meals` (Đã thêm category_id)
--

DROP TABLE IF EXISTS `meals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meals` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `category_id` INT NOT NULL, -- Khóa ngoại liên kết với bảng categories
  `name` VARCHAR(100) NOT NULL,
  `price` DOUBLE NOT NULL,
  `image_path` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_category` (`category_id`),
  CONSTRAINT `fk_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data cho bảng `meals` (Đã thêm category_id)
--
LOCK TABLES `meals` WRITE;
/*!40000 ALTER TABLE `meals` DISABLE KEYS */;
-- ID Mapping: 1='Món nước', 2='Món cơm', 5='Đồ uống & Thuốc lá'
INSERT INTO `meals` (`id`, `category_id`, `name`, `price`, `image_path`) VALUES 
(1, 2, 'Cơm gà xối mỡ', 45000, '/images/com_ga.jpg'),           -- Món cơm
(2, 1, 'Bún bò Huế', 40000, '/images/bun_bo_hue.jpg'),          -- Món nước
(3, 1, 'Phở bò tái', 42000, '/images/pho_bo.jpg'),             -- Món nước
(4, 1, 'Hủ tiếu Nam Vang', 38000, '/images/hu_tieu.jpg'),      -- Món nước

-- Dữ liệu mới: Đồ uống và Thuốc lá (ID 5)
(6, 5, 'Bia Tiger', 22000, '/images/tiger_beer.jpg'),       -- Đồ uống & Thuốc lá
(7, 5, 'Bia Heineken', 25000, '/images/heineken.jpg'),         -- Đồ uống & Thuốc lá
(8, 5, 'Thuốc Lá Marlboro', 35000, '/images/marlboro.jpg'),     -- Đồ uống & Thuốc lá
-- Thêm cà phê vào nhóm Đồ uống & Thuốc lá
(9, 5, 'Cà phê sữa đá', 25000, '/images/ca_phe_sua.jpg');       -- Đồ uống & Thuốc lá

DROP TABLE IF EXISTS `tables`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tables` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `status` varchar(50) DEFAULT 'Trống',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data cho bảng `tables`
--

LOCK TABLES `tables` WRITE;
/*!40000 ALTER TABLE `tables` DISABLE KEYS */;
INSERT INTO `tables` VALUES
(1,'Bàn 1','Trống'),
(2,'Bàn 2','Có khách'),
(3,'Bàn 3','Trống'),
(4,'Bàn 4','Đã đặt'),
(5,'Bàn 5','Trống'),
(6,'VIP 1','Có khách'),
(7,'VIP 2','Trống'),
(8,'Ngoài trời 1','Có khách'),
(9,'Ngoài trời 2','Trống'),
(10,'Ngoài trời 3','Trống');


CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'EMPLOYEE') NOT NULL
);

-- Thêm tài khoản Admin
INSERT INTO users (username, password, role)
VALUES ('admin', 'admin123', 'ADMIN');

-- Thêm tài khoản Nhân viên
INSERT INTO users (username, password, role)
VALUES ('nhanvien', 'nhanvien123', 'EMPLOYEE');


-- (9, 'Old Fashioned') và (10, 'Mojito Chanh') đã bị loại bỏ
/*!40000 ALTER TABLE `meals` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;