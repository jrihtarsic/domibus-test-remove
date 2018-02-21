-- *********************************************************************
-- Update Database Script
-- *********************************************************************
-- Change Log: src/main/resources/db/changelog-3.3.1.xml
-- Ran at: 23/01/18 11:08
-- Against: null@offline:oracle?version=11.2.0&changeLogFile=C:\PGM\DEV\workspace\domibusCEF\Domibus-MSH-db\target/liquibase/changelog-3.3.1.oracle
-- Liquibase version: 3.4.2
-- *********************************************************************

SET DEFINE OFF;

-- Changeset src/main/resources/db/changelog-3.0.xml::changelog-3.0::Christian Koch
CREATE TABLE qrtz_blob_triggers (SCHED_NAME VARCHAR2(120) NOT NULL, TRIGGER_NAME VARCHAR2(200) NOT NULL, TRIGGER_GROUP VARCHAR2(200) NOT NULL, BLOB_DATA BLOB);

CREATE TABLE qrtz_calendars (SCHED_NAME VARCHAR2(120) NOT NULL, CALENDAR_NAME VARCHAR2(200) NOT NULL, CALENDAR_BLOB BLOB NOT NULL);

CREATE TABLE qrtz_cron_triggers (SCHED_NAME VARCHAR2(120) NOT NULL, TRIGGER_NAME VARCHAR2(200) NOT NULL, TRIGGER_GROUP VARCHAR2(200) NOT NULL, CRON_EXPRESSION VARCHAR2(120) NOT NULL, TIME_ZONE_ID VARCHAR2(80));

CREATE TABLE qrtz_fired_triggers (SCHED_NAME VARCHAR2(120) NOT NULL, ENTRY_ID VARCHAR2(95) NOT NULL, TRIGGER_NAME VARCHAR2(200) NOT NULL, TRIGGER_GROUP VARCHAR2(200) NOT NULL, INSTANCE_NAME VARCHAR2(200) NOT NULL, FIRED_TIME NUMBER(38, 0) NOT NULL, SCHED_TIME NUMBER(38, 0) NOT NULL, PRIORITY NUMBER(10) NOT NULL, STATE VARCHAR2(16) NOT NULL, JOB_NAME VARCHAR2(200), JOB_GROUP VARCHAR2(200), IS_NONCONCURRENT NUMBER(1), REQUESTS_RECOVERY NUMBER(1));

CREATE TABLE qrtz_job_details (SCHED_NAME VARCHAR2(120) NOT NULL, JOB_NAME VARCHAR2(200) NOT NULL, JOB_GROUP VARCHAR2(200) NOT NULL, DESCRIPTION VARCHAR2(250), JOB_CLASS_NAME VARCHAR2(250) NOT NULL, IS_DURABLE NUMBER(1) NOT NULL, IS_NONCONCURRENT NUMBER(1) NOT NULL, IS_UPDATE_DATA NUMBER(1) NOT NULL, REQUESTS_RECOVERY NUMBER(1) NOT NULL, JOB_DATA BLOB);

CREATE TABLE qrtz_locks (SCHED_NAME VARCHAR2(120) NOT NULL, LOCK_NAME VARCHAR2(40) NOT NULL);

CREATE TABLE qrtz_paused_trigger_grps (SCHED_NAME VARCHAR2(120) NOT NULL, TRIGGER_GROUP VARCHAR2(200) NOT NULL);

CREATE TABLE qrtz_scheduler_state (SCHED_NAME VARCHAR2(120) NOT NULL, INSTANCE_NAME VARCHAR2(200) NOT NULL, LAST_CHECKIN_TIME NUMBER(38, 0) NOT NULL, CHECKIN_INTERVAL NUMBER(38, 0) NOT NULL);

CREATE TABLE qrtz_simple_triggers (SCHED_NAME VARCHAR2(120) NOT NULL, TRIGGER_NAME VARCHAR2(200) NOT NULL, TRIGGER_GROUP VARCHAR2(200) NOT NULL, REPEAT_COUNT NUMBER(38, 0) NOT NULL, REPEAT_INTERVAL NUMBER(38, 0) NOT NULL, TIMES_TRIGGERED NUMBER(38, 0) NOT NULL);

CREATE TABLE qrtz_simprop_triggers (SCHED_NAME VARCHAR2(120) NOT NULL, TRIGGER_NAME VARCHAR2(200) NOT NULL, TRIGGER_GROUP VARCHAR2(200) NOT NULL, STR_PROP_1 VARCHAR2(512), STR_PROP_2 VARCHAR2(512), STR_PROP_3 VARCHAR2(512), INT_PROP_1 NUMBER(10), INT_PROP_2 NUMBER(10), LONG_PROP_1 NUMBER(38, 0), LONG_PROP_2 NUMBER(38, 0), DEC_PROP_1 DECIMAL(13, 4), DEC_PROP_2 DECIMAL(13, 4), BOOL_PROP_1 NUMBER(1), BOOL_PROP_2 NUMBER(1));

CREATE TABLE qrtz_triggers (SCHED_NAME VARCHAR2(120) NOT NULL, TRIGGER_NAME VARCHAR2(200) NOT NULL, TRIGGER_GROUP VARCHAR2(200) NOT NULL, JOB_NAME VARCHAR2(200) NOT NULL, JOB_GROUP VARCHAR2(200) NOT NULL, DESCRIPTION VARCHAR2(250), NEXT_FIRE_TIME NUMBER(38, 0), PREV_FIRE_TIME NUMBER(38, 0), PRIORITY NUMBER(10), TRIGGER_STATE VARCHAR2(16) NOT NULL, TRIGGER_TYPE VARCHAR2(8) NOT NULL, START_TIME NUMBER(38, 0) NOT NULL, END_TIME NUMBER(38, 0), CALENDAR_NAME VARCHAR2(200), MISFIRE_INSTR NUMBER(5), JOB_DATA BLOB);

CREATE TABLE tb_action (ID_PK NUMBER(10) NOT NULL, NAME VARCHAR2(255), VALUE VARCHAR2(255), FK_BUSINESSPROCESS NUMBER(10), CONSTRAINT PK_TB_ACTION PRIMARY KEY (ID_PK));

CREATE TABLE tb_agreement (ID_PK NUMBER(10) NOT NULL, NAME VARCHAR2(255), TYPE VARCHAR2(255), VALUE VARCHAR2(255), FK_BUSINESSPROCESS NUMBER(10), CONSTRAINT PK_TB_AGREEMENT PRIMARY KEY (ID_PK));

CREATE TABLE tb_backend_filter (ID_PK NUMBER(10) NOT NULL, BACKEND_NAME VARCHAR2(255), PRIORITY NUMBER(10), CONSTRAINT PK_TB_BACKEND_FILTER PRIMARY KEY (ID_PK));

CREATE TABLE tb_business_process (ID_PK NUMBER(10) NOT NULL, CONSTRAINT PK_TB_BUSINESS_PROCESS PRIMARY KEY (ID_PK));

CREATE TABLE tb_configuration (ID_PK NUMBER(10) NOT NULL, FK_BUSINESSPROCESSES NUMBER(10), FK_PARTY NUMBER(10), CONSTRAINT PK_TB_CONFIGURATION PRIMARY KEY (ID_PK));

CREATE TABLE tb_error (ID_PK NUMBER(10) NOT NULL, CATEGORY VARCHAR2(255), DESCRIPTION_LANG VARCHAR2(255), DESCRIPTION_VALUE VARCHAR2(255), ERROR_CODE VARCHAR2(255), ERROR_DETAIL CLOB, ORIGIN VARCHAR2(255), REF_TO_MESSAGE_ID VARCHAR2(255), SEVERITY VARCHAR2(255), SHORT_DESCRIPTION VARCHAR2(255), SIGNALMESSAGE_ID NUMBER(10), CONSTRAINT PK_TB_ERROR PRIMARY KEY (ID_PK));

