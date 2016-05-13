INSERT INTO "TB_BUSINESS_PROCESS" VALUES (1);

INSERT INTO "TB_ACTION" VALUES (1,'noSecAction','NoSecurity',1);
INSERT INTO "TB_ACTION" VALUES (2,'tc2Action','TC2Leg1',1);
INSERT INTO "TB_ACTION" VALUES (3,'tc1Action','TC1Leg1',1);
INSERT INTO "TB_ACTION" VALUES (4,'tc3ActionLeg1','TC3Leg1',1);
INSERT INTO "TB_ACTION" VALUES (5,'tc3ActionLeg2','TC3Leg2',1);

INSERT INTO "TB_AGREEMENT" VALUES (1,'agreementEmpty','','',1);

INSERT INTO "TB_PARTY" VALUES (1,'http://fmstest.flame.co.za:8080/AS4','flame',NULL,NULL,1);
INSERT INTO "TB_PARTY" VALUES (2,'http://5.153.46.53:29001/AS4','ibmgw',NULL,NULL,1);
INSERT INTO "TB_PARTY" VALUES (3,'http://localhost:8090/domibus/services/msh','red_gw',NULL,NULL,1);
INSERT INTO "TB_PARTY" VALUES (4,'http://msh.holodeck-b2b.org:8080/msh','holodeck',NULL,NULL,1);
INSERT INTO "TB_PARTY" VALUES (5,'http://test.edelivery.it.nrw.de/domibus-msh','domibus_de',NULL,NULL,1);
INSERT INTO "TB_PARTY" VALUES (6,'http://localhost:8080/domibus/services/msh','blue_gw',NULL,NULL,1);
INSERT INTO "TB_PARTY" VALUES (7,'http://208.67.130.9/exchange/axwayas4','axway',NULL,NULL,1);
INSERT INTO "TB_PARTY" VALUES (8,'https://secure.gateway.eu/as4','cefgw',NULL,NULL,1);

INSERT INTO "TB_CONFIGURATION" VALUES (1,1,6);

INSERT INTO "TB_ERROR_HANDLING" VALUES (1,0x00,0x00,0x00,0x01,'demoErrorHandling',1);

INSERT INTO "TB_ROLE" VALUES (1,'defaultInitiatorRole','http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/initiator',1);
INSERT INTO "TB_ROLE" VALUES (2,'defaultResponderRole','http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder',1);

INSERT INTO "TB_MEP" VALUES (1,0,'twoway','http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/twoWay',1);
INSERT INTO "TB_MEP" VALUES (2,0,'oneway','http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/oneWay',1);

INSERT INTO "TB_MEP_BINDING" VALUES (1,'push','http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/push',1);
INSERT INTO "TB_MEP_BINDING" VALUES (2,'pushAndPush','http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/push-and-push',1);

INSERT INTO "TB_PROCESS" VALUES (1,0x00,0x00,'tc2Process',1,1,2,1,2,1);
INSERT INTO "TB_PROCESS" VALUES (2,0x00,0x00,'tc3Process',1,1,1,2,2,1);
INSERT INTO "TB_PROCESS" VALUES (3,0x00,0x00,'noSecProcess',1,1,2,1,2,1);
INSERT INTO "TB_PROCESS" VALUES (4,0x00,0x00,'tc1Process',1,1,2,1,2,1);

