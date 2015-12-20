package de.ii.xsf.core.views;

/**
 *
 * @author zahnen
 */
public class DirectoryView extends XSFView {
    private final Object directory;

    public DirectoryView(Object directory, String template, String uri, String token) {
        super(template, uri, token);
        this.directory = directory;
    }

    public Object getDirectory() {
        return directory;
    }
}
