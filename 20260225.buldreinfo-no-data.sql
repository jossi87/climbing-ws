-- MySQL dump 10.13  Distrib 8.0.43, for Linux (x86_64)
--
-- Host: localhost    Database: buldreinfo
-- ------------------------------------------------------
-- Server version	8.0.43-0ubuntu0.22.04.2

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
  KEY `activity_timestamp_problem_id_ix` (`activity_timestamp` DESC,`problem_id` DESC),
  CONSTRAINT `guestbook_id_fk` FOREIGN KEY (`guestbook_id`) REFERENCES `guestbook` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `media_id_fk` FOREIGN KEY (`media_id`) REFERENCES `media` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `problem_id_fk` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `tick_repeat_id_fk` FOREIGN KEY (`tick_repeat_id`) REFERENCES `tick_repeat` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1515842 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `sun_from_hour` int DEFAULT NULL,
  `sun_to_hour` int DEFAULT NULL,
  `coordinates_id` int DEFAULT NULL,
  `last_updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `hits` int DEFAULT '0',
  `for_developers` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `area_region_id_idx` (`region_id`),
  KEY `area_coordinates_id_idx` (`coordinates_id`),
  CONSTRAINT `area_coordinates_id` FOREIGN KEY (`coordinates_id`) REFERENCES `coordinates` (`id`) ON DELETE SET NULL ON UPDATE SET NULL,
  CONSTRAINT `area_region_id` FOREIGN KEY (`region_id`) REFERENCES `region` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3531 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `compass_direction`
--

DROP TABLE IF EXISTS `compass_direction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `compass_direction` (
  `id` int NOT NULL AUTO_INCREMENT,
  `direction` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `coordinates`
--

DROP TABLE IF EXISTS `coordinates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coordinates` (
  `id` int NOT NULL AUTO_INCREMENT,
  `latitude` decimal(13,10) DEFAULT NULL,
  `longitude` decimal(13,10) DEFAULT NULL,
  `elevation` decimal(6,2) DEFAULT NULL,
  `elevation_source` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `coordinates_lat_lng_unique` (`latitude`,`longitude`)
) ENGINE=InnoDB AUTO_INCREMENT=135385 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `external_link`
--

DROP TABLE IF EXISTS `external_link`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `external_link` (
  `id` int NOT NULL AUTO_INCREMENT,
  `url` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=58 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `external_link_area`
--

DROP TABLE IF EXISTS `external_link_area`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `external_link_area` (
  `external_link_id` int NOT NULL,
  `area_id` int NOT NULL,
  PRIMARY KEY (`external_link_id`,`area_id`),
  KEY `external_link_area_area_id_idx` (`area_id`),
  CONSTRAINT `external_link_area_area_id` FOREIGN KEY (`area_id`) REFERENCES `area` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `external_link_area_external_link_id` FOREIGN KEY (`external_link_id`) REFERENCES `external_link` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `external_link_problem`
--

DROP TABLE IF EXISTS `external_link_problem`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `external_link_problem` (
  `external_link_id` int NOT NULL,
  `problem_id` int NOT NULL,
  PRIMARY KEY (`external_link_id`,`problem_id`),
  KEY `external_link_problem_problem_id_idx` (`problem_id`),
  CONSTRAINT `external_link_problem_external_link_id` FOREIGN KEY (`external_link_id`) REFERENCES `external_link` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `external_link_problem_problem_id` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `external_link_sector`
--

DROP TABLE IF EXISTS `external_link_sector`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `external_link_sector` (
  `external_link_id` int NOT NULL,
  `sector_id` int NOT NULL,
  PRIMARY KEY (`external_link_id`,`sector_id`),
  KEY `external_link_sector_sector_id_idx` (`sector_id`),
  CONSTRAINT `external_link_sector_external_link_id` FOREIGN KEY (`external_link_id`) REFERENCES `external_link` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `external_link_sector_sector_id` FOREIGN KEY (`sector_id`) REFERENCES `sector` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=4957 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
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
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
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
) ENGINE=InnoDB AUTO_INCREMENT=41512 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=647 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=139 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=25078 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=13380 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=671 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `broken` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `trash` timestamp NULL DEFAULT NULL,
  `trash_by` int DEFAULT '0',
  `locked_admin` int NOT NULL DEFAULT '0',
  `locked_superadmin` int NOT NULL DEFAULT '0',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `rock` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `description` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `nr` int NOT NULL,
  `coordinates_id` int DEFAULT NULL,
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
  KEY `problem_coordinates_id_idx` (`coordinates_id`),
  KEY `problem_grade_id_idx` (`grade`),
  CONSTRAINT `problem_coordinates_id` FOREIGN KEY (`coordinates_id`) REFERENCES `coordinates` (`id`) ON DELETE SET NULL ON UPDATE SET NULL,
  CONSTRAINT `problem_sector_id` FOREIGN KEY (`sector_id`) REFERENCES `sector` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `problem_type_id` FOREIGN KEY (`type_id`) REFERENCES `type` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=22345 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=22376 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
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
  `latitude` decimal(13,10) DEFAULT NULL,
  `longitude` decimal(13,10) DEFAULT NULL,
  `default_zoom` int DEFAULT NULL,
  `emails` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `last_updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `region_outline`
--

DROP TABLE IF EXISTS `region_outline`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `region_outline` (
  `id` int NOT NULL AUTO_INCREMENT,
  `region_id` int NOT NULL,
  `coordinates_id` int NOT NULL,
  `sorting` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `region_outline_unique_region_id_coordinate_id` (`region_id`,`coordinates_id`),
  UNIQUE KEY `region_outline_unique_region_id_sorting` (`region_id`,`sorting`),
  KEY `region_outline_region_id_fk_idx` (`region_id`),
  KEY `region_outline_coordinates_id_idx` (`coordinates_id`),
  CONSTRAINT `region_outline_coordinates_id` FOREIGN KEY (`coordinates_id`) REFERENCES `coordinates` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `region_outline_region_id` FOREIGN KEY (`region_id`) REFERENCES `region` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7646 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `sun_from_hour` int DEFAULT NULL,
  `sun_to_hour` int DEFAULT NULL,
  `parking_coordinates_id` int DEFAULT NULL,
  `compass_direction_id_calculated` int DEFAULT NULL,
  `compass_direction_id_manual` int DEFAULT NULL,
  `last_updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `hits` int DEFAULT '0',
  `sorting` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `sector_area_id_idx` (`area_id`),
  KEY `sector_parking_coordinates_id_fk_idx` (`parking_coordinates_id`),
  KEY `sector_compass_direction_id_calculated_idx` (`compass_direction_id_calculated`),
  KEY `sector_compass_direction_id_manual_idx` (`compass_direction_id_manual`),
  CONSTRAINT `sector_area_id` FOREIGN KEY (`area_id`) REFERENCES `area` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `sector_compass_direction_id_calculated` FOREIGN KEY (`compass_direction_id_calculated`) REFERENCES `compass_direction` (`id`) ON DELETE SET NULL ON UPDATE SET NULL,
  CONSTRAINT `sector_compass_direction_id_manual` FOREIGN KEY (`compass_direction_id_manual`) REFERENCES `compass_direction` (`id`) ON DELETE SET NULL ON UPDATE SET NULL,
  CONSTRAINT `sector_parking_coordinates_id` FOREIGN KEY (`parking_coordinates_id`) REFERENCES `coordinates` (`id`) ON DELETE SET NULL ON UPDATE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=4972 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sector_approach`
--

DROP TABLE IF EXISTS `sector_approach`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sector_approach` (
  `id` int NOT NULL AUTO_INCREMENT,
  `sector_id` int NOT NULL,
  `coordinates_id` int NOT NULL,
  `sorting` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `sector_approach_unique_sector_id_coordinates_id` (`sector_id`,`coordinates_id`) /*!80000 INVISIBLE */,
  UNIQUE KEY `sector_approach_unique_sector_id_sorting` (`sector_id`,`sorting`),
  KEY `sector_approach_sector_id_fk_idx` (`sector_id`),
  KEY `sector_approach_coordinates_id_idx` (`coordinates_id`),
  CONSTRAINT `sector_approach_coordinates_id` FOREIGN KEY (`coordinates_id`) REFERENCES `coordinates` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `sector_approach_sector_id` FOREIGN KEY (`sector_id`) REFERENCES `sector` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=83628 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sector_descent`
--

DROP TABLE IF EXISTS `sector_descent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sector_descent` (
  `id` int NOT NULL AUTO_INCREMENT,
  `sector_id` int NOT NULL,
  `coordinates_id` int NOT NULL,
  `sorting` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `sector_descent_unique_sector_id_coordinates_id` (`sector_id`,`coordinates_id`) /*!80000 INVISIBLE */,
  UNIQUE KEY `sector_descent_unique_sector_id_sorting` (`sector_id`,`sorting`),
  KEY `sector_descent_sector_id_fk_idx` (`sector_id`),
  KEY `sector_descent_coordinates_id_idx` (`coordinates_id`),
  CONSTRAINT `sector_descent_coordinates_id` FOREIGN KEY (`coordinates_id`) REFERENCES `coordinates` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `sector_descent_sector_id` FOREIGN KEY (`sector_id`) REFERENCES `sector` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=70947 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sector_outline`
--

DROP TABLE IF EXISTS `sector_outline`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sector_outline` (
  `id` int NOT NULL AUTO_INCREMENT,
  `sector_id` int NOT NULL,
  `coordinates_id` int NOT NULL,
  `sorting` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `sector_outline_unique_sector_id_coordinates_id` (`sector_id`,`coordinates_id`) /*!80000 INVISIBLE */,
  UNIQUE KEY `sector_outline_unique_sector_id_sorting` (`sector_id`,`sorting`),
  KEY `sector_outline_sector_id_fk_idx` (`sector_id`),
  KEY `sector_outline_coordinates_id_idx` (`coordinates_id`),
  CONSTRAINT `sector_outline_coordinates_id` FOREIGN KEY (`coordinates_id`) REFERENCES `coordinates` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `sector_outline_sector_id` FOREIGN KEY (`sector_id`) REFERENCES `sector` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=21669 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `trad_belay_stations` varchar(4000) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `pitch` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `svg_unique` (`media_id`,`problem_id`,`pitch`),
  KEY `svg_media_id_idx` (`media_id`),
  KEY `svg_problem_id_idx` (`problem_id`),
  CONSTRAINT `svg_media_id` FOREIGN KEY (`media_id`) REFERENCES `media` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `svg_problem_id` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=15264 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=74132 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=180 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=18711 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
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
  `email_visible_to_all` int NOT NULL DEFAULT '0',
  `media_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `user_media_id_idx` (`media_id`),
  CONSTRAINT `user_media_id` FOREIGN KEY (`media_id`) REFERENCES `media` (`id`) ON DELETE SET NULL ON UPDATE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=12302 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=734988 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
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

-- Dump completed on 2026-02-25 18:39:03