INSERT INTO "TB_JOIN_PROCESS_INIT_PARTY" VALUES (1,1);
INSERT INTO "TB_JOIN_PROCESS_INIT_PARTY" VALUES (2,1);
INSERT INTO "TB_JOIN_PROCESS_INIT_PARTY" VALUES (4,1);
INSERT INTO "TB_JOIN_PROCESS_INIT_PARTY" VALUES (1,2);
INSERT INTO "TB_JOIN_PROCESS_INIT_PARTY" VALUES (2,2);
INSERT INTO "TB_JOIN_PROCESS_INIT_PARTY" VALUES (4,2);
INSERT INTO "TB_JOIN_PROCESS_INIT_PARTY" VALUES (1,3);
INSERT INTO "TB_JOIN_PROCESS_INIT_PARTY" VALUES (2,3);
INSERT INTO "TB_JOIN_PROCESS_INIT_PARTY" VALUES (3,3);
INSERT INTO "TB_JOIN_PROCESS_INIT_PARTY" VALUES (4,3);
INSERT INTO "TB_JOIN_PROCESS_INIT_PARTY" VALUES (1,4);
INSERT INTO "TB_JOIN_PROCESS_INIT_PARTY" VALUES (2,4);
INSERT INTO "TB_JOIN_PROCESS_INIT_PARTY" VALUES (4,4);
INSERT INTO "TB_JOIN_PROCESS_INIT_PARTY" VALUES (1,5);
INSERT INTO "TB_JOIN_PROCESS_INIT_PARTY" VALUES (2,5);
INSERT INTO "TB_JOIN_PROCESS_INIT_PARTY" VALUES (4,5);
INSERT INTO "TB_JOIN_PROCESS_INIT_PARTY" VALUES (1,6);
INSERT INTO "TB_JOIN_PROCESS_INIT_PARTY" VALUES (2,6);
INSERT INTO "TB_JOIN_PROCESS_INIT_PARTY" VALUES (3,6);
INSERT INTO "TB_JOIN_PROCESS_INIT_PARTY" VALUES (4,6);
INSERT INTO "TB_JOIN_PROCESS_INIT_PARTY" VALUES (1,7);
INSERT INTO "TB_JOIN_PROCESS_INIT_PARTY" VALUES (2,7);
INSERT INTO "TB_JOIN_PROCESS_INIT_PARTY" VALUES (4,7);

INSERT INTO "TB_MPC" VALUES (1,0x01,0x01,'defaultMpc','http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC',0,60,1);

INSERT INTO "TB_MESSAGE_PROPERTY" VALUES (1,'string','originalSender','originalSenderProperty',0x01,1);
INSERT INTO "TB_MESSAGE_PROPERTY" VALUES (2,'string','finalRecipient','finalRecipientProperty',0x01,1);

INSERT INTO "TB_MESSAGE_PROPERTY_SET" VALUES (1,'ecodexPropertySet',1);

INSERT INTO "TB_RECEPTION_AWARENESS" VALUES (1,0x01,'receptionAwareness',4,5,'CONSTANT',1);

INSERT INTO "TB_RELIABILITY" VALUES (1,'noReliability',0x00,'RESPONSE',1);
INSERT INTO "TB_RELIABILITY" VALUES (2,'AS4Reliability',0x01,'RESPONSE',1);

INSERT INTO "TB_SECURITY" VALUES (1,'noSigNoEnc','doNothingPolicy.xml','RSA_SHA256',1);
INSERT INTO "TB_SECURITY" VALUES (2,'signAndEncrypt','signEncrypt.xml','RSA_SHA256',1);
INSERT INTO "TB_SECURITY" VALUES (3,'signOnly','signOnly.xml','RSA_SHA256',1);
INSERT INTO "TB_SECURITY" VALUES (4,'encryptAll','encryptAll.xml','RSA_SHA256',1);

INSERT INTO "TB_SERVICE" VALUES (1,'testService1','tc1','bdx:noprocess',1);
INSERT INTO "TB_SERVICE" VALUES (2,'testService2','tc2','bdx:noprocess',1);
INSERT INTO "TB_SERVICE" VALUES (3,'noSecService','','InternalTesting',1);
INSERT INTO "TB_SERVICE" VALUES (4,'testService3','tc3','bdx:noprocess',1);

INSERT INTO "TB_PAYLOAD" VALUES (5,'payload',0,0,'text/xml','businessContentPayload',1,NULL,1);
INSERT INTO "TB_PAYLOAD" VALUES (6,'attachment',0,0,'application/octet-stream','businessContentAttachment',0,NULL,1);

INSERT INTO "TB_PAYLOAD_PROFILE" VALUES (3,40894464,'MessageProfile',1);

INSERT INTO "TB_JOIN_PAYLOAD_PROFILE" VALUES (3,5),(3,6);

