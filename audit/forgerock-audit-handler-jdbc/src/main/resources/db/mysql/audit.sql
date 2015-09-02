SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

CREATE SCHEMA IF NOT EXISTS `audit` DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ;
USE `audit` ;

-- -----------------------------------------------------
-- Table `audit`.`auditauthentication`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `audit`.`auditauthentication` (
  `id` VARCHAR(38) NOT NULL ,
  `transactionid` VARCHAR(38) NULL ,
  `timestamp_` VARCHAR(29) NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `authentication_id` VARCHAR(255) NULL ,
  `eventname` VARCHAR(50) NULL ,
  `result` VARCHAR(255) NULL ,
  `principals` TEXT ,
  `context` TEXT ,
  `sessionid` VARCHAR(255) ,
  `entries` TEXT ,
  PRIMARY KEY (`id`)
)
  ENGINE = InnoDB;

-- -----------------------------------------------------
-- Table `audit`.`auditactivity`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `audit`.`auditactivity` (
  `objectid` VARCHAR(38) NOT NULL ,
  `timestamp_` VARCHAR(29) NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `transactionid` VARCHAR(38) NULL ,
  `eventname` VARCHAR(255) NULL ,
  `authentication_id` VARCHAR(255) NULL ,
  `runas` VARCHAR(255) NULL ,
  `resource_uri` VARCHAR(255) NULL ,
  `resource_protocol` VARCHAR(10) NULL ,
  `resource_method` VARCHAR(10) NULL ,
  `resource_detail` VARCHAR(255) NULL ,
  `before` MEDIUMTEXT NULL ,
  `after` MEDIUMTEXT NULL ,
  `changedfields` VARCHAR(255) NULL ,
  `rev` VARCHAR(255) NULL ,
  PRIMARY KEY (`id`) ,
  INDEX `idx_auditactivity_transactionid` (`transactionid` ASC)
)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `audit`.`auditaccess`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `audit`.`auditaccess` (
  `id` VARCHAR(38) NOT NULL ,
  `timestamp_` VARCHAR(29) NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `transactionid` VARCHAR(38) NULL ,
  `eventname` VARCHAR(255) ,
  `server_ip` VARCHAR(40) ,
  `server_port` VARCHAR(5) ,
  `client_host` VARCHAR(255) ,
  `client_ip` VARCHAR(40) ,
  `client_port` VARCHAR(5) ,
  `authentication_id` VARCHAR(255) NULL ,
  `authorizationid_id` TEXT NULL ,
  `authorizationid_roles` VARCHAR(1024) NULL ,
  `authorizationid_component` VARCHAR(255) NULL ,
  `resource_uri` VARCHAR(255) NULL ,
  `resource_protocol` VARCHAR(10) NULL ,
  `resource_method` VARCHAR(10) NULL ,
  `resource_detail` VARCHAR(255) NULL ,
  `http_method` VARCHAR(10) NULL ,
  `http_path` VARCHAR(255) NULL ,
  `http_querystring` VARCHAR(255) NULL ,
  `http_headers` TEXT ,
  `status` VARCHAR(20) NULL ,
  `elapsedtime` VARCHAR(13) NULL ,
  PRIMARY KEY (`id`),
  INDEX `idx_auditaccess_status` (`status` ASC),
  INDEX `idx_auditaccess_authorizationid_id` (`authorizationid_id`(28) ASC) )
ENGINE = InnoDB;

-- -----------------------------------------------------
-- Table `audit`.`auditconfig`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `audit`.`auditconfig` (
  `id` VARCHAR(38) NOT NULL ,
  `timestamp_` VARCHAR(29) NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `transactionid` VARCHAR(38) NULL ,
  `eventname` VARCHAR(255) NULL ,
  `authentication_id` VARCHAR(255) NULL ,
  `runas` VARCHAR(255) NULL ,
  `resource_uri` VARCHAR(255) NULL ,
  `resource_protocol` VARCHAR(10) NULL ,
  `resource_method` VARCHAR(10) NULL ,
  `resource_detail` VARCHAR(255) NULL ,
  `before` MEDIUMTEXT NULL ,
  `after` MEDIUMTEXT NULL ,
  `changedfields` VARCHAR(255) NULL ,
  `rev` VARCHAR(255) NULL ,
  PRIMARY KEY (`id`) ,
  INDEX `idx_auditactivity_transactionid` (`transactionid` ASC)
)
ENGINE = InnoDB;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;

-- -------------------------------------------
-- audit database user
-- ------------------------------------------
GRANT ALL PRIVILEGES on audit.* TO audit IDENTIFIED BY 'audit';
GRANT ALL PRIVILEGES on audit.* TO audit@'%' IDENTIFIED BY 'audit';
GRANT ALL PRIVILEGES on audit.* TO audit@localhost IDENTIFIED BY 'audit';
