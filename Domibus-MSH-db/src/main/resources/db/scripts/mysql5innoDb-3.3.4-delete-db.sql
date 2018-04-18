-- *********************************************************************
-- Delete script for MySQL Domibus DB with a time interval
-- Change START_DATE and END_DATE values accordingly - please pay attention
-- that the data stored in DB is timezone agnostic.
--
-- Important: In order to keep the JMS queues synchronized with the DB data that will be
-- deleted by this script, the Domibus Administrator should remove manually the associated
-- JMS messages from the plugin notifications queues
-- *********************************************************************
SET @START_DATE=STR_TO_DATE('2017-01-20 10:00:00', '%Y-%m-%d %H:%i:%s');
SET @END_DATE=STR_TO_DATE('2017-12-20 15:00:00', '%Y-%m-%d %H:%i:%s');

SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;


DELETE FROM tb_messaging
WHERE
    (signal_message_id IN (SELECT
        id_pk
    FROM
        tb_signal_message

    WHERE
        messageinfo_id_pk IN (SELECT
            id_pk
        FROM
            tb_message_info

        WHERE
            message_id IN (SELECT
                message_id
            FROM
                tb_message_log

            WHERE
                received BETWEEN @START_DATE AND @END_DATE))));

DELETE FROM tb_messaging
WHERE
    (user_message_id IN (SELECT
        id_pk
    FROM
        tb_user_message

    WHERE
        messageinfo_id_pk IN (SELECT
            id_pk
        FROM
            tb_message_info

        WHERE
            message_id IN (SELECT
                message_id
            FROM
                tb_message_log

            WHERE
                received BETWEEN @START_DATE AND @END_DATE))));

DELETE FROM tb_error_log
WHERE
    (error_signal_message_id IN (SELECT
        message_id
    FROM
        tb_message_log

    WHERE
        received BETWEEN @START_DATE AND @END_DATE));

DELETE FROM tb_error_log
WHERE
    (message_in_error_id IN (SELECT
        message_id
    FROM
        tb_message_log

    WHERE
        received BETWEEN @START_DATE AND @END_DATE));

DELETE FROM tb_party_id
WHERE
    from_id IN (SELECT
        id_pk
    FROM
        tb_user_message

    WHERE
        messageinfo_id_pk IN (SELECT
            id_pk
        FROM
            tb_message_info

        WHERE
            message_id IN (SELECT
                message_id
            FROM
                tb_message_log

            WHERE
                received BETWEEN @START_DATE AND @END_DATE)));

DELETE FROM tb_party_id
WHERE
    to_id IN (SELECT
        id_pk
    FROM
        tb_user_message

    WHERE
        messageinfo_id_pk IN (SELECT
            id_pk
        FROM
            tb_message_info

        WHERE
            message_id IN (SELECT
                message_id
            FROM
                tb_message_log

            WHERE
                received BETWEEN @START_DATE AND @END_DATE)));

DELETE FROM tb_receipt_data
WHERE
    receipt_id IN (SELECT
        id_pk
    FROM
        tb_receipt

    WHERE
        id_pk IN (SELECT
            receipt_id_pk
        FROM
            tb_signal_message

        WHERE
            messageinfo_id_pk IN (SELECT
                id_pk
            FROM
                tb_message_info

            WHERE
                message_id IN (SELECT
                    message_id
                FROM
                    tb_message_log

                WHERE
                    received BETWEEN @START_DATE AND @END_DATE))));

DELETE FROM tb_property
WHERE
    partproperties_id IN (SELECT
        id_pk
    FROM
        tb_part_info

    WHERE
        payloadinfo_id IN (SELECT
            id_pk
        FROM
            tb_user_message

        WHERE
            messageinfo_id_pk IN (SELECT
                id_pk
            FROM
                tb_message_info

            WHERE
                message_id IN (SELECT
                    message_id
                FROM
                    tb_message_log

                WHERE
                    received BETWEEN @START_DATE AND @END_DATE))));

DELETE FROM tb_property
WHERE
    messageproperties_id IN (SELECT
        id_pk
    FROM
        tb_user_message

    WHERE
        messageinfo_id_pk IN (SELECT
            id_pk
        FROM
            tb_message_info

        WHERE
            message_id IN (SELECT
                message_id
            FROM
                tb_message_log

            WHERE
                received BETWEEN @START_DATE AND @END_DATE)));

DELETE FROM tb_part_info
WHERE
    payloadinfo_id IN (SELECT
        id_pk
    FROM
        tb_user_message

    WHERE
        messageinfo_id_pk IN (SELECT
            id_pk
        FROM
            tb_message_info

        WHERE
            message_id IN (SELECT
                message_id
            FROM
                tb_message_log

            WHERE
                received BETWEEN @START_DATE AND @END_DATE)));

DELETE FROM tb_rawenvelope_log
WHERE
    usermessage_id_fk IN (SELECT
        id_pk
    FROM
        tb_user_message

    WHERE
        messageinfo_id_pk IN (SELECT
            id_pk
        FROM
            tb_message_info

        WHERE
            message_id IN (SELECT
                message_id
            FROM
                tb_message_log

            WHERE
                received BETWEEN @START_DATE AND @END_DATE)));

DELETE FROM tb_rawenvelope_log
WHERE
    signalmessage_id_fk IN (SELECT
        id_pk
    FROM
        tb_signal_message

    WHERE
        messageinfo_id_pk IN (SELECT
            id_pk
        FROM
            tb_message_info

        WHERE
            message_id IN (SELECT
                message_id
            FROM
                tb_message_log

            WHERE
                received BETWEEN @START_DATE AND @END_DATE)));

DELETE FROM tb_error
WHERE
    signalmessage_id IN (SELECT
        id_pk
    FROM
        tb_signal_message

    WHERE
        messageinfo_id_pk IN (SELECT
            id_pk
        FROM
            tb_message_info

        WHERE
            message_id IN (SELECT
                message_id
            FROM
                tb_message_log

            WHERE
                received BETWEEN @START_DATE AND @END_DATE)));

DELETE FROM tb_user_message
WHERE
    messageinfo_id_pk IN (SELECT
        id_pk
    FROM
        tb_message_info

    WHERE
        message_id IN (SELECT
            message_id
        FROM
            tb_message_log

        WHERE
            received BETWEEN @START_DATE AND @END_DATE));

DELETE FROM tb_signal_message
WHERE
    messageinfo_id_pk IN (SELECT
        id_pk
    FROM
        tb_message_info

    WHERE
        message_id IN (SELECT
            message_id
        FROM
            tb_message_log

        WHERE
            received BETWEEN @START_DATE AND @END_DATE));

DELETE FROM tb_receipt
WHERE
    id_pk IN (SELECT
        receipt_id_pk
    FROM
        tb_signal_message

    WHERE
        messageinfo_id_pk IN (SELECT
            id_pk
        FROM
            tb_message_info

        WHERE
            message_id IN (SELECT
                message_id
            FROM
                tb_message_log

            WHERE
                received BETWEEN @START_DATE AND @END_DATE)));

DELETE FROM tb_message_info
WHERE
    message_id IN (SELECT
        message_id
    FROM
        tb_message_log

    WHERE
        received BETWEEN @START_DATE AND @END_DATE);

DELETE FROM tb_message_log
WHERE
    received BETWEEN @START_DATE AND @END_DATE;


SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

COMMIT;