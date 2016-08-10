# ************************************************************
# Sequel Pro SQL dump
# Version 4541
#
# http://www.sequelpro.com/
# https://github.com/sequelpro/sequelpro
#
# Host: 127.0.0.1 (MySQL 5.6.25)
# Database: lep-senate
# Generation Time: 2016-07-27 00:36:29 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# Dump of table actions
# ------------------------------------------------------------

DROP TABLE IF EXISTS `actions`;

CREATE TABLE `actions` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `bill_id` int(11) unsigned NOT NULL,
  `date` date NOT NULL,
  `title` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `bill_id` (`bill_id`),
  CONSTRAINT `actions_ibfk_1` FOREIGN KEY (`bill_id`) REFERENCES `bills` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table amendments
# ------------------------------------------------------------

DROP TABLE IF EXISTS `amendments`;

CREATE TABLE `amendments` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `bill_id` int(11) unsigned NOT NULL,
  `sponsor_id` int(11) unsigned DEFAULT NULL,
  `name` varchar(255) NOT NULL DEFAULT '',
  `purpose` varchar(255) NOT NULL DEFAULT '',
  `offered_on` date NOT NULL,
  PRIMARY KEY (`id`),
  KEY `bill_id` (`bill_id`),
  KEY `senator_id` (`sponsor_id`),
  CONSTRAINT `amendments_ibfk_1` FOREIGN KEY (`bill_id`) REFERENCES `bills` (`id`),
  CONSTRAINT `amendments_ibfk_2` FOREIGN KEY (`sponsor_id`) REFERENCES `senators` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table bills
# ------------------------------------------------------------

DROP TABLE IF EXISTS `bills`;

CREATE TABLE `bills` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `sponsor_id` int(11) unsigned NOT NULL,
  `congress_id` int(11) unsigned NOT NULL,
  `name` varchar(255) NOT NULL DEFAULT '',
  `title` varchar(255) NOT NULL DEFAULT '',
  `issue` varchar(255) NOT NULL DEFAULT '',
  `importance` int(11) NOT NULL DEFAULT '2',
  `BILL` int(11) NOT NULL DEFAULT '0',
  `AIC` int(11) NOT NULL DEFAULT '0',
  `ABC` int(11) NOT NULL DEFAULT '0',
  `PASS` int(11) NOT NULL DEFAULT '0',
  `LAW` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `senator_id` (`sponsor_id`),
  KEY `congress_id` (`congress_id`),
  CONSTRAINT `bills_ibfk_1` FOREIGN KEY (`sponsor_id`) REFERENCES `senators` (`id`),
  CONSTRAINT `bills_ibfk_2` FOREIGN KEY (`congress_id`) REFERENCES `congresses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table bills_committees
# ------------------------------------------------------------

DROP TABLE IF EXISTS `bills_committees`;

CREATE TABLE `bills_committees` (
  `bill_id` int(11) unsigned NOT NULL,
  `committee_id` int(11) unsigned NOT NULL,
  PRIMARY KEY (`bill_id`,`committee_id`),
  KEY `committee_id` (`committee_id`),
  CONSTRAINT `bills_committees_ibfk_1` FOREIGN KEY (`bill_id`) REFERENCES `bills` (`id`),
  CONSTRAINT `bills_committees_ibfk_2` FOREIGN KEY (`committee_id`) REFERENCES `committees` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table bills_subcommittees
# ------------------------------------------------------------

DROP TABLE IF EXISTS `bills_subcommittees`;

CREATE TABLE `bills_subcommittees` (
  `bill_id` int(11) unsigned NOT NULL,
  `subcommittee_id` int(11) unsigned NOT NULL,
  PRIMARY KEY (`bill_id`,`subcommittee_id`),
  KEY `subcommittee_id` (`subcommittee_id`),
  CONSTRAINT `bills_subcommittees_ibfk_1` FOREIGN KEY (`bill_id`) REFERENCES `bills` (`id`),
  CONSTRAINT `bills_subcommittees_ibfk_2` FOREIGN KEY (`subcommittee_id`) REFERENCES `subcommittees` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table committees
# ------------------------------------------------------------

DROP TABLE IF EXISTS `committees`;

CREATE TABLE `committees` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table congresses
# ------------------------------------------------------------

DROP TABLE IF EXISTS `congresses`;

CREATE TABLE `congresses` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `num` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `num` (`num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

