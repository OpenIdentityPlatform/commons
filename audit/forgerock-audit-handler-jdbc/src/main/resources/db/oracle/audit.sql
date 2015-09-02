-- -----------------------------------------------------
-- Table auditaccess
-- -----------------------------------------------------
PROMPT Creating Table auditaccess ...
CREATE TABLE auditaccess (
  id VARCHAR2(38 CHAR) NOT NULL,
  timestamp VARCHAR2(29 CHAR) NOT NULL,
  transactionid VARCHAR2(56 CHAR) NOT NULL,
  eventname VARCHAR2(255 CHAR),
  authentication_id VARCHAR2(255 CHAR),
  server_ip VARCHAR2(40 CHAR),
  server_port VARCHAR2(5 CHAR),
  client_host VARCHAR2(255 CHAR),
  client_ip VARCHAR2(40 CHAR),
  client_port VARCHAR2(5 CHAR),
  authorizationid_id CLOB,
  authorizationid_roles VARCHAR2(1024 CHAR),
  authorizationid_component VARCHAR2(255 CHAR),
  resource_uri VARCHAR2(255 CHAR),
  resource_protocol VARCHAR2(10 CHAR),
  resource_method VARCHAR2(10 CHAR),
  resource_detail VARCHAR2(255 CHAR),
  http_method VARCHAR2(10 CHAR),
  http_path VARCHAR2(255 CHAR),
  http_querystring VARCHAR2(255 CHAR),
  http_headers CLOB,
  status VARCHAR2(20 CHAR),
  elapsedtime VARCHAR2(13 CHAR)
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
  id VARCHAR2(38 CHAR) NOT NULL,
  transactionid VARCHAR2(56 CHAR) NOT NULL,
  timestamp VARCHAR2(29 CHAR) NOT NULL,
  authentication_id VARCHAR2(255 CHAR),
  eventname VARCHAR2(50 CHAR),
  result VARCHAR2(255 CHAR),
  principals CLOB,
  context CLOB,
  sessionid VARCHAR2(255 CHAR),
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
  id VARCHAR2(38 CHAR) NOT NULL,
  timestamp VARCHAR2(29 CHAR) NOT NULL,
  transactionid VARCHAR2(56 CHAR) NOT NULL,
  eventname VARCHAR2(255 CHAR),
  authentication_id VARCHAR2(255 CHAR),
  runas VARCHAR2(255 CHAR),
  resource_uri VARCHAR2(255 CHAR),
  resource_protocol VARCHAR2(10 CHAR),
  resource_method VARCHAR2(10 CHAR),
  resource_detail VARCHAR2(255 CHAR),
  before CLOB,
  after CLOB,
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
  id VARCHAR2(38 CHAR) NOT NULL,
  timestamp VARCHAR2(29 CHAR) NOT NULL,
  transactionid VARCHAR2(56 CHAR) NOT NULL,
  eventname VARCHAR2(255 CHAR),
  authentication_id VARCHAR2(255 CHAR),
  runas VARCHAR2(255 CHAR),
  resource_uri VARCHAR2(255 CHAR),
  resource_protocol VARCHAR2(10 CHAR),
  resource_method VARCHAR2(10 CHAR),
  resource_detail VARCHAR2(255 CHAR),
  before CLOB,
  after CLOB,
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