CREATE TABLE tb_error_handling (ID_PK NUMBER(10) NOT NULL, BUSINESS_ERROR_NOTIFY_CONSUMER NUMBER(1), BUSINESS_ERROR_NOTIFY_PRODUCER NUMBER(1), DELIVERY_FAIL_NOTIFY_PRODUCER NUMBER(1), ERROR_AS_RESPONSE NUMBER(1), NAME VARCHAR2(255), FK_BUSINESSPROCESS NUMBER(10), CONSTRAINT PK_TB_ERROR_HANDLING PRIMARY KEY (ID_PK));

CREATE TABLE tb_error_log (ID_PK NUMBER(10) NOT NULL, ERROR_CODE VARCHAR2(255), ERROR_DETAIL VARCHAR2(255), ERROR_SIGNAL_MESSAGE_ID VARCHAR2(255), MESSAGE_IN_ERROR_ID VARCHAR2(255), MSH_ROLE VARCHAR2(255), NOTIFIED TIMESTAMP, TIMESTAMP TIMESTAMP, CONSTRAINT PK_TB_ERROR_LOG PRIMARY KEY (ID_PK));

CREATE TABLE tb_join_payload_profile (FK_PAYLOAD NUMBER(10) NOT NULL, FK_PROFILE NUMBER(10) NOT NULL);

CREATE TABLE tb_join_process_init_party (PROCESS_FK NUMBER(10) NOT NULL, PARTY_FK NUMBER(10) NOT NULL);

CREATE TABLE tb_join_process_leg (PROCESS_FK NUMBER(10) NOT NULL, LEG_FK NUMBER(10) NOT NULL);

CREATE TABLE tb_join_process_resp_party (PROCESS_FK NUMBER(10) NOT NULL, PARTY_FK NUMBER(10) NOT NULL);

CREATE TABLE tb_join_property_set (PROPERTY_FK NUMBER(10) NOT NULL, SET_FK NUMBER(10) NOT NULL);

CREATE TABLE tb_leg (ID_PK NUMBER(10) NOT NULL, COMPRESS_PAYLOADS NUMBER(1), NAME VARCHAR2(255), FK_ACTION NUMBER(10), FK_MPC NUMBER(10), FK_ERROR_HANDLING NUMBER(10), FK_PAYLOAD_PROFILE NUMBER(10), FK_PROPERTY_SET NUMBER(10), FK_RECEPTION_AWARENESS NUMBER(10), FK_RELIABILITY NUMBER(10), FK_SECURITY NUMBER(10), FK_SERVICE NUMBER(10), FK_BUSINESSPROCESS NUMBER(10), CONSTRAINT PK_TB_LEG PRIMARY KEY (ID_PK));

CREATE TABLE tb_leg_tb_mpc (LegConfiguration_ID_PK NUMBER(10) NOT NULL, partyMpcMap_ID_PK NUMBER(10) NOT NULL, partyMpcMap_KEY NUMBER(10) NOT NULL);

CREATE TABLE tb_mep (ID_PK NUMBER(10) NOT NULL, LEG_COUNT NUMBER(10), NAME VARCHAR2(255), VALUE VARCHAR2(255), FK_BUSINESSPROCESS NUMBER(10), CONSTRAINT PK_TB_MEP PRIMARY KEY (ID_PK));

CREATE TABLE tb_mep_binding (ID_PK NUMBER(10) NOT NULL, NAME VARCHAR2(255), VALUE VARCHAR2(255), FK_BUSINESSPROCESS NUMBER(10), CONSTRAINT PK_TB_MEP_BINDING PRIMARY KEY (ID_PK));

CREATE TABLE tb_message_info (ID_PK NUMBER(10) NOT NULL, MESSAGE_ID VARCHAR2(255) NOT NULL, REF_TO_MESSAGE_ID VARCHAR2(255), TIMESTAMP TIMESTAMP, CONSTRAINT PK_TB_MESSAGE_INFO PRIMARY KEY (ID_PK));

CREATE TABLE tb_message_log (ID_PK NUMBER(10) NOT NULL, DELETED TIMESTAMP, MESSAGE_ID VARCHAR2(255), MESSAGE_STATUS VARCHAR2(255), MESSAGE_TYPE VARCHAR2(255), MPC VARCHAR2(255), MSH_ROLE VARCHAR2(255), NEXT_ATTEMPT TIMESTAMP, NOTIFICATION_STATUS VARCHAR2(255), RECEIVED TIMESTAMP, SEND_ATTEMPTS NUMBER(10), SEND_ATTEMPTS_MAX NUMBER(10), CONSTRAINT PK_TB_MESSAGE_LOG PRIMARY KEY (ID_PK));

CREATE TABLE tb_message_property (ID_PK NUMBER(10) NOT NULL, DATATYPE VARCHAR2(255), KEY_ VARCHAR2(255), NAME VARCHAR2(255), REQUIRED_ NUMBER(1), FK_BUSINESSPROCESS NUMBER(10), CONSTRAINT PK_TB_MESSAGE_PROPERTY PRIMARY KEY (ID_PK));

CREATE TABLE tb_message_property_set (ID_PK NUMBER(10) NOT NULL, NAME VARCHAR2(255), FK_BUSINESSPROCESS NUMBER(10), CONSTRAINT PK_TB_MESSAGE_PROPERTY_SET PRIMARY KEY (ID_PK));

CREATE TABLE tb_messaging (ID_PK NUMBER(10) NOT NULL, ID VARCHAR2(255), SIGNAL_MESSAGE_ID NUMBER(10), USER_MESSAGE_ID NUMBER(10), CONSTRAINT PK_TB_MESSAGING PRIMARY KEY (ID_PK));

CREATE TABLE tb_mpc (ID_PK NUMBER(10) NOT NULL, DEFAULT_MPC NUMBER(1), IS_ENABLED NUMBER(1), name VARCHAR2(255), QUALIFIED_NAME VARCHAR2(255), RETENTION_DOWNLOADED NUMBER(10), RETENTION_UNDOWNLOADED NUMBER(10), FK_CONFIGURATION NUMBER(10), CONSTRAINT PK_TB_MPC PRIMARY KEY (ID_PK));

CREATE TABLE tb_part_info (ID_PK NUMBER(10) NOT NULL, BINARY_DATA BLOB, DESCRIPTION_LANG VARCHAR2(255), DESCRIPTION_VALUE VARCHAR2(255), HREF VARCHAR2(255), IN_BODY NUMBER(1), SCHEMA_LOCATION VARCHAR2(255), SCHEMA_NAMESPACE VARCHAR2(255), SCHEMA_VERSION VARCHAR2(255), PAYLOADINFO_ID NUMBER(10), CONSTRAINT PK_TB_PART_INFO PRIMARY KEY (ID_PK));

CREATE TABLE tb_party (ID_PK NUMBER(10) NOT NULL, ENDPOINT VARCHAR2(255), NAME VARCHAR2(255), "PASSWORD" VARCHAR2(255), USERNAME VARCHAR2(255), FK_BUSINESSPROCESS NUMBER(10), CONSTRAINT PK_TB_PARTY PRIMARY KEY (ID_PK));

CREATE TABLE tb_party_id (ID_PK NUMBER(10) NOT NULL, TYPE VARCHAR2(255), VALUE VARCHAR2(255), TO_ID NUMBER(10), FROM_ID NUMBER(10), CONSTRAINT PK_TB_PARTY_ID PRIMARY KEY (ID_PK));

CREATE TABLE tb_party_id_type (ID_PK NUMBER(10) NOT NULL, NAME VARCHAR2(255), VALUE VARCHAR2(255), FK_BUSINESSPROCESS NUMBER(10), CONSTRAINT PK_TB_PARTY_ID_TYPE PRIMARY KEY (ID_PK));