LOCK TABLES `congresses` WRITE;
/*!40000 ALTER TABLE `congresses` DISABLE KEYS */;

INSERT INTO `congresses` (`id`, `num`)
VALUES
	(15,93),
	(16,94),
	(17,95),
	(18,96),
	(19,97),
	(20,98),
	(21,99),
	(1,100),
	(2,101),
	(3,102),
	(4,103),
	(5,104),
	(6,105),
	(7,106),
	(8,107),
	(9,108),
	(10,109),
	(11,110),
	(12,111),
	(13,112),
	(14,113);

/*!40000 ALTER TABLE `congresses` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table congresses_committees
# ------------------------------------------------------------

DROP TABLE IF EXISTS `congresses_committees`;

CREATE TABLE `congresses_committees` (
  `congress_id` int(11) unsigned NOT NULL,
  `committee_id` int(11) unsigned NOT NULL,
  PRIMARY KEY (`congress_id`,`committee_id`),
  KEY `committee_id` (`committee_id`),
  CONSTRAINT `congresses_committees_ibfk_1` FOREIGN KEY (`congress_id`) REFERENCES `congresses` (`id`),
  CONSTRAINT `congresses_committees_ibfk_2` FOREIGN KEY (`committee_id`) REFERENCES `committees` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table congresses_senators
# ------------------------------------------------------------

DROP TABLE IF EXISTS `congresses_senators`;

CREATE TABLE `congresses_senators` (
  `congress_id` int(11) unsigned NOT NULL,
  `senator_id` int(11) unsigned NOT NULL,
  PRIMARY KEY (`congress_id`,`senator_id`),
  KEY `senator_id` (`senator_id`),
  CONSTRAINT `congresses_senators_ibfk_1` FOREIGN KEY (`congress_id`) REFERENCES `congresses` (`id`),
  CONSTRAINT `congresses_senators_ibfk_2` FOREIGN KEY (`senator_id`) REFERENCES `senators` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table congresses_subcommittees
# ------------------------------------------------------------

DROP TABLE IF EXISTS `congresses_subcommittees`;

CREATE TABLE `congresses_subcommittees` (
  `congress_id` int(11) unsigned NOT NULL,
  `subcommittee_id` int(11) unsigned NOT NULL,
  PRIMARY KEY (`congress_id`,`subcommittee_id`),
  KEY `subcommittee_id` (`subcommittee_id`),
  CONSTRAINT `congresses_subcommittees_ibfk_1` FOREIGN KEY (`congress_id`) REFERENCES `congresses` (`id`),
  CONSTRAINT `congresses_subcommittees_ibfk_2` FOREIGN KEY (`subcommittee_id`) REFERENCES `subcommittees` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table cosponsors
# ------------------------------------------------------------

DROP TABLE IF EXISTS `cosponsors`;

CREATE TABLE `cosponsors` (
  `bill_id` int(11) unsigned NOT NULL,
  `senator_id` int(11) unsigned NOT NULL,
  PRIMARY KEY (`bill_id`,`senator_id`),
  KEY `senator_id` (`senator_id`),
  CONSTRAINT `cosponsors_ibfk_1` FOREIGN KEY (`bill_id`) REFERENCES `bills` (`id`),
  CONSTRAINT `cosponsors_ibfk_2` FOREIGN KEY (`senator_id`) REFERENCES `senators` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table senators
# ------------------------------------------------------------

DROP TABLE IF EXISTS `senators`;

CREATE TABLE `senators` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `first_name` varchar(255) NOT NULL DEFAULT '',
  `last_name` varchar(255) NOT NULL DEFAULT '',
  `middle_name` varchar(255) NOT NULL DEFAULT '',
  `nickname` varchar(255) NOT NULL DEFAULT '',
  `suffix` varchar(255) NOT NULL DEFAULT '',
  `state` char(2) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table subcommittees
# ------------------------------------------------------------

DROP TABLE IF EXISTS `subcommittees`;

CREATE TABLE `subcommittees` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;




/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
