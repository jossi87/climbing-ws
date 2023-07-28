-- MySQL dump 10.13  Distrib 8.0.33, for Linux (x86_64)
--
-- Host: localhost    Database: buldreinfo
-- ------------------------------------------------------
-- Server version	8.0.33-0ubuntu0.22.04.2

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

--
-- Table structure for table `activity`
--

DROP TABLE IF EXISTS `activity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity` (
  `id` int NOT NULL AUTO_INCREMENT,
  `activity_timestamp` datetime NOT NULL,
  `type` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `problem_id` int NOT NULL,
  `media_id` int DEFAULT NULL,
  `user_id` int DEFAULT NULL,
  `guestbook_id` int DEFAULT NULL,
  `tick_repeat_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  KEY `activity_timestamp_index` (`activity_timestamp`),
  KEY `type_index` (`type`),
  KEY `problem_id_fk_idx` (`problem_id`),
  KEY `media_id_fk_idx` (`media_id`),
  KEY `user_id_fk_idx` (`user_id`),
  KEY `guestbook_id_fk_idx` (`guestbook_id`),
  KEY `tick_repeat_id_fk_idx` (`tick_repeat_id`),
  CONSTRAINT `guestbook_id_fk` FOREIGN KEY (`guestbook_id`) REFERENCES `guestbook` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `media_id_fk` FOREIGN KEY (`media_id`) REFERENCES `media` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `problem_id_fk` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `tick_repeat_id_fk` FOREIGN KEY (`tick_repeat_id`) REFERENCES `tick_repeat` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=718500 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `android_user`
--

DROP TABLE IF EXISTS `android_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `android_user` (
  `unique_id` varchar(16) NOT NULL,
  `account_name` varchar(255) DEFAULT NULL,
  `user_id` int DEFAULT NULL,
  `last_sync` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`unique_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `area`
--

DROP TABLE IF EXISTS `area`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `area` (
  `id` int NOT NULL AUTO_INCREMENT,
  `android_id` bigint NOT NULL,
  `region_id` int NOT NULL,
  `trash` timestamp NULL DEFAULT NULL,
  `trash_by` int DEFAULT '0',
  `locked_admin` int NOT NULL DEFAULT '0',
  `locked_superadmin` int NOT NULL DEFAULT '0',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `description` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `access_info` varchar(4000) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `access_closed` varchar(4000) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `no_dogs_allowed` int NOT NULL DEFAULT '0',
  `latitude` decimal(12,10) DEFAULT NULL,
  `longitude` decimal(12,10) DEFAULT NULL,
  `last_updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `hits` int DEFAULT '0',
  `for_developers` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `area_region_id_idx` (`region_id`),
  CONSTRAINT `area_region_id` FOREIGN KEY (`region_id`) REFERENCES `region` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3376 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fa`
--

DROP TABLE IF EXISTS `fa`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fa` (
  `problem_id` int NOT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`user_id`,`problem_id`),
  KEY `fa_problem_id_idx` (`problem_id`),
  CONSTRAINT `fa_problem_id` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fa_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fa_aid`
--

DROP TABLE IF EXISTS `fa_aid`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fa_aid` (
  `problem_id` int NOT NULL,
  `aid_date` date DEFAULT NULL,
  `aid_description` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`problem_id`),
  CONSTRAINT `fa_aid_problem_id` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fa_aid_user`
--

DROP TABLE IF EXISTS `fa_aid_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fa_aid_user` (
  `problem_id` int NOT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`problem_id`,`user_id`),
  KEY `fa_aid_user_user_id_idx` (`user_id`),
  CONSTRAINT `fa_aid_user_problem_id` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fa_aid_user_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `grade`
--

DROP TABLE IF EXISTS `grade`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `grade` (
  `t` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `grade_id` int NOT NULL,
  `grade` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `group` int NOT NULL,
  `base_no` varchar(3) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`t`,`grade_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `guestbook`
--

DROP TABLE IF EXISTS `guestbook`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `guestbook` (
  `id` int NOT NULL AUTO_INCREMENT,
  `parent_id` int DEFAULT NULL,
  `problem_id` int DEFAULT NULL,
  `title` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `message` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` int DEFAULT NULL,
  `post_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `danger` int NOT NULL DEFAULT '0',
  `resolved` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `guestbook_parent_id_idx` (`parent_id`),
  KEY `guestbook_problem_id_idx` (`problem_id`),
  KEY `guestbook_user_id_idx` (`user_id`),
  CONSTRAINT `guestbook_parent_id` FOREIGN KEY (`parent_id`) REFERENCES `guestbook` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `guestbook_problem_id` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `guestbook_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4498 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `media`
--

DROP TABLE IF EXISTS `media`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `media` (
  `id` int NOT NULL AUTO_INCREMENT,
  `is_movie` int NOT NULL DEFAULT '0',
  `suffix` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `photographer_user_id` int DEFAULT NULL,
  `uploader_user_id` int NOT NULL,
  `date_taken` timestamp NULL DEFAULT NULL,
  `deleted_user_id` int DEFAULT NULL,
  `deleted_timestamp` timestamp NULL DEFAULT NULL,
  `checksum` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `width` int DEFAULT NULL,
  `height` int DEFAULT NULL,
  `date_created` timestamp NULL DEFAULT NULL,
  `description` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `embed_url` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `media_photographer_user_id_idx` (`photographer_user_id`),
  KEY `media_uploader_user_id_idx` (`uploader_user_id`),
  KEY `media_deleted_user_id_idx` (`deleted_user_id`),
  CONSTRAINT `media_deleted_user_id` FOREIGN KEY (`deleted_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `media_photographer_user_id` FOREIGN KEY (`photographer_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `media_uploader_user_id` FOREIGN KEY (`uploader_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=32724 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `media_area`
--

DROP TABLE IF EXISTS `media_area`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `media_area` (
  `id` int NOT NULL AUTO_INCREMENT,
  `media_id` int NOT NULL,
  `area_id` int NOT NULL,
  `trivia` int NOT NULL DEFAULT '0',
  `sorting` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `media_area_media_id_idx` (`media_id`),
  KEY `media_area_area_id_idx` (`area_id`),
  CONSTRAINT `media_area_area_id` FOREIGN KEY (`area_id`) REFERENCES `area` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `media_area_media_id` FOREIGN KEY (`media_id`) REFERENCES `media` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=335 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `media_guestbook`
--

DROP TABLE IF EXISTS `media_guestbook`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `media_guestbook` (
  `id` int NOT NULL AUTO_INCREMENT,
  `media_id` int NOT NULL,
  `guestbook_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `media_guestbook_media_id_idx` (`media_id`),
  KEY `media_guestbook_guestbook_id_idx` (`guestbook_id`),
  CONSTRAINT `media_guestbook_guestbook_id` FOREIGN KEY (`guestbook_id`) REFERENCES `guestbook` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `media_guestbook_media_id` FOREIGN KEY (`media_id`) REFERENCES `media` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=58 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `media_problem`
--

DROP TABLE IF EXISTS `media_problem`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `media_problem` (
  `id` int NOT NULL AUTO_INCREMENT,
  `media_id` int NOT NULL,
  `problem_id` int NOT NULL,
  `milliseconds` int NOT NULL DEFAULT '0',
  `pitch` int DEFAULT NULL,
  `trivia` int NOT NULL DEFAULT '0',
  `sorting` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `media_problem_media_id_idx` (`media_id`),
  KEY `media_problem_problem_id_idx` (`problem_id`),
  CONSTRAINT `media_problem_media_id` FOREIGN KEY (`media_id`) REFERENCES `media` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `media_problem_problem_id` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=19664 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `media_sector`
--

DROP TABLE IF EXISTS `media_sector`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `media_sector` (
  `id` int NOT NULL AUTO_INCREMENT,
  `media_id` int NOT NULL,
  `sector_id` int NOT NULL,
  `trivia` int NOT NULL DEFAULT '0',
  `sorting` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `media_sector_media_id_idx` (`media_id`),
  KEY `media_sector_sector_id_idx` (`sector_id`),
  CONSTRAINT `media_sector_media_id` FOREIGN KEY (`media_id`) REFERENCES `media` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `media_sector_sector_id` FOREIGN KEY (`sector_id`) REFERENCES `sector` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=11702 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `media_svg`
--

DROP TABLE IF EXISTS `media_svg`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `media_svg` (
  `id` int NOT NULL AUTO_INCREMENT,
  `media_id` int NOT NULL,
  `path` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rappel_x` int DEFAULT NULL,
  `rappel_y` int DEFAULT NULL,
  `rappel_bolted` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  KEY `media_svg_media_id_idx` (`media_id`),
  CONSTRAINT `media_svg_media_id` FOREIGN KEY (`media_id`) REFERENCES `media` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=328 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `media_user`
--

DROP TABLE IF EXISTS `media_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `media_user` (
  `media_id` int NOT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`media_id`,`user_id`),
  KEY `media_user_user_id_idx` (`user_id`),
  CONSTRAINT `media_user_media_id` FOREIGN KEY (`media_id`) REFERENCES `media` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `media_user_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `problem`
--

DROP TABLE IF EXISTS `problem`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `problem` (
  `id` int NOT NULL AUTO_INCREMENT,
  `android_id` bigint NOT NULL,
  `sector_id` int NOT NULL,
  `trash` timestamp NULL DEFAULT NULL,
  `trash_by` int DEFAULT '0',
  `locked_admin` int NOT NULL DEFAULT '0',
  `locked_superadmin` int NOT NULL DEFAULT '0',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `rock` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `description` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `nr` int NOT NULL,
  `latitude` decimal(12,10) DEFAULT NULL,
  `longitude` decimal(12,10) DEFAULT NULL,
  `grade` int DEFAULT NULL,
  `fa_date` date DEFAULT NULL,
  `last_updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `type_id` int NOT NULL,
  `created` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `hits` int DEFAULT '0',
  `trivia` varchar(4000) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `starting_altitude` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `aspect` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `route_length` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `descent` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `problem_sector_id_idx` (`sector_id`),
  KEY `problem_type_id_idx` (`type_id`),
  CONSTRAINT `problem_sector_id` FOREIGN KEY (`sector_id`) REFERENCES `sector` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `problem_type_id` FOREIGN KEY (`type_id`) REFERENCES `type` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=17915 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `problem_coordinations_history`
--

DROP TABLE IF EXISTS `problem_coordinations_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `problem_coordinations_history` (
  `id` int NOT NULL AUTO_INCREMENT,
  `problem_id` int NOT NULL,
  `user_id` int NOT NULL,
  `latitude` decimal(12,10) NOT NULL,
  `longitude` decimal(12,10) NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `problem_ch_problem_id_idx` (`problem_id`),
  KEY `problem_ch_user_id_idx` (`user_id`),
  CONSTRAINT `problem_ch_problem_id` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `problem_ch_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=955 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `problem_section`
--

DROP TABLE IF EXISTS `problem_section`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `problem_section` (
  `id` int NOT NULL AUTO_INCREMENT,
  `problem_id` int NOT NULL,
  `nr` int NOT NULL DEFAULT '1',
  `description` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `grade` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `problem_subsection_problem_id_idx` (`problem_id`),
  CONSTRAINT `problem_subsection_problem_id` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=14137 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `region`
--

DROP TABLE IF EXISTS `region`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `region` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `description` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `url` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `polygon_coords` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `latitude` decimal(12,10) DEFAULT NULL,
  `longitude` decimal(12,10) DEFAULT NULL,
  `default_zoom` int DEFAULT NULL,
  `emails` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `last_updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `region_type`
--

DROP TABLE IF EXISTS `region_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `region_type` (
  `region_id` int NOT NULL,
  `type_id` int NOT NULL,
  PRIMARY KEY (`region_id`,`type_id`),
  KEY `region_type_type_id_idx` (`type_id`),
  CONSTRAINT `region_type_region_id` FOREIGN KEY (`region_id`) REFERENCES `region` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `region_type_type_id` FOREIGN KEY (`type_id`) REFERENCES `type` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sector`
--

DROP TABLE IF EXISTS `sector`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sector` (
  `id` int NOT NULL AUTO_INCREMENT,
  `android_id` bigint NOT NULL,
  `area_id` int NOT NULL,
  `trash` timestamp NULL DEFAULT NULL,
  `trash_by` int DEFAULT '0',
  `locked_admin` int NOT NULL DEFAULT '0',
  `locked_superadmin` int NOT NULL DEFAULT '0',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `description` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `access_info` varchar(4000) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `access_closed` varchar(4000) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `parking_latitude` decimal(12,10) DEFAULT NULL,
  `parking_longitude` decimal(12,10) DEFAULT NULL,
  `polygon_coords` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `last_updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `polyline` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `hits` int DEFAULT '0',
  `sorting` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `sector_area_id_idx` (`area_id`),
  CONSTRAINT `sector_area_id` FOREIGN KEY (`area_id`) REFERENCES `area` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4518 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `svg`
--

DROP TABLE IF EXISTS `svg`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `svg` (
  `id` int NOT NULL AUTO_INCREMENT,
  `media_id` int NOT NULL,
  `problem_id` int NOT NULL,
  `path` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `has_anchor` int NOT NULL DEFAULT '0',
  `texts` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `anchors` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `svg_media_id_problem_id_unique` (`media_id`,`problem_id`),
  KEY `svg_media_id_idx` (`media_id`),
  KEY `svg_problem_id_idx` (`problem_id`),
  CONSTRAINT `svg_media_id` FOREIGN KEY (`media_id`) REFERENCES `media` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `svg_problem_id` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10562 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tick`
--

DROP TABLE IF EXISTS `tick`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tick` (
  `id` int NOT NULL AUTO_INCREMENT,
  `problem_id` int NOT NULL,
  `user_id` int NOT NULL,
  `date` date DEFAULT NULL,
  `grade` int DEFAULT NULL,
  `comment` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `stars` int NOT NULL,
  `created` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `tick_problem_id_user_id_unique` (`problem_id`,`user_id`),
  KEY `tick_problem_id_idx` (`problem_id`),
  KEY `tick_user_id_idx` (`user_id`),
  CONSTRAINT `tick_problem_id` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `tick_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=42626 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tick_repeat`
--

DROP TABLE IF EXISTS `tick_repeat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tick_repeat` (
  `id` int NOT NULL AUTO_INCREMENT,
  `tick_id` int NOT NULL,
  `date` date DEFAULT NULL,
  `comment` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `tick_repeat_tick_id_idx` (`tick_id`),
  CONSTRAINT `tick_repeat_tick_id` FOREIGN KEY (`tick_id`) REFERENCES `tick` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `todo`
--

DROP TABLE IF EXISTS `todo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `todo` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `problem_id` int NOT NULL,
  `created` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `todo_fk1_idx` (`user_id`),
  KEY `todo_fk2_idx` (`problem_id`),
  CONSTRAINT `todo_fk1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `todo_fk2` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=9163 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `type`
--

DROP TABLE IF EXISTS `type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `type` (
  `id` int NOT NULL,
  `group` varchar(45) COLLATE utf8mb4_general_ci NOT NULL,
  `type` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `subtype` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `firstname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `lastname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `picture` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_unique_name` (`firstname`,`lastname`)
) ENGINE=InnoDB AUTO_INCREMENT=6221 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_email`
--

DROP TABLE IF EXISTS `user_email`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_email` (
  `user_id` int NOT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`user_id`,`email`),
  UNIQUE KEY `email_UNIQUE` (`email`),
  CONSTRAINT `user_email_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_login`
--

DROP TABLE IF EXISTS `user_login`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_login` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `region_id` int NOT NULL,
  `when` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `headers` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `user_login_user_id_idx` (`user_id`),
  KEY `user_login_region_id_idx` (`region_id`),
  CONSTRAINT `user_login_region_id` FOREIGN KEY (`region_id`) REFERENCES `region` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `user_login_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=215737 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_region`
--

DROP TABLE IF EXISTS `user_region`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_region` (
  `user_id` int NOT NULL,
  `region_id` int NOT NULL,
  `admin_read` int NOT NULL DEFAULT '0',
  `admin_write` int NOT NULL DEFAULT '0',
  `superadmin_read` int NOT NULL DEFAULT '0',
  `superadmin_write` int NOT NULL DEFAULT '0',
  `region_visible` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`user_id`,`region_id`),
  KEY `user_region_region_id_idx` (`region_id`),
  CONSTRAINT `user_region_region_id` FOREIGN KEY (`region_id`) REFERENCES `region` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `user_region_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `firstname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `lastname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `picture` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `use_blue_not_red` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_unique_name` (`firstname`,`lastname`)
) ENGINE=InnoDB AUTO_INCREMENT=3387 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2023-07-28 10:02:08