CREATE TABLE tb_party_identifier (ID_PK NUMBER(10) NOT NULL, PARTY_ID VARCHAR2(255), FK_PARTY_ID_TYPE NUMBER(10), FK_PARTY NUMBER(10), CONSTRAINT PK_TB_PARTY_IDENTIFIER PRIMARY KEY (ID_PK));

CREATE TABLE tb_payload (ID_PK NUMBER(10) NOT NULL, CID VARCHAR2(255), IN_BODY NUMBER(1), MAX_SIZE NUMBER(10), MIME_TYPE VARCHAR2(255), NAME VARCHAR2(255), REQUIRED_ NUMBER(1), SCHEMA_FILE VARCHAR2(255), FK_BUSINESSPROCESS NUMBER(10), CONSTRAINT PK_TB_PAYLOAD PRIMARY KEY (ID_PK));

CREATE TABLE tb_payload_profile (ID_PK NUMBER(10) NOT NULL, MAX_SIZE NUMBER(10), NAME VARCHAR2(255), FK_BUSINESSPROCESS NUMBER(10), CONSTRAINT PK_TB_PAYLOAD_PROFILE PRIMARY KEY (ID_PK));

CREATE TABLE tb_process (ID_PK NUMBER(10) NOT NULL, NAME VARCHAR2(255), FK_AGREEMENT NUMBER(10), FK_INITIATOR_ROLE NUMBER(10), FK_MEP NUMBER(10), FK_MEP_BINDING NUMBER(10), FK_RESPONDER_ROLE NUMBER(10), FK_BUSINESSPROCESS NUMBER(10), CONSTRAINT PK_TB_PROCESS PRIMARY KEY (ID_PK));

CREATE TABLE tb_property (ID_PK NUMBER(10) NOT NULL, NAME VARCHAR2(255) NOT NULL, VALUE VARCHAR2(255), MESSAGEPROPERTIES_ID NUMBER(10), PARTPROPERTIES_ID NUMBER(10), CONSTRAINT PK_TB_PROPERTY PRIMARY KEY (ID_PK));

CREATE TABLE tb_receipt (ID_PK NUMBER(10) NOT NULL, CONSTRAINT PK_TB_RECEIPT PRIMARY KEY (ID_PK));

CREATE TABLE tb_receipt_data (RECEIPT_ID NUMBER(10) NOT NULL, RAW_XML CLOB);

CREATE TABLE tb_reception_awareness (ID_PK NUMBER(10) NOT NULL, DUPLICATE_DETECTION NUMBER(1), NAME VARCHAR2(255), RETRY_COUNT NUMBER(10), RETRY_TIMEOUT NUMBER(10), STRATEGY VARCHAR2(255), FK_BUSINESSPROCESS NUMBER(10), CONSTRAINT PK_TB_RECEPTION_AWARENESS PRIMARY KEY (ID_PK));

CREATE TABLE tb_reliability (ID_PK NUMBER(10) NOT NULL, NAME VARCHAR2(255), NON_REPUDIATION NUMBER(1), REPLY_PATTERN VARCHAR2(255), FK_BUSINESSPROCESS NUMBER(10), CONSTRAINT PK_TB_RELIABILITY PRIMARY KEY (ID_PK));

CREATE TABLE tb_role (ID_PK NUMBER(10) NOT NULL, NAME VARCHAR2(255), VALUE VARCHAR2(255), FK_BUSINESSPROCESS NUMBER(10), CONSTRAINT PK_TB_ROLE PRIMARY KEY (ID_PK));

CREATE TABLE tb_routing_criteria (ID_PK NUMBER(10) NOT NULL, EXPRESSION VARCHAR2(255), NAME VARCHAR2(255), FK_BACKEND_FILTER NUMBER(10), PRIORITY NUMBER(10), CONSTRAINT PK_TB_ROUTING_CRITERIA PRIMARY KEY (ID_PK));

CREATE TABLE tb_security (ID_PK NUMBER(10) NOT NULL, NAME VARCHAR2(255), POLICY VARCHAR2(255), SIGNATURE_METHOD VARCHAR2(255), FK_BUSINESSPROCESS NUMBER(10), CONSTRAINT PK_TB_SECURITY PRIMARY KEY (ID_PK));

CREATE TABLE tb_service (ID_PK NUMBER(10) NOT NULL, NAME VARCHAR2(255), SERVICE_TYPE VARCHAR2(255), VALUE VARCHAR2(255), FK_BUSINESSPROCESS NUMBER(10), CONSTRAINT PK_TB_SERVICE PRIMARY KEY (ID_PK));

CREATE TABLE tb_signal_message (ID_PK NUMBER(10) NOT NULL, PULL_REQUEST_MPC VARCHAR2(255), messageInfo_ID_PK NUMBER(10), receipt_ID_PK NUMBER(10), CONSTRAINT PK_TB_SIGNAL_MESSAGE PRIMARY KEY (ID_PK));

CREATE TABLE tb_user_message (ID_PK NUMBER(10) NOT NULL, COLLABORATION_INFO_ACTION VARCHAR2(255), AGREEMENT_REF_PMODE VARCHAR2(255), AGREEMENT_REF_TYPE VARCHAR2(255), AGREEMENT_REF_VALUE VARCHAR2(255), COLL_INFO_CONVERS_ID VARCHAR2(255) NOT NULL, SERVICE_TYPE VARCHAR2(255), SERVICE_VALUE VARCHAR2(255), MPC VARCHAR2(255), FROM_ROLE VARCHAR2(255), TO_ROLE VARCHAR2(255), messageInfo_ID_PK NUMBER(10), CONSTRAINT PK_TB_USER_MESSAGE PRIMARY KEY (ID_PK));

ALTER TABLE qrtz_blob_triggers ADD CONSTRAINT PRIMARY_01 PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);

ALTER TABLE qrtz_calendars ADD CONSTRAINT PRIMARY_02 PRIMARY KEY (SCHED_NAME, CALENDAR_NAME);

ALTER TABLE qrtz_cron_triggers ADD CONSTRAINT PRIMARY_03 PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);

ALTER TABLE qrtz_fired_triggers ADD CONSTRAINT PRIMARY_04 PRIMARY KEY (SCHED_NAME, ENTRY_ID);

ALTER TABLE qrtz_job_details ADD CONSTRAINT PRIMARY_05 PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP);

ALTER TABLE qrtz_locks ADD CONSTRAINT PRIMARY_06 PRIMARY KEY (SCHED_NAME, LOCK_NAME);

ALTER TABLE qrtz_paused_trigger_grps ADD CONSTRAINT PRIMARY_07 PRIMARY KEY (SCHED_NAME, TRIGGER_GROUP);

ALTER TABLE qrtz_scheduler_state ADD CONSTRAINT PRIMARY_08 PRIMARY KEY (SCHED_NAME, INSTANCE_NAME, LAST_CHECKIN_TIME, CHECKIN_INTERVAL);

ALTER TABLE qrtz_simple_triggers ADD CONSTRAINT PRIMARY_09 PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);

ALTER TABLE qrtz_simprop_triggers ADD CONSTRAINT PRIMARY_10 PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);

ALTER TABLE qrtz_triggers ADD CONSTRAINT PRIMARY_11 PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);

ALTER TABLE tb_leg_tb_mpc ADD CONSTRAINT PRIMARY_12 PRIMARY KEY (LegConfiguration_ID_PK, partyMpcMap_KEY);

ALTER TABLE tb_leg_tb_mpc ADD CONSTRAINT UK_7h5nw411791gf4lg1yh6si1wd UNIQUE (partyMpcMap_ID_PK);

ALTER TABLE tb_message_info ADD CONSTRAINT UK_ecrraoe83o1uqq0dn9424x7ln UNIQUE (MESSAGE_ID);

CREATE INDEX FK_13k2eptp1ejp5othy1njrg3p8 ON tb_leg(FK_RELIABILITY);

CREATE INDEX FK_277j83lgsq3nmwyhsw637ynd8 ON tb_mep_binding(FK_BUSINESSPROCESS);

