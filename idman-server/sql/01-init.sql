-- MariaDB dump 10.18  Distrib 10.5.7-MariaDB, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: idman_db
-- ------------------------------------------------------
-- Server version	10.5.7-MariaDB-1:10.5.7+maria~bionic

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `passwords`
--

DROP TABLE IF EXISTS `passwords`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `passwords` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  `failed_count` int(11) DEFAULT 0,
  `deleted` tinyint(1) DEFAULT 0,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `roles` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role_id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `service_id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `display_name` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  `deleted` tinyint(1) DEFAULT 0,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_id` (`role_id`),
  KEY `idx_service_id` (`service_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `services`
--

DROP TABLE IF EXISTS `services`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `services` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `service_id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `name` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `deleted` tinyint(1) DEFAULT 0,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_service_id` (`service_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sessions`
--

DROP TABLE IF EXISTS `sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sessions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `session_id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `user_id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `session_type` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `expiry` datetime(3) DEFAULT NULL,
  `partition_id` int(10) NOT NULL,
  `deleted` tinyint(1) DEFAULT 0,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`id`,`partition_id`),
  UNIQUE KEY `uk_session_id` (`session_id`,`partition_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin
 PARTITION BY RANGE (`partition_id`)
(PARTITION `p0` VALUES LESS THAN (1) ENGINE = InnoDB,
 PARTITION `p1` VALUES LESS THAN (2) ENGINE = InnoDB,
 PARTITION `p2` VALUES LESS THAN (3) ENGINE = InnoDB,
 PARTITION `p3` VALUES LESS THAN (4) ENGINE = InnoDB,
 PARTITION `p4` VALUES LESS THAN (5) ENGINE = InnoDB,
 PARTITION `p5` VALUES LESS THAN (6) ENGINE = InnoDB,
 PARTITION `p6` VALUES LESS THAN (7) ENGINE = InnoDB,
 PARTITION `p7` VALUES LESS THAN (8) ENGINE = InnoDB,
 PARTITION `p8` VALUES LESS THAN (9) ENGINE = InnoDB,
 PARTITION `p9` VALUES LESS THAN (10) ENGINE = InnoDB,
 PARTITION `p10` VALUES LESS THAN (11) ENGINE = InnoDB,
 PARTITION `p11` VALUES LESS THAN (12) ENGINE = InnoDB,
 PARTITION `p12` VALUES LESS THAN (13) ENGINE = InnoDB,
 PARTITION `p13` VALUES LESS THAN (14) ENGINE = InnoDB,
 PARTITION `p14` VALUES LESS THAN (15) ENGINE = InnoDB,
 PARTITION `p15` VALUES LESS THAN (16) ENGINE = InnoDB,
 PARTITION `p16` VALUES LESS THAN (17) ENGINE = InnoDB,
 PARTITION `p17` VALUES LESS THAN (18) ENGINE = InnoDB,
 PARTITION `p18` VALUES LESS THAN (19) ENGINE = InnoDB,
 PARTITION `p19` VALUES LESS THAN (20) ENGINE = InnoDB,
 PARTITION `p20` VALUES LESS THAN (21) ENGINE = InnoDB,
 PARTITION `p21` VALUES LESS THAN (22) ENGINE = InnoDB,
 PARTITION `p22` VALUES LESS THAN (23) ENGINE = InnoDB,
 PARTITION `p23` VALUES LESS THAN (24) ENGINE = InnoDB,
 PARTITION `p24` VALUES LESS THAN (25) ENGINE = InnoDB,
 PARTITION `p25` VALUES LESS THAN (26) ENGINE = InnoDB,
 PARTITION `p26` VALUES LESS THAN (27) ENGINE = InnoDB,
 PARTITION `p27` VALUES LESS THAN (28) ENGINE = InnoDB,
 PARTITION `p28` VALUES LESS THAN (29) ENGINE = InnoDB,
 PARTITION `p29` VALUES LESS THAN (30) ENGINE = InnoDB,
 PARTITION `p30` VALUES LESS THAN (31) ENGINE = InnoDB,
 PARTITION `p31` VALUES LESS THAN (32) ENGINE = InnoDB,
 PARTITION `p32` VALUES LESS THAN (33) ENGINE = InnoDB,
 PARTITION `p33` VALUES LESS THAN (34) ENGINE = InnoDB,
 PARTITION `p34` VALUES LESS THAN (35) ENGINE = InnoDB,
 PARTITION `p35` VALUES LESS THAN (36) ENGINE = InnoDB,
 PARTITION `p36` VALUES LESS THAN (37) ENGINE = InnoDB,
 PARTITION `p37` VALUES LESS THAN (38) ENGINE = InnoDB,
 PARTITION `p38` VALUES LESS THAN (39) ENGINE = InnoDB,
 PARTITION `p39` VALUES LESS THAN (40) ENGINE = InnoDB,
 PARTITION `p40` VALUES LESS THAN (41) ENGINE = InnoDB,
 PARTITION `p41` VALUES LESS THAN (42) ENGINE = InnoDB,
 PARTITION `p42` VALUES LESS THAN (43) ENGINE = InnoDB,
 PARTITION `p43` VALUES LESS THAN (44) ENGINE = InnoDB,
 PARTITION `p44` VALUES LESS THAN (45) ENGINE = InnoDB,
 PARTITION `p45` VALUES LESS THAN (46) ENGINE = InnoDB,
 PARTITION `p46` VALUES LESS THAN (47) ENGINE = InnoDB,
 PARTITION `p47` VALUES LESS THAN (48) ENGINE = InnoDB,
 PARTITION `p48` VALUES LESS THAN (49) ENGINE = InnoDB,
 PARTITION `p49` VALUES LESS THAN (50) ENGINE = InnoDB,
 PARTITION `p50` VALUES LESS THAN (51) ENGINE = InnoDB,
 PARTITION `p51` VALUES LESS THAN (52) ENGINE = InnoDB,
 PARTITION `p52` VALUES LESS THAN (53) ENGINE = InnoDB,
 PARTITION `p53` VALUES LESS THAN (54) ENGINE = InnoDB,
 PARTITION `p54` VALUES LESS THAN MAXVALUE ENGINE = InnoDB);
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_auth_state`
--

DROP TABLE IF EXISTS `user_auth_state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_auth_state` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `auth_mode` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `auth_state` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `failed_auth_count` int(11) DEFAULT 0,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_roles`
--

DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_roles` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `service_id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `role_id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `assigned_by` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `deleted` tinyint(1) DEFAULT 0,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_service_user` (`user_id`,`service_id`),
  KEY `idx_service_id` (`service_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_sessions`
--

DROP TABLE IF EXISTS `user_sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_sessions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `session_id` varchar(45) NOT NULL,
  `user_id` varchar(45) NOT NULL,
  `session_type` varchar(45) NOT NULL,
  `expiry` datetime(3) DEFAULT NULL,
  `partition_id` int(10) NOT NULL,
  `deleted` tinyint(1) DEFAULT 0,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`id`,`partition_id`),
  UNIQUE KEY `uk_session_id` (`session_id`,`partition_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
 PARTITION BY RANGE (`partition_id`)
(PARTITION `p0` VALUES LESS THAN (1) ENGINE = InnoDB,
 PARTITION `p1` VALUES LESS THAN (2) ENGINE = InnoDB,
 PARTITION `p2` VALUES LESS THAN (3) ENGINE = InnoDB,
 PARTITION `p3` VALUES LESS THAN (4) ENGINE = InnoDB,
 PARTITION `p4` VALUES LESS THAN (5) ENGINE = InnoDB,
 PARTITION `p5` VALUES LESS THAN (6) ENGINE = InnoDB,
 PARTITION `p6` VALUES LESS THAN (7) ENGINE = InnoDB,
 PARTITION `p7` VALUES LESS THAN (8) ENGINE = InnoDB,
 PARTITION `p8` VALUES LESS THAN (9) ENGINE = InnoDB,
 PARTITION `p9` VALUES LESS THAN (10) ENGINE = InnoDB,
 PARTITION `p10` VALUES LESS THAN (11) ENGINE = InnoDB,
 PARTITION `p11` VALUES LESS THAN (12) ENGINE = InnoDB,
 PARTITION `p12` VALUES LESS THAN (13) ENGINE = InnoDB,
 PARTITION `p13` VALUES LESS THAN (14) ENGINE = InnoDB,
 PARTITION `p14` VALUES LESS THAN (15) ENGINE = InnoDB,
 PARTITION `p15` VALUES LESS THAN (16) ENGINE = InnoDB,
 PARTITION `p16` VALUES LESS THAN (17) ENGINE = InnoDB,
 PARTITION `p17` VALUES LESS THAN (18) ENGINE = InnoDB,
 PARTITION `p18` VALUES LESS THAN (19) ENGINE = InnoDB,
 PARTITION `p19` VALUES LESS THAN (20) ENGINE = InnoDB,
 PARTITION `p20` VALUES LESS THAN (21) ENGINE = InnoDB,
 PARTITION `p21` VALUES LESS THAN (22) ENGINE = InnoDB,
 PARTITION `p22` VALUES LESS THAN (23) ENGINE = InnoDB,
 PARTITION `p23` VALUES LESS THAN (24) ENGINE = InnoDB,
 PARTITION `p24` VALUES LESS THAN (25) ENGINE = InnoDB,
 PARTITION `p25` VALUES LESS THAN (26) ENGINE = InnoDB,
 PARTITION `p26` VALUES LESS THAN (27) ENGINE = InnoDB,
 PARTITION `p27` VALUES LESS THAN (28) ENGINE = InnoDB,
 PARTITION `p28` VALUES LESS THAN (29) ENGINE = InnoDB,
 PARTITION `p29` VALUES LESS THAN (30) ENGINE = InnoDB,
 PARTITION `p30` VALUES LESS THAN (31) ENGINE = InnoDB,
 PARTITION `p31` VALUES LESS THAN (32) ENGINE = InnoDB,
 PARTITION `p32` VALUES LESS THAN (33) ENGINE = InnoDB,
 PARTITION `p33` VALUES LESS THAN (34) ENGINE = InnoDB,
 PARTITION `p34` VALUES LESS THAN (35) ENGINE = InnoDB,
 PARTITION `p35` VALUES LESS THAN (36) ENGINE = InnoDB,
 PARTITION `p36` VALUES LESS THAN (37) ENGINE = InnoDB,
 PARTITION `p37` VALUES LESS THAN (38) ENGINE = InnoDB,
 PARTITION `p38` VALUES LESS THAN (39) ENGINE = InnoDB,
 PARTITION `p39` VALUES LESS THAN (40) ENGINE = InnoDB,
 PARTITION `p40` VALUES LESS THAN (41) ENGINE = InnoDB,
 PARTITION `p41` VALUES LESS THAN (42) ENGINE = InnoDB,
 PARTITION `p42` VALUES LESS THAN (43) ENGINE = InnoDB,
 PARTITION `p43` VALUES LESS THAN (44) ENGINE = InnoDB,
 PARTITION `p44` VALUES LESS THAN (45) ENGINE = InnoDB,
 PARTITION `p45` VALUES LESS THAN (46) ENGINE = InnoDB,
 PARTITION `p46` VALUES LESS THAN (47) ENGINE = InnoDB,
 PARTITION `p47` VALUES LESS THAN (48) ENGINE = InnoDB,
 PARTITION `p48` VALUES LESS THAN (49) ENGINE = InnoDB,
 PARTITION `p49` VALUES LESS THAN (50) ENGINE = InnoDB,
 PARTITION `p50` VALUES LESS THAN (51) ENGINE = InnoDB,
 PARTITION `p51` VALUES LESS THAN (52) ENGINE = InnoDB,
 PARTITION `p52` VALUES LESS THAN (53) ENGINE = InnoDB,
 PARTITION `p53` VALUES LESS THAN (54) ENGINE = InnoDB,
 PARTITION `p54` VALUES LESS THAN MAXVALUE ENGINE = InnoDB);
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `type` varchar(45) COLLATE utf8mb4_bin DEFAULT NULL,
  `auth_state_id` bigint(20) NOT NULL,
  `deleted` tinyint(1) DEFAULT 0,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `fk_auth` (`auth_state_id`),
  CONSTRAINT `fk_auth` FOREIGN KEY (`auth_state_id`) REFERENCES `user_auth_state` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2021-04-20 12:45:10