INSERT INTO "TB_LEG" VALUES (1,0x01,'pushTestcase3Leg2tc3ActionLeg2',5,1,1,3,1,1,2,1,4,1);
INSERT INTO "TB_LEG" VALUES (2,0x01,'pushTestcase3Leg1tc3ActionLeg1',4,1,1,3,1,1,2,1,4,1);
INSERT INTO "TB_LEG" VALUES (3,0x01,'pushTestcase2tc2Action',2,1,1,3,1,1,2,1,2,1);
INSERT INTO "TB_LEG" VALUES (4,0x01,'pushNoSecnoSecAction',1,1,1,3,1,1,2,1,3,1);
INSERT INTO "TB_LEG" VALUES (5,0x01,'pushTestcase1tc1Action',3,1,1,3,1,1,2,1,1,1);

INSERT INTO "TB_JOIN_PROCESS_LEG" VALUES (2,1);
INSERT INTO "TB_JOIN_PROCESS_LEG" VALUES (2,2);
INSERT INTO "TB_JOIN_PROCESS_LEG" VALUES (1,3);
INSERT INTO "TB_JOIN_PROCESS_LEG" VALUES (3,4);
INSERT INTO "TB_JOIN_PROCESS_LEG" VALUES (4,5);



INSERT INTO "TB_JOIN_PROCESS_RESP_PARTY" VALUES (1,1);
INSERT INTO "TB_JOIN_PROCESS_RESP_PARTY" VALUES (2,1);
INSERT INTO "TB_JOIN_PROCESS_RESP_PARTY" VALUES (4,1);
INSERT INTO "TB_JOIN_PROCESS_RESP_PARTY" VALUES (1,2);
INSERT INTO "TB_JOIN_PROCESS_RESP_PARTY" VALUES (2,2);
INSERT INTO "TB_JOIN_PROCESS_RESP_PARTY" VALUES (4,2);
INSERT INTO "TB_JOIN_PROCESS_RESP_PARTY" VALUES (1,3);
INSERT INTO "TB_JOIN_PROCESS_RESP_PARTY" VALUES (2,3);
INSERT INTO "TB_JOIN_PROCESS_RESP_PARTY" VALUES (3,3);
INSERT INTO "TB_JOIN_PROCESS_RESP_PARTY" VALUES (4,3);
INSERT INTO "TB_JOIN_PROCESS_RESP_PARTY" VALUES (1,4);
INSERT INTO "TB_JOIN_PROCESS_RESP_PARTY" VALUES (2,4);
INSERT INTO "TB_JOIN_PROCESS_RESP_PARTY" VALUES (4,4);
INSERT INTO "TB_JOIN_PROCESS_RESP_PARTY" VALUES (1,5);
INSERT INTO "TB_JOIN_PROCESS_RESP_PARTY" VALUES (2,5);
INSERT INTO "TB_JOIN_PROCESS_RESP_PARTY" VALUES (4,5);
INSERT INTO "TB_JOIN_PROCESS_RESP_PARTY" VALUES (1,6);
INSERT INTO "TB_JOIN_PROCESS_RESP_PARTY" VALUES (2,6);
INSERT INTO "TB_JOIN_PROCESS_RESP_PARTY" VALUES (3,6);
INSERT INTO "TB_JOIN_PROCESS_RESP_PARTY" VALUES (4,6);
INSERT INTO "TB_JOIN_PROCESS_RESP_PARTY" VALUES (1,7);
INSERT INTO "TB_JOIN_PROCESS_RESP_PARTY" VALUES (2,7);
INSERT INTO "TB_JOIN_PROCESS_RESP_PARTY" VALUES (4,7);

INSERT INTO "TB_JOIN_PROPERTY_SET" VALUES (1,1);
INSERT INTO "TB_JOIN_PROPERTY_SET" VALUES (1,2);

INSERT INTO "TB_PARTY_ID_TYPE" VALUES (1,'partyTypeEmpty','',1);