CREATE INDEX FK_2h31p0a2y6weg2grh70c9cyva ON tb_property(MESSAGEPROPERTIES_ID);

CREATE INDEX FK_2sw6knalor7rpr04ye0o2r9ap ON tb_role(FK_BUSINESSPROCESS);

CREATE INDEX FK_3emt6xaglh7676lpckiy6r1vb ON tb_process(FK_RESPONDER_ROLE);

CREATE INDEX FK_3sgaxwfr5koe3oldbbd62siwq ON tb_reception_awareness(FK_BUSINESSPROCESS);

CREATE INDEX FK_433rgm5a446t59q6jkb885l3x ON tb_messaging(USER_MESSAGE_ID);

CREATE INDEX FK_53gp9smtemqcuio8o9t10narm ON tb_messaging(SIGNAL_MESSAGE_ID);

CREATE INDEX FK_62dt1y6o7od1t0iqn7dow8uq5 ON tb_configuration(FK_BUSINESSPROCESSES);

CREATE INDEX FK_6hldnnqkmh6lui555t4n9fjiv ON tb_action(FK_BUSINESSPROCESS);

CREATE INDEX FK_70m9kqpqab3rgk90wsuv9vn64 ON tb_agreement(FK_BUSINESSPROCESS);

CREATE INDEX FK_7p1g7sqvli1sj6k7vphjo9irc ON tb_process(FK_BUSINESSPROCESS);

CREATE INDEX FK_8nccae214mvs1kh5sgcj3y5oy ON tb_receipt_data(RECEIPT_ID);

CREATE INDEX FK_8q319stwm1noijpb3jfl8jri8 ON tb_mpc(FK_CONFIGURATION);

CREATE INDEX FK_9hpmoewlitivcl8rw1joccuau ON tb_leg(FK_SECURITY);

CREATE INDEX FK_advgudix024irqpuge4dl9iqf ON tb_party_id_type(FK_BUSINESSPROCESS);

CREATE INDEX FK_am7bwsm92se3nvclbeiep5vwg ON tb_leg(FK_PAYLOAD_PROFILE);

CREATE INDEX FK_asjdl57budmuwmj3611969jpp ON tb_join_process_init_party(PARTY_FK);

CREATE INDEX FK_be4xc1069qyw2klhvm3xmg26s ON tb_user_message(messageInfo_ID_PK);

CREATE INDEX FK_c08ljjwi4p9dx1rjtcdbe1sfu ON tb_signal_message(messageInfo_ID_PK);

CREATE INDEX FK_cloyy9k391vhsup85iwr8ixiv ON tb_party(FK_BUSINESSPROCESS);

CREATE INDEX FK_cwylvg6ernwba61mo3yagmkg8 ON tb_property(PARTPROPERTIES_ID);

CREATE INDEX FK_d9el0l8u1gm5oeu67nqrkherq ON tb_payload(FK_BUSINESSPROCESS);

CREATE INDEX FK_desr6xtdp1lp41d5venlhf4bc ON tb_party_identifier(FK_PARTY);

CREATE INDEX FK_dgtgtg41yrfebyysmkyi1gcaa ON tb_leg(FK_ACTION);

CREATE INDEX FK_e7ehghifrns83w6v3tps7vpii ON tb_party_identifier(FK_PARTY_ID_TYPE);

CREATE INDEX FK_eiy3f9ahx0kdx4wxoi6prytn9 ON tb_message_property_set(FK_BUSINESSPROCESS);

CREATE INDEX FK_ekk8pn89y50g22kd3gpja7j39 ON tb_join_property_set(PROPERTY_FK);

CREATE INDEX FK_epmxkh8u7jnw5pbyj47sirmlh ON tb_mep(FK_BUSINESSPROCESS);

CREATE INDEX FK_fkftd5orw2etiu4ghmn3mhx9i ON tb_party_id(FROM_ID);

CREATE INDEX FK_fpxbdc63gifacrd4qb6afrqyw ON tb_join_process_leg(PROCESS_FK);

CREATE INDEX FK_fwrq81cctu2nh0qeprmpvker9 ON tb_security(FK_BUSINESSPROCESS);

CREATE INDEX FK_g20eow5f2cke3avsrg6qpkuxv ON tb_join_payload_profile(FK_PROFILE);

CREATE INDEX FK_gh98q07kcjl7wdaon9ggifqux ON tb_join_process_leg(LEG_FK);

CREATE INDEX FK_hlb2y2prpm52ssynn22h3swsr ON tb_join_process_resp_party(PROCESS_FK);

CREATE INDEX FK_i7gei6bdr2cdn61hdoplxbu7p ON tb_routing_criteria(FK_BACKEND_FILTER);

CREATE INDEX FK_j0y3fcediqx5but8jbscy59kw ON tb_leg(FK_ERROR_HANDLING);

CREATE INDEX FK_j7lab5n5suklcldqhxn8jl2jo ON tb_process(FK_MEP);

CREATE INDEX FK_kjanlcdeap7nirdigr7rt7p4v ON tb_process(FK_AGREEMENT);

CREATE INDEX FK_kkhxs36rw15aygpn00nvvgyxg ON tb_service(FK_BUSINESSPROCESS);

CREATE INDEX FK_kv2c5k41apdlvc6i18ar57abe ON tb_leg(FK_SERVICE);

CREATE INDEX FK_kyn19swm143m96in317sr97h2 ON tb_join_process_resp_party(PARTY_FK);

CREATE INDEX FK_l1mmhs1tbt8pw7vx5teuytjvj ON tb_error_handling(FK_BUSINESSPROCESS);

CREATE INDEX FK_le4rxnki51eak6xetc7fq8sri ON tb_leg(FK_PROPERTY_SET);

CREATE INDEX FK_lnnplhiawxx7wlt43ye3pej00 ON tb_reliability(FK_BUSINESSPROCESS);

CREATE INDEX FK_lynom7rxkc0t1xfl0nowxpeuj ON tb_payload_profile(FK_BUSINESSPROCESS);

CREATE INDEX FK_m6uu2y6g9buet3o3k1n4qxwec ON tb_signal_message(receipt_ID_PK);

CREATE INDEX FK_mk54xe43f3hokh7fjl3w66efk ON tb_join_property_set(SET_FK);

CREATE INDEX FK_orddltv2g3lq79eu48c2mc2fy ON tb_leg_tb_mpc(partyMpcMap_KEY);

CREATE INDEX FK_ot8jfkotd6qu7jrdaptrohbn8 ON tb_join_process_init_party(PROCESS_FK);

CREATE INDEX FK_ppvxyurcvfy71fvejw9bosghw ON tb_leg(FK_MPC);

CREATE INDEX FK_prrcmi4grm4txhfsfnfhgq1k1 ON tb_error(SIGNALMESSAGE_ID);

CREATE INDEX FK_q0l3ej6ruqcfutru2securq9l ON tb_join_payload_profile(FK_PAYLOAD);

CREATE INDEX FK_q1jfsxfoj3nl7hii3co7bu0fr ON tb_process(FK_INITIATOR_ROLE);

CREATE INDEX FK_q3gapfc1e7hfbgmo0nebj4k1n ON tb_configuration(FK_PARTY);

CREATE INDEX FK_r2swpqof7636vveqt0dxl89dp ON tb_leg(FK_BUSINESSPROCESS);

CREATE INDEX FK_rj8h1b65vnjjgyfcjnvswkguh ON tb_message_property(FK_BUSINESSPROCESS);

CREATE INDEX FK_rlbjstvo4gpdcp6ysv9mofqxj ON tb_leg(FK_RECEPTION_AWARENESS);

CREATE INDEX FK_tp11d8fg7cv1fwf5xkvwqpp34 ON tb_process(FK_MEP_BINDING);

CREATE INDEX FK_tq6lbn3mp0vsfc6qqu7wxy54g ON tb_part_info(PAYLOADINFO_ID);

