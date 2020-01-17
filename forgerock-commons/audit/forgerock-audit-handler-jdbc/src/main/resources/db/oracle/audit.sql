-- -----------------------------------------------------
-- Table auditaccess
-- -----------------------------------------------------
PROMPT Creating Table auditaccess ...
CREATE TABLE auditaccess (
  id VARCHAR2(56 CHAR) NOT NULL,
  timestamp_ VARCHAR2(29 CHAR) NOT NULL,
  transactionid VARCHAR2(255 CHAR) NOT NULL,
  eventname VARCHAR2(255 CHAR),
  userid VARCHAR2(255 CHAR),
  trackingids CLOB,
  server_ip VARCHAR2(40 CHAR),
  server_port VARCHAR2(5 CHAR),
  client_host VARCHAR2(255 CHAR),
  client_ip VARCHAR2(40 CHAR),
  client_port VARCHAR2(5 CHAR),
  request_protocol VARCHAR2(255 CHAR) NULL ,
  request_operation VARCHAR2(255 CHAR) NULL ,
  request_detail CLOB NULL ,
  http_request_secure VARCHAR2(255 CHAR) NULL ,
  http_request_method VARCHAR2(7 CHAR) NULL ,
  http_request_path VARCHAR2(255 CHAR) NULL ,
  http_request_queryparameters CLOB(2M) NULL ,
  http_request_headers CLOB NULL ,
  http_request_cookies CLOB NULL ,
  http_response_headers CLOB NULL ,
  response_status VARCHAR2(10 CHAR) NULL ,
  response_statuscode VARCHAR2(255 CHAR) NULL ,
  response_elapsedtime VARCHAR2(255 CHAR) NULL ,
  response_elapsedtimeunits VARCHAR2(255 CHAR) NULL
);


COMMENT ON COLUMN auditaccess.timestamp IS 'Date format: 2011-09-09T14:58:17.654+02:00'
;

PROMPT Creating Primary Key Constraint PRIMARY_ACCESS on table auditaccess ...
ALTER TABLE auditaccess
ADD CONSTRAINT PRIMARY_ACCESS PRIMARY KEY
(
  id
)
ENABLE
;

-- -----------------------------------------------------
-- Table auditauthentication
-- -----------------------------------------------------
PROMPT Creating TABLE auditauthentication ...
CREATE TABLE auditauthentication (
  id VARCHAR2(56 CHAR) NOT NULL,
  timestamp_ VARCHAR2(29 CHAR) NOT NULL,
  transactionid VARCHAR2(255 CHAR) NOT NULL,
  eventname VARCHAR2(255 CHAR),
  userid VARCHAR2(255 CHAR),
  trackingids CLOB,
  result VARCHAR2(255 CHAR),
  principals CLOB,
  context CLOB,
  entries CLOB
);

COMMENT ON COLUMN auditauthentication.timestamp IS 'Date format: 2011-09-09T14:58:17.654+02:00'
;

PROMPT Creating PRIMARY KEY CONSTRAINT PRIMARY_AUTHENTICATION ON TABLE auditauthentication ...
ALTER TABLE auditauthentication
ADD CONSTRAINT PRIMARY_AUTHENTICATION PRIMARY KEY
(
  id
)
ENABLE
;


-- -----------------------------------------------------
-- Table auditactivity
-- -----------------------------------------------------
PROMPT Creating Table auditactivity ...
CREATE TABLE auditactivity (
  id VARCHAR2(56 CHAR) NOT NULL,
  timestamp_ VARCHAR2(29 CHAR) NOT NULL,
  transactionid VARCHAR2(255 CHAR) NOT NULL,
  eventname VARCHAR2(255 CHAR),
  userid VARCHAR2(255 CHAR),
  trackingids CLOB,
  runas VARCHAR2(255 CHAR),
  objectid VARCHAR2(255 CHAR) NULL ,
  operation VARCHAR2(255 CHAR) NULL ,
  beforeObject CLOB,
  afterObject CLOB,
  changedfields VARCHAR2(255 CHAR),
  rev VARCHAR2(255 CHAR)
);


COMMENT ON COLUMN auditactivity.timestamp IS 'Date format: 2011-09-09T14:58:17.654+02:00'
;

PROMPT Creating Primary Key Constraint PRIMARY_ACTIVITY on table auditactivity ...
ALTER TABLE auditactivity
ADD CONSTRAINT PRIMARY_ACTIVITY PRIMARY KEY
(
  id
)
ENABLE
;
PROMPT Creating Index idx_activity_txid on auditactivity ...
CREATE INDEX idx_activity_txid ON auditactivity
(
  transactionid
)
;

-- -----------------------------------------------------
-- Table auditconfig
-- -----------------------------------------------------
PROMPT Creating Table auditconfig ...
CREATE TABLE auditconfig (
  id VARCHAR2(56 CHAR) NOT NULL,
  timestamp_ VARCHAR2(29 CHAR) NOT NULL,
  transactionid VARCHAR2(255 CHAR) NOT NULL,
  eventname VARCHAR2(255 CHAR),
  userid VARCHAR2(255 CHAR),
  trackingids CLOB,
  runas VARCHAR2(255 CHAR),
  objectid VARCHAR2(255 CHAR) NULL ,
  operation VARCHAR2(255 CHAR) NULL ,
  beforeObject CLOB,
  afterObject CLOB,
  changedfields VARCHAR2(255 CHAR),
  rev VARCHAR2(255 CHAR)
);


COMMENT ON COLUMN auditconfig.timestamp IS 'Date format: 2011-09-09T14:58:17.654+02:00'
;

PROMPT Creating Primary Key Constraint PRIMARY_CONFIG on table auditconfig ...
ALTER TABLE auditconfig
ADD CONSTRAINT PRIMARY_CONFIG PRIMARY KEY
(
  id
)
ENABLE
;
PROMPT Creating Index idx_config_txid on auditconfig ...
CREATE INDEX idx_config_txid ON auditconfig
(
  transactionid
)
;

COMMIT;
