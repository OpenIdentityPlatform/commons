CREATE  TABLE IF NOT EXISTS `audittest` (
  `objectid` VARCHAR(38) NOT NULL ,
  `activitydate` VARCHAR(29) NOT NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `transactionid` VARCHAR(56) NOT NULL ,
  `eventname` VARCHAR(255) ,
  `userid` VARCHAR(255) NULL ,
  `custom_integer` INT NULL ,
  `custom_object` TEXT NULL ,
  `custom_array` TEXT NULL ,
  PRIMARY KEY (`objectid`) );