CREATE INDEX FK_ylub8lptgvsch02mj71euuil ON tb_party_id(TO_ID);

CREATE INDEX IDX_QRTZ_FT_INST_JOB_REQ_RCVRY ON qrtz_fired_triggers(SCHED_NAME, INSTANCE_NAME, REQUESTS_RECOVERY);

CREATE INDEX IDX_QRTZ_FT_JG ON qrtz_fired_triggers(SCHED_NAME, JOB_GROUP);

CREATE INDEX IDX_QRTZ_FT_J_G ON qrtz_fired_triggers(SCHED_NAME, JOB_NAME, JOB_GROUP);

CREATE INDEX IDX_QRTZ_FT_TG ON qrtz_fired_triggers(SCHED_NAME, TRIGGER_GROUP);

CREATE INDEX IDX_QRTZ_FT_TRIG_INST_NAME ON qrtz_fired_triggers(SCHED_NAME, INSTANCE_NAME);

CREATE INDEX IDX_QRTZ_FT_T_G ON qrtz_fired_triggers(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);

CREATE INDEX IDX_QRTZ_J_GRP ON qrtz_job_details(SCHED_NAME, JOB_GROUP);

CREATE INDEX IDX_QRTZ_J_REQ_RECOVERY ON qrtz_job_details(SCHED_NAME, REQUESTS_RECOVERY);

CREATE INDEX IDX_QRTZ_T_C ON qrtz_triggers(SCHED_NAME, CALENDAR_NAME);

CREATE INDEX IDX_QRTZ_T_G ON qrtz_triggers(SCHED_NAME, TRIGGER_GROUP);

CREATE INDEX IDX_QRTZ_T_J ON qrtz_triggers(SCHED_NAME, JOB_NAME, JOB_GROUP);

CREATE INDEX IDX_QRTZ_T_JG ON qrtz_triggers(SCHED_NAME, JOB_GROUP);

CREATE INDEX IDX_QRTZ_T_NEXT_FIRE_TIME ON qrtz_triggers(SCHED_NAME, NEXT_FIRE_TIME);

CREATE INDEX IDX_QRTZ_T_NFT_MISFIRE ON qrtz_triggers(SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME);

CREATE INDEX IDX_QRTZ_T_NFT_ST ON qrtz_triggers(SCHED_NAME, TRIGGER_STATE, NEXT_FIRE_TIME);

CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE ON qrtz_triggers(SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_STATE);

CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE_GRP ON qrtz_triggers(SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_GROUP, TRIGGER_STATE);

CREATE INDEX IDX_QRTZ_T_N_G_STATE ON qrtz_triggers(SCHED_NAME, TRIGGER_GROUP, TRIGGER_STATE);

CREATE INDEX IDX_QRTZ_T_N_STATE ON qrtz_triggers(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, TRIGGER_STATE);

CREATE INDEX IDX_QRTZ_T_STATE ON qrtz_triggers(SCHED_NAME, TRIGGER_STATE);

CREATE INDEX UK_bpeflt902ybu9nshba2fjpv9d ON tb_message_log(MSH_ROLE);

ALTER TABLE tb_leg ADD CONSTRAINT FK_13k2eptp1ejp5othy1njrg3p8 FOREIGN KEY (FK_RELIABILITY) REFERENCES tb_reliability (ID_PK);

ALTER TABLE tb_mep_binding ADD CONSTRAINT FK_277j83lgsq3nmwyhsw637ynd8 FOREIGN KEY (FK_BUSINESSPROCESS) REFERENCES tb_business_process (ID_PK);

ALTER TABLE tb_property ADD CONSTRAINT FK_2h31p0a2y6weg2grh70c9cyva FOREIGN KEY (MESSAGEPROPERTIES_ID) REFERENCES tb_user_message (ID_PK);

ALTER TABLE tb_role ADD CONSTRAINT FK_2sw6knalor7rpr04ye0o2r9ap FOREIGN KEY (FK_BUSINESSPROCESS) REFERENCES tb_business_process (ID_PK);

ALTER TABLE tb_process ADD CONSTRAINT FK_3emt6xaglh7676lpckiy6r1vb FOREIGN KEY (FK_RESPONDER_ROLE) REFERENCES tb_role (ID_PK);

ALTER TABLE tb_reception_awareness ADD CONSTRAINT FK_3sgaxwfr5koe3oldbbd62siwq FOREIGN KEY (FK_BUSINESSPROCESS) REFERENCES tb_business_process (ID_PK);

ALTER TABLE tb_messaging ADD CONSTRAINT FK_433rgm5a446t59q6jkb885l3x FOREIGN KEY (USER_MESSAGE_ID) REFERENCES tb_user_message (ID_PK);

ALTER TABLE tb_messaging ADD CONSTRAINT FK_53gp9smtemqcuio8o9t10narm FOREIGN KEY (SIGNAL_MESSAGE_ID) REFERENCES tb_signal_message (ID_PK);

ALTER TABLE tb_configuration ADD CONSTRAINT FK_62dt1y6o7od1t0iqn7dow8uq5 FOREIGN KEY (FK_BUSINESSPROCESSES) REFERENCES tb_business_process (ID_PK);

ALTER TABLE tb_action ADD CONSTRAINT FK_6hldnnqkmh6lui555t4n9fjiv FOREIGN KEY (FK_BUSINESSPROCESS) REFERENCES tb_business_process (ID_PK);

ALTER TABLE tb_agreement ADD CONSTRAINT FK_70m9kqpqab3rgk90wsuv9vn64 FOREIGN KEY (FK_BUSINESSPROCESS) REFERENCES tb_business_process (ID_PK);

ALTER TABLE tb_leg_tb_mpc ADD CONSTRAINT FK_7h5nw411791gf4lg1yh6si1wd FOREIGN KEY (partyMpcMap_ID_PK) REFERENCES tb_mpc (ID_PK);

ALTER TABLE tb_process ADD CONSTRAINT FK_7p1g7sqvli1sj6k7vphjo9irc FOREIGN KEY (FK_BUSINESSPROCESS) REFERENCES tb_business_process (ID_PK);

ALTER TABLE tb_receipt_data ADD CONSTRAINT FK_8nccae214mvs1kh5sgcj3y5oy FOREIGN KEY (RECEIPT_ID) REFERENCES tb_receipt (ID_PK);

ALTER TABLE tb_mpc ADD CONSTRAINT FK_8q319stwm1noijpb3jfl8jri8 FOREIGN KEY (FK_CONFIGURATION) REFERENCES tb_configuration (ID_PK);

ALTER TABLE tb_leg ADD CONSTRAINT FK_9hpmoewlitivcl8rw1joccuau FOREIGN KEY (FK_SECURITY) REFERENCES tb_security (ID_PK);

ALTER TABLE qrtz_blob_triggers ADD CONSTRAINT FK_BLOB_TRIGGERS FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES qrtz_triggers (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);

ALTER TABLE qrtz_cron_triggers ADD CONSTRAINT FK_CRON_TRIGGERS FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES qrtz_triggers (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);

ALTER TABLE qrtz_simple_triggers ADD CONSTRAINT FK_SIMPLE_TRIGGERS FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES qrtz_triggers (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);

ALTER TABLE qrtz_simprop_triggers ADD CONSTRAINT FK_SIMPROP_TRIGGERS FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES qrtz_triggers (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);

ALTER TABLE qrtz_triggers ADD CONSTRAINT FK_TRIGGERS FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP) REFERENCES qrtz_job_details (SCHED_NAME, JOB_NAME, JOB_GROUP);

ALTER TABLE tb_party_id_type ADD CONSTRAINT FK_advgudix024irqpuge4dl9iqf FOREIGN KEY (FK_BUSINESSPROCESS) REFERENCES tb_business_process (ID_PK);

ALTER TABLE tb_leg ADD CONSTRAINT FK_am7bwsm92se3nvclbeiep5vwg FOREIGN KEY (FK_PAYLOAD_PROFILE) REFERENCES tb_payload_profile (ID_PK);