INSERT INTO "TB_PARTY_IDENTIFIER" VALUES (1,'urn:oasis:names:tc:ebcore:partyid-type:unregistered:flame',1,1);
INSERT INTO "TB_PARTY_IDENTIFIER" VALUES (2,'urn:oasis:names:tc:ebcore:partyid-type:unregistered:IBMgw',1,2);
INSERT INTO "TB_PARTY_IDENTIFIER" VALUES (3,'urn:oasis:names:tc:ebcore:partyid-type:unregistered:domibus-red',1,3);
INSERT INTO "TB_PARTY_IDENTIFIER" VALUES (4,'urn:oasis:names:tc:ebcore:partyid-type:unregistered:holodeck-b2b',1,4);
INSERT INTO "TB_PARTY_IDENTIFIER" VALUES (5,'urn:oasis:names:tc:ebcore:partyid-type:unregistered:domibus-de',1,5);
INSERT INTO "TB_PARTY_IDENTIFIER" VALUES (6,'urn:oasis:names:tc:ebcore:partyid-type:unregistered:domibus-blue',1,6);
INSERT INTO "TB_PARTY_IDENTIFIER" VALUES (7,'urn:oasis:names:tc:ebcore:partyid-type:unregistered:axway',1,7);
INSERT INTO "TB_PARTY_IDENTIFIER" VALUES (8,'urn:oasis:names:tc:ebcore:partyid-type:unregistered:CEF',1,8);

INSERT INTO "TB_MESSAGE_INFO" ("ID_PK","MESSAGE_ID","REF_TO_MESSAGE_ID","TIME_STAMP") VALUES (1,'2809cef6-240f-4792-bec1-7cb300a34679@domibus.eu',NULL,'2016-02-11 12:57:19');
INSERT INTO "TB_MESSAGE_INFO" ("ID_PK","MESSAGE_ID","REF_TO_MESSAGE_ID","TIME_STAMP") VALUES (2,'78a1d578-0cc7-41fb-9f35-86a5b2769a14@domibus.eu',NULL,'2016-02-11 16:29:44');
INSERT INTO "TB_MESSAGE_INFO" ("ID_PK","MESSAGE_ID","REF_TO_MESSAGE_ID","TIME_STAMP") VALUES (3,'2bbc05d8-b603-4742-a118-137898a81de3@domibus.eu',NULL,'2016-02-11 16:30:00');

INSERT INTO "TB_USER_MESSAGE" ("ID_PK","COLLABORATION_INFO_ACTION","AGREEMENT_REF_PMODE","AGREEMENT_REF_TYPE","AGREEMENT_REF_VALUE","COLL_INFO_CONVERS_ID","SERVICE_TYPE","SERVICE_VALUE","MPC","FROM_ROLE","TO_ROLE","MESSAGEINFO_ID_PK")
VALUES (1,'TC1Leg1',NULL,NULL,NULL,'7318c713-a1a7-4dc7-8497-337d40d95d39','tc1','bdx:noprocess','http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC','http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/initiator','http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder',1);

INSERT INTO "TB_USER_MESSAGE" ("ID_PK","COLLABORATION_INFO_ACTION","AGREEMENT_REF_PMODE","AGREEMENT_REF_TYPE","AGREEMENT_REF_VALUE","COLL_INFO_CONVERS_ID","SERVICE_TYPE","SERVICE_VALUE","MPC","FROM_ROLE","TO_ROLE","MESSAGEINFO_ID_PK")
VALUES (2,'TC1Leg1',NULL,NULL,NULL,'489c1e59-2f4b-4c15-b780-38fa81f1df0e','tc1','bdx:noprocess','http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC','http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/initiator','http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder',2);

INSERT INTO "TB_USER_MESSAGE" ("ID_PK","COLLABORATION_INFO_ACTION","AGREEMENT_REF_PMODE","AGREEMENT_REF_TYPE","AGREEMENT_REF_VALUE","COLL_INFO_CONVERS_ID","SERVICE_TYPE","SERVICE_VALUE","MPC","FROM_ROLE","TO_ROLE","MESSAGEINFO_ID_PK")
VALUES (3,'TC1Leg1',NULL,NULL,NULL,'9985e5cd-b898-4a7e-acd8-5fdf7a9edde7','tc1','bdx:noprocess','http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC','http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/initiator','http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder',3);

INSERT INTO "TB_MESSAGING" ("ID_PK","ID","SIGNAL_MESSAGE_ID","USER_MESSAGE_ID") VALUES (1,NULL,NULL,1);
INSERT INTO "TB_MESSAGING" ("ID_PK","ID","SIGNAL_MESSAGE_ID","USER_MESSAGE_ID") VALUES (2,NULL,NULL,2);
INSERT INTO "TB_MESSAGING" ("ID_PK","ID","SIGNAL_MESSAGE_ID","USER_MESSAGE_ID") VALUES (3,NULL,NULL,3);

