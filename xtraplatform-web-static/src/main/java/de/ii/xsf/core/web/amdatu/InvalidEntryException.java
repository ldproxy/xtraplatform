package de.ii.xsf.core.web.amdatu;

/**
 * Thrown when {@link ResourceKeyParser} finds an invalid entry.
 */
public class InvalidEntryException extends Exception {
    private final String m_entry;

    public InvalidEntryException(String message, String entry) {
        super(message);
        m_entry = entry;
    }

    public String getEntry() {
        return m_entry;
    }
}