ALTER TABLE tb_join_process_init_party ADD CONSTRAINT FK_asjdl57budmuwmj3611969jpp FOREIGN KEY (PARTY_FK) REFERENCES tb_party (ID_PK);

ALTER TABLE tb_user_message ADD CONSTRAINT FK_be4xc1069qyw2klhvm3xmg26s FOREIGN KEY (messageInfo_ID_PK) REFERENCES tb_message_info (ID_PK);

ALTER TABLE tb_signal_message ADD CONSTRAINT FK_c08ljjwi4p9dx1rjtcdbe1sfu FOREIGN KEY (messageInfo_ID_PK) REFERENCES tb_message_info (ID_PK);

ALTER TABLE tb_party ADD CONSTRAINT FK_cloyy9k391vhsup85iwr8ixiv FOREIGN KEY (FK_BUSINESSPROCESS) REFERENCES tb_business_process (ID_PK);

ALTER TABLE tb_property ADD CONSTRAINT FK_cwylvg6ernwba61mo3yagmkg8 FOREIGN KEY (PARTPROPERTIES_ID) REFERENCES tb_part_info (ID_PK);

ALTER TABLE tb_payload ADD CONSTRAINT FK_d9el0l8u1gm5oeu67nqrkherq FOREIGN KEY (FK_BUSINESSPROCESS) REFERENCES tb_business_process (ID_PK);

ALTER TABLE tb_party_identifier ADD CONSTRAINT FK_desr6xtdp1lp41d5venlhf4bc FOREIGN KEY (FK_PARTY) REFERENCES tb_party (ID_PK);

ALTER TABLE tb_leg ADD CONSTRAINT FK_dgtgtg41yrfebyysmkyi1gcaa FOREIGN KEY (FK_ACTION) REFERENCES tb_action (ID_PK);

ALTER TABLE tb_party_identifier ADD CONSTRAINT FK_e7ehghifrns83w6v3tps7vpii FOREIGN KEY (FK_PARTY_ID_TYPE) REFERENCES tb_party_id_type (ID_PK);

ALTER TABLE tb_message_property_set ADD CONSTRAINT FK_eiy3f9ahx0kdx4wxoi6prytn9 FOREIGN KEY (FK_BUSINESSPROCESS) REFERENCES tb_business_process (ID_PK);

ALTER TABLE tb_join_property_set ADD CONSTRAINT FK_ekk8pn89y50g22kd3gpja7j39 FOREIGN KEY (PROPERTY_FK) REFERENCES tb_message_property_set (ID_PK);

ALTER TABLE tb_mep ADD CONSTRAINT FK_epmxkh8u7jnw5pbyj47sirmlh FOREIGN KEY (FK_BUSINESSPROCESS) REFERENCES tb_business_process (ID_PK);

ALTER TABLE tb_party_id ADD CONSTRAINT FK_fkftd5orw2etiu4ghmn3mhx9i FOREIGN KEY (FROM_ID) REFERENCES tb_user_message (ID_PK);

ALTER TABLE tb_join_process_leg ADD CONSTRAINT FK_fpxbdc63gifacrd4qb6afrqyw FOREIGN KEY (PROCESS_FK) REFERENCES tb_process (ID_PK);

ALTER TABLE tb_security ADD CONSTRAINT FK_fwrq81cctu2nh0qeprmpvker9 FOREIGN KEY (FK_BUSINESSPROCESS) REFERENCES tb_business_process (ID_PK);

ALTER TABLE tb_join_payload_profile ADD CONSTRAINT FK_g20eow5f2cke3avsrg6qpkuxv FOREIGN KEY (FK_PROFILE) REFERENCES tb_payload (ID_PK);

ALTER TABLE tb_join_process_leg ADD CONSTRAINT FK_gh98q07kcjl7wdaon9ggifqux FOREIGN KEY (LEG_FK) REFERENCES tb_leg (ID_PK);

ALTER TABLE tb_join_process_resp_party ADD CONSTRAINT FK_hlb2y2prpm52ssynn22h3swsr FOREIGN KEY (PROCESS_FK) REFERENCES tb_process (ID_PK);

ALTER TABLE tb_routing_criteria ADD CONSTRAINT FK_i7gei6bdr2cdn61hdoplxbu7p FOREIGN KEY (FK_BACKEND_FILTER) REFERENCES tb_backend_filter (ID_PK);

ALTER TABLE tb_leg ADD CONSTRAINT FK_j0y3fcediqx5but8jbscy59kw FOREIGN KEY (FK_ERROR_HANDLING) REFERENCES tb_error_handling (ID_PK);

ALTER TABLE tb_process ADD CONSTRAINT FK_j7lab5n5suklcldqhxn8jl2jo FOREIGN KEY (FK_MEP) REFERENCES tb_mep (ID_PK);

ALTER TABLE tb_process ADD CONSTRAINT FK_kjanlcdeap7nirdigr7rt7p4v FOREIGN KEY (FK_AGREEMENT) REFERENCES tb_agreement (ID_PK);

ALTER TABLE tb_service ADD CONSTRAINT FK_kkhxs36rw15aygpn00nvvgyxg FOREIGN KEY (FK_BUSINESSPROCESS) REFERENCES tb_business_process (ID_PK);

ALTER TABLE tb_leg ADD CONSTRAINT FK_kv2c5k41apdlvc6i18ar57abe FOREIGN KEY (FK_SERVICE) REFERENCES tb_service (ID_PK);

ALTER TABLE tb_join_process_resp_party ADD CONSTRAINT FK_kyn19swm143m96in317sr97h2 FOREIGN KEY (PARTY_FK) REFERENCES tb_party (ID_PK);

ALTER TABLE tb_error_handling ADD CONSTRAINT FK_l1mmhs1tbt8pw7vx5teuytjvj FOREIGN KEY (FK_BUSINESSPROCESS) REFERENCES tb_business_process (ID_PK);

ALTER TABLE tb_leg ADD CONSTRAINT FK_le4rxnki51eak6xetc7fq8sri FOREIGN KEY (FK_PROPERTY_SET) REFERENCES tb_message_property_set (ID_PK);

ALTER TABLE tb_reliability ADD CONSTRAINT FK_lnnplhiawxx7wlt43ye3pej00 FOREIGN KEY (FK_BUSINESSPROCESS) REFERENCES tb_business_process (ID_PK);

ALTER TABLE tb_payload_profile ADD CONSTRAINT FK_lynom7rxkc0t1xfl0nowxpeuj FOREIGN KEY (FK_BUSINESSPROCESS) REFERENCES tb_business_process (ID_PK);

ALTER TABLE tb_signal_message ADD CONSTRAINT FK_m6uu2y6g9buet3o3k1n4qxwec FOREIGN KEY (receipt_ID_PK) REFERENCES tb_receipt (ID_PK);

ALTER TABLE tb_join_property_set ADD CONSTRAINT FK_mk54xe43f3hokh7fjl3w66efk FOREIGN KEY (SET_FK) REFERENCES tb_message_property (ID_PK);

ALTER TABLE tb_leg_tb_mpc ADD CONSTRAINT FK_nylcxsy1f9cn3tdskh97h8pt1 FOREIGN KEY (LegConfiguration_ID_PK) REFERENCES tb_leg (ID_PK);

ALTER TABLE tb_leg_tb_mpc ADD CONSTRAINT FK_orddltv2g3lq79eu48c2mc2fy FOREIGN KEY (partyMpcMap_KEY) REFERENCES tb_party (ID_PK);

ALTER TABLE tb_join_process_init_party ADD CONSTRAINT FK_ot8jfkotd6qu7jrdaptrohbn8 FOREIGN KEY (PROCESS_FK) REFERENCES tb_process (ID_PK);