INSERT INTO "TB_MESSAGE_LOG" ("ID_PK","BACKEND","DELETED","ENDPOINT","MESSAGE_ID","MESSAGE_STATUS","MESSAGE_TYPE","MPC","MSH_ROLE","NEXT_ATTEMPT","NOTIFICATION_STATUS","RECEIVED","SEND_ATTEMPTS","SEND_ATTEMPTS_MAX")
VALUES (1,NULL,NULL,NULL,'2809cef6-240f-4792-bec1-7cb300a34679@domibus.eu','RECEIVED','USER_MESSAGE','http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC','RECEIVING',NULL,NULL,'2016-02-11 12:57:24',0,0);

INSERT INTO "TB_MESSAGE_LOG" ("ID_PK","BACKEND","DELETED","ENDPOINT","MESSAGE_ID","MESSAGE_STATUS","MESSAGE_TYPE","MPC","MSH_ROLE","NEXT_ATTEMPT","NOTIFICATION_STATUS","RECEIVED","SEND_ATTEMPTS","SEND_ATTEMPTS_MAX")
VALUES (2,NULL,NULL,NULL,'78a1d578-0cc7-41fb-9f35-86a5b2769a14@domibus.eu','RECEIVED','USER_MESSAGE','http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC','RECEIVING',NULL,NULL,'2016-02-11 16:29:50',0,0);

INSERT INTO "TB_MESSAGE_LOG" ("ID_PK","BACKEND","DELETED","ENDPOINT","MESSAGE_ID","MESSAGE_STATUS","MESSAGE_TYPE","MPC","MSH_ROLE","NEXT_ATTEMPT","NOTIFICATION_STATUS","RECEIVED","SEND_ATTEMPTS","SEND_ATTEMPTS_MAX")
VALUES (3,NULL,NULL,NULL,'2bbc05d8-b603-4742-a118-137898a81de3@domibus.eu','RECEIVED','USER_MESSAGE','http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC','RECEIVING',NULL,NULL,'2016-02-11 16:30:00',0,0);

INSERT INTO "TB_PART_INFO" ("ID_PK","BINARY_DATA","DESCRIPTION_LANG","DESCRIPTION_VALUE","FILENAME","HREF","IN_BODY","MIME","SCHEMA_LOCATION","SCHEMA_NAMESPACE","SCHEMA_VERSION","PAYLOADINFO_ID")
VALUES (1, x'3c3f786d6c2076657273696f6e3d22312e302220656e636f64696e673d225554462d38223f3e3c68656c6c6f3e776f726c643c2f68656c6c6f3e',NULL,NULL,NULL,'sbdh-order','0','application/unknown',NULL,NULL,NULL,1);

INSERT INTO "TB_PART_INFO" ("ID_PK","BINARY_DATA","DESCRIPTION_LANG","DESCRIPTION_VALUE","FILENAME","HREF","IN_BODY","MIME","SCHEMA_LOCATION","SCHEMA_NAMESPACE","SCHEMA_VERSION","PAYLOADINFO_ID")
VALUES (2, x'3c3f786d6c2076657273696f6e3d22312e302220656e636f64696e673d225554462d38223f3e0d0a3c68656c6c6f3e776f726c643c2f68656c6c6f3e',NULL,NULL,NULL,'sbdh-order','0','application/unknown',NULL,NULL,NULL,2);

INSERT INTO "TB_PART_INFO" ("ID_PK","BINARY_DATA","DESCRIPTION_LANG","DESCRIPTION_VALUE","FILENAME","HREF","IN_BODY","MIME","SCHEMA_LOCATION","SCHEMA_NAMESPACE","SCHEMA_VERSION","PAYLOADINFO_ID")
VALUES (3, x'3c3f786d6c2076657273696f6e3d22312e302220656e636f64696e673d225554462d38223f3e3c68656c6c6f3e776f726c643c2f68656c6c6f3e',NULL,NULL,NULL,'sbdh-order','0','application/unknown',NULL,NULL,NULL,3);