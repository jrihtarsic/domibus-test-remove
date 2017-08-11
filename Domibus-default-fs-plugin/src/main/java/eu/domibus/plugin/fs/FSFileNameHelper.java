package eu.domibus.plugin.fs;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import eu.domibus.common.MessageStatus;

/**
 * Helper to create and recognize derived file names
 * 
 * @author FERNANDES Henrique, GONCALVES Bruno
 */
public class FSFileNameHelper {
    
    private static final String NAME_SEPARATOR = "_";
    private static final String EXTENSION_SEPARATOR = ".";
    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private static final Pattern PROCESSED_FILE_PATTERN = Pattern.compile(
            NAME_SEPARATOR + UUID_PATTERN + "@.", Pattern.CASE_INSENSITIVE);
    private static final List<String> STATE_SUFFIXES;
    
    static {
        List<String> tempStateSuffixes = new LinkedList<>();
        for (MessageStatus status : MessageStatus.values()) {
            tempStateSuffixes.add(EXTENSION_SEPARATOR + status.name());
        }
        
        STATE_SUFFIXES = Collections.unmodifiableList(tempStateSuffixes);
    }

    /**
     * Checks if a given file name has been derived from a {@link eu.domibus.common.MessageStatus}.
     * In practice checks if the filename is suffixed by a dot and any of the
     * known {@link eu.domibus.common.MessageStatus}.
     * @param fileName the file name to test
     * @return true, if the file name has been derived from a {@link eu.domibus.common.MessageStatus}
     */
    public static boolean isAnyState(final String fileName) {
        return StringUtils.endsWithAny(fileName, STATE_SUFFIXES.toArray(new String[0]));
    }
    
    /**
     * Checks if a given file name has been derived from any message Id.
     * In practice checks if the filename contains an underscore followed by a
     * message Id.
     * @param fileName the file name to test
     * @return true, if the file name has been derived from a message Id
     */
    public static boolean isProcessed(final String fileName) {
        return PROCESSED_FILE_PATTERN.matcher(fileName).find();
    }
    
    /**
     * Checks if a given file name has been derived from a given message Id.
     * In practice checks if the filename contains an underscore followed by the
     * given message Id.
     * @param fileName the file name to test
     * @param messageId the message Id to test
     * @return true, if the file name has been derived from the given message Id
     */
    public static boolean isMessageRelated(String fileName, String messageId) {
        return fileName.contains(NAME_SEPARATOR + messageId);
    }
    
    /**
     * Derives a new file name from the given file name and a message Id.
     * In practice, for a given file name {@code filename.ext} and message Id
     * {@code messageId} generates a new file name of the form {@code filename_messageId.ext}.
     * @param fileName the file name to derive
     * @param messageId the message Id to use for the derivation
     * @return a new file name of the form {@code filename_messageId.ext}
     */
    public static String deriveFileName(final String fileName, final String messageId) {
        int extensionIdx = StringUtils.lastIndexOf(fileName, EXTENSION_SEPARATOR);
        
        if (extensionIdx != -1) {
            String fileNamePrefix = StringUtils.substring(fileName, 0, extensionIdx);
            String fileNameSuffix = StringUtils.substring(fileName, extensionIdx + 1);
            
            return fileNamePrefix + NAME_SEPARATOR + messageId + EXTENSION_SEPARATOR + fileNameSuffix;
        } else {
            return fileName + NAME_SEPARATOR + messageId;
        }
    }
    
    /**
     * Derives a new file name from the given file name and a {@link eu.domibus.common.MessageStatus}.
     * In practice, for a given file name {@code filename.ext} and message status
     * {@code MESSAGE_STATUS} generates a new file name of the form {@code filename.ext.MESSAGE_STATUS}.
     * @param fileName the file name to derive
     * @param status the message status to use for the derivation
     * @return a new file name of the form {@code filename.ext.MESSAGE_STATUS}
     */
    public static String deriveFileName(final String fileName, final MessageStatus status) {
        if (isAnyState(fileName)) {
            String strippedFileName = StringUtils.substringBeforeLast(fileName, EXTENSION_SEPARATOR);
            return strippedFileName + EXTENSION_SEPARATOR + status.name();
        } else {
            return fileName + EXTENSION_SEPARATOR + status.name();
        }
    }

}