ALTER TABLE tb_leg ADD CONSTRAINT FK_ppvxyurcvfy71fvejw9bosghw FOREIGN KEY (FK_MPC) REFERENCES tb_mpc (ID_PK);

ALTER TABLE tb_error ADD CONSTRAINT FK_prrcmi4grm4txhfsfnfhgq1k1 FOREIGN KEY (SIGNALMESSAGE_ID) REFERENCES tb_signal_message (ID_PK);

ALTER TABLE tb_join_payload_profile ADD CONSTRAINT FK_q0l3ej6ruqcfutru2securq9l FOREIGN KEY (FK_PAYLOAD) REFERENCES tb_payload_profile (ID_PK);

ALTER TABLE tb_process ADD CONSTRAINT FK_q1jfsxfoj3nl7hii3co7bu0fr FOREIGN KEY (FK_INITIATOR_ROLE) REFERENCES tb_role (ID_PK);

ALTER TABLE tb_configuration ADD CONSTRAINT FK_q3gapfc1e7hfbgmo0nebj4k1n FOREIGN KEY (FK_PARTY) REFERENCES tb_party (ID_PK);

ALTER TABLE tb_leg ADD CONSTRAINT FK_r2swpqof7636vveqt0dxl89dp FOREIGN KEY (FK_BUSINESSPROCESS) REFERENCES tb_business_process (ID_PK);

ALTER TABLE tb_message_property ADD CONSTRAINT FK_rj8h1b65vnjjgyfcjnvswkguh FOREIGN KEY (FK_BUSINESSPROCESS) REFERENCES tb_business_process (ID_PK);

ALTER TABLE tb_leg ADD CONSTRAINT FK_rlbjstvo4gpdcp6ysv9mofqxj FOREIGN KEY (FK_RECEPTION_AWARENESS) REFERENCES tb_reception_awareness (ID_PK);

ALTER TABLE tb_process ADD CONSTRAINT FK_tp11d8fg7cv1fwf5xkvwqpp34 FOREIGN KEY (FK_MEP_BINDING) REFERENCES tb_mep_binding (ID_PK);

ALTER TABLE tb_part_info ADD CONSTRAINT FK_tq6lbn3mp0vsfc6qqu7wxy54g FOREIGN KEY (PAYLOADINFO_ID) REFERENCES tb_user_message (ID_PK);

ALTER TABLE tb_party_id ADD CONSTRAINT FK_ylub8lptgvsch02mj71euuil FOREIGN KEY (TO_ID) REFERENCES tb_user_message (ID_PK);

-- Changeset src/main/resources/db/changelog-3.1-delta.xml::changelog-3.1::Christian Koch
ALTER TABLE tb_message_log ADD BACKEND VARCHAR2(255);

ALTER TABLE tb_message_log ADD ENDPOINT VARCHAR2(255);

ALTER TABLE tb_part_info ADD FILENAME VARCHAR2(255);

ALTER TABLE tb_part_info ADD MIME VARCHAR2(255) NOT NULL;

ALTER TABLE tb_property ADD TYPE VARCHAR2(255);

ALTER TABLE tb_process ADD USE_DYNAMIC_INITIATOR NUMBER(1) NOT NULL;

ALTER TABLE tb_process ADD USE_DYNAMIC_RESPONDER NUMBER(1) NOT NULL;

-- Changeset src/main/resources/db/changelog-3.1-delta.xml::changelog-3.1-FINAL::Stefan Mueller
ALTER TABLE tb_error_log RENAME COLUMN TIMESTAMP TO TIME_STAMP;

ALTER TABLE tb_message_info RENAME COLUMN TIMESTAMP TO TIME_STAMP;

-- Changeset src/main/resources/db/changelog-3.2-rc1-delta.xml::changelog-3.2-delta-1::martifp
CREATE SEQUENCE HIBERNATE_SEQUENCE START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 9999999999999999999999999999 CACHE 20 NOORDER;

-- Changeset src/main/resources/db/changelog-3.2-rc1-delta.xml::changelog-3.2-delta-tb_message_log-index::Cosmin Baciu
CREATE INDEX IDX_TB_ML_MSG_ID ON tb_message_log(MESSAGE_ID);

-- Changeset src/main/resources/db/changelog-3.2.0-delta.xml::changelog-ws-plugin-auth::idragusa
CREATE TABLE TB_AUTHENTICATION_ENTRY (ID_PK NUMBER(10) NOT NULL, CERTIFICATE_ID VARCHAR2(255), USERNAME VARCHAR2(255), PASSWD VARCHAR2(255), AUTH_ROLES VARCHAR2(255) NOT NULL, ORIGINAL_USER VARCHAR2(255), BACKEND VARCHAR2(255), CONSTRAINT PK_TB_AUTHENTICATION_ENTRY PRIMARY KEY (ID_PK));

-- Changeset src/main/resources/db/changelog-3.2.0-delta.xml::insert_ws_default_auth::idragusa
INSERT INTO TB_AUTHENTICATION_ENTRY (ID_PK, USERNAME, PASSWD, AUTH_ROLES) VALUES (HIBERNATE_SEQUENCE.nextval, 'admin', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'ROLE_ADMIN');

INSERT INTO TB_AUTHENTICATION_ENTRY (ID_PK, USERNAME, PASSWD, AUTH_ROLES, ORIGINAL_USER) VALUES (HIBERNATE_SEQUENCE.nextval, 'user', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'ROLE_USER', 'urn:oasis:names:tc:ebcore:partyid-type:unregistered:C1');

INSERT INTO TB_AUTHENTICATION_ENTRY (ID_PK, CERTIFICATE_ID, AUTH_ROLES) VALUES (HIBERNATE_SEQUENCE.nextval, 'CN=blue_gw,O=eDelivery,C=BE:10370035830817850458', 'ROLE_ADMIN');

-- Changeset src/main/resources/db/changelog-3.2.5-delta.xml::EDELIVERY-2117::Cosmin Baciu
ALTER TABLE TB_BACKEND_FILTER ADD CRITERIA_OPERATOR VARCHAR2(255) DEFAULT 'AND' NOT NULL;

-- Changeset src/main/resources/db/changelog-3.2.5-delta.xml::changelog-nonrepudiation::idragusa
CREATE TABLE TB_RAWENVELOPE_LOG (ID_PK NUMBER(10) NOT NULL, USERMESSAGE_ID_FK NUMBER(10), SIGNALMESSAGE_ID_FK NUMBER(10), RAW_XML CLOB, CONSTRAINT PK_TB_RAWENVELOPE_LOG PRIMARY KEY (ID_PK));

ALTER TABLE TB_RAWENVELOPE_LOG ADD CONSTRAINT FK_usermsg_id_fk_rawenv_id FOREIGN KEY (USERMESSAGE_ID_FK) REFERENCES TB_USER_MESSAGE (ID_PK);

ALTER TABLE TB_RAWENVELOPE_LOG ADD CONSTRAINT FK_signalmsg_id_fk_rawenv_id FOREIGN KEY (SIGNALMESSAGE_ID_FK) REFERENCES TB_SIGNAL_MESSAGE (ID_PK);

-- Changeset src/main/resources/db/changelog-3.3.0-delta-model.xml::Add_Downloaded_1542::ArunRaj
ALTER TABLE TB_MESSAGE_LOG ADD DOWNLOADED TIMESTAMP;

-- Changeset src/main/resources/db/changelog-3.3.0-delta-model.xml::EDelivery_1940::TiagoMiguel
CREATE TABLE TB_MESSAGE_ACKNW (ID_PK NUMBER(10) NOT NULL, MESSAGE_ID VARCHAR2(255), FROM_VALUE VARCHAR2(255), TO_VALUE VARCHAR2(255), CREATE_DATE TIMESTAMP, CREATE_USER VARCHAR2(255), ACKNOWLEDGE_DATE TIMESTAMP, CONSTRAINT PK_TB_MESSAGE_ACKNW PRIMARY KEY (ID_PK));

CREATE TABLE TB_MESSAGE_ACKNW_PROP (ID_PK NUMBER(10) NOT NULL, PROPERTY_NAME VARCHAR2(255), PROPERTY_VALUE VARCHAR2(255), FK_MSG_ACKNOWLEDGE NUMBER(10), CONSTRAINT PK_TB_MESSAGE_ACKNW_PROP PRIMARY KEY (ID_PK));

ALTER TABLE TB_MESSAGE_ACKNW_PROP ADD CONSTRAINT FK_MSG_ACK_PROP_MSG_ACK FOREIGN KEY (FK_MSG_ACKNOWLEDGE) REFERENCES TB_MESSAGE_ACKNW (ID_PK);

-- Changeset src/main/resources/db/changelog-3.3.0-delta-model.xml::EDELIVERY_1941::CosminBaciu
ALTER TABLE TB_MESSAGE_LOG ADD FAILED TIMESTAMP;

ALTER TABLE TB_MESSAGE_LOG ADD RESTORED TIMESTAMP;

CREATE TABLE TB_SEND_ATTEMPT (ID_PK NUMBER(10) NOT NULL, MESSAGE_ID VARCHAR2(255) NOT NULL, START_DATE TIMESTAMP NOT NULL, END_DATE TIMESTAMP NOT NULL, STATUS VARCHAR2(255) NOT NULL, ERROR VARCHAR2(255), CONSTRAINT PK_TB_SEND_ATTEMPT PRIMARY KEY (ID_PK));

-- Changeset src/main/resources/db/changelog-3.3.0-delta-model.xml::Add raw configuration table::Mircea
CREATE TABLE TB_CONFIGURATION_RAW (ID_PK NUMBER(10) NOT NULL, CONFIGURATION_DATE TIMESTAMP, XML BLOB, CONSTRAINT PK_TB_CONFIGURATION_RAW PRIMARY KEY (ID_PK));

-- Changeset src/main/resources/db/changelog-3.3.0-delta-model.xml::EDELIVERY_2144::dussath
CREATE TABLE TB_USER (ID_PK NUMBER(38, 0) NOT NULL, USER_EMAIL VARCHAR2(255), USER_ENABLED NUMBER(1) NOT NULL, USER_PASSWORD VARCHAR2(255) NOT NULL, USER_NAME VARCHAR2(255) NOT NULL, OPTLOCK NUMBER(10), CONSTRAINT PK_TB_USER PRIMARY KEY (ID_PK));

CREATE TABLE TB_USER_ROLE (ID_PK NUMBER(38, 0) NOT NULL, ROLE_NAME VARCHAR2(255) NOT NULL, CONSTRAINT PK_TB_USER_ROLE PRIMARY KEY (ID_PK));

CREATE TABLE TB_USER_ROLES (USER_ID NUMBER(38, 0) NOT NULL, ROLE_ID NUMBER(38, 0) NOT NULL);

ALTER TABLE TB_USER_ROLES ADD CONSTRAINT PK_PRIMARY PRIMARY KEY (USER_ID, ROLE_ID);

ALTER TABLE TB_USER_ROLE ADD CONSTRAINT UQ_ROLE_NAME UNIQUE (ROLE_NAME);

ALTER TABLE TB_USER ADD CONSTRAINT USER_NAME UNIQUE (USER_NAME);

CREATE INDEX IDX_ROLE_ID ON TB_USER_ROLES(ROLE_ID);

CREATE INDEX IDX_USER_ID ON TB_USER_ROLES(USER_ID);

ALTER TABLE TB_USER_ROLES ADD CONSTRAINT FK_USER_ROLES_USER FOREIGN KEY (ROLE_ID) REFERENCES TB_USER_ROLE (ID_PK);

ALTER TABLE TB_USER_ROLES ADD CONSTRAINT FK_USER_ROLES_ROLE FOREIGN KEY (USER_ID) REFERENCES TB_USER (ID_PK);

-- Changeset src/main/resources/db/changelog-3.3.0-delta-model.xml::changelog-ws-plugin-auth-drop-trigger::musatmi 
-- Drop the trigger_auth trigger.
BEGIN
            FOR i IN (SELECT null FROM user_triggers WHERE trigger_name = 'TRIGGER_AUTH') LOOP
            EXECUTE IMMEDIATE 'DROP TRIGGER TRIGGER_AUTH';
            END LOOP;
            END;
/

-- Changeset src/main/resources/db/changelog-3.3.0-delta-model.xml::EDELIVERY_2580::dussath
CREATE INDEX IDX_PROPERTY_NAME ON TB_PROPERTY(NAME);

CREATE INDEX IDX_PARTY_ID_VALUE ON TB_PARTY_ID(VALUE);

CREATE INDEX IDX_MESSAGE_LOG_RECEIVED ON TB_MESSAGE_LOG(RECEIVED);

CREATE INDEX IDX_MESSAGE_LOG_M_STATUS ON TB_MESSAGE_LOG(MESSAGE_STATUS);

CREATE INDEX IDX_MESSAGE_INFO_R_T_M_ID ON TB_MESSAGE_INFO(REF_TO_MESSAGE_ID);

-- Changeset src/main/resources/db/changelog-3.3.0.xml::EDELIVERY-2144::thomas dussart
INSERT INTO TB_USER_ROLE (ID_PK, ROLE_NAME) VALUES ('1', 'ROLE_ADMIN');

INSERT INTO TB_USER_ROLE (ID_PK, ROLE_NAME) VALUES ('2', 'ROLE_USER');

INSERT INTO TB_USER (ID_PK, USER_NAME, USER_PASSWORD, USER_ENABLED) VALUES ('1', 'admin', '$2a$10$5uKS72xK2ArGDgb2CwjYnOzQcOmB7CPxK6fz2MGcDBM9vJ4rUql36', 1);

INSERT INTO TB_USER (ID_PK, USER_NAME, USER_PASSWORD, USER_ENABLED) VALUES ('2', 'user', '$2a$10$HApapHvDStTEwjjneMCvxuqUKVyycXZRfXMwjU0rRmaWMsjWQp/Zu', 1);

INSERT INTO TB_USER_ROLES (USER_ID, ROLE_ID) VALUES ('1', '1');

INSERT INTO TB_USER_ROLES (USER_ID, ROLE_ID) VALUES ('1', '2');

INSERT INTO TB_USER_ROLES (USER_ID, ROLE_ID) VALUES ('2', '2');

-- Changeset src/main/resources/db/changelog-3.3.0.xml::EDELIVERY-2726::migueti
ALTER TABLE TB_BACKEND_FILTER DROP COLUMN CRITERIA_OPERATOR;

-- Changeset src/main/resources/db/changelog-3.3.1-delta-model.xml::EDELIVERY-2859::dussath
ALTER TABLE TB_USER ADD ATTEMPT_COUNT NUMBER(10) DEFAULT 0;

ALTER TABLE TB_USER ADD SUSPENSION_DATE TIMESTAMP;

ALTER SEQUENCE HIBERNATE_SEQUENCE CACHE 1000;

CREATE TABLE TB_MESSAGING_LOCK (ID_PK INT AUTO_INCREMENT NOT NULL, MESSAGE_TYPE VARCHAR(10) NULL, MESSAGE_RECEIVED datetime NULL, MESSAGE_STATE VARCHAR(10) NULL, MESSAGE_ID VARCHAR(255) NULL, INITIATOR VARCHAR(255) NULL, MPC VARCHAR(255) NULL, CONSTRAINT PK_TB_MESSAGING_LOCK PRIMARY KEY (ID_PK));

CREATE INDEX IDX_MESSAGE_LOCK_SEARCH ON TB_MESSAGING_LOCK(MESSAGE_TYPE, MESSAGE_STATE, INITIATOR, MPC);

CREATE UNIQUE INDEX IDX_MESSAGE_LOCK_UNIQUE_ID ON TB_MESSAGING_LOCK(MESSAGE_ID);