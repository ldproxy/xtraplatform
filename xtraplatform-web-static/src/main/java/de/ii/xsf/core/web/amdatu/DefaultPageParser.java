/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.web.amdatu;

import java.util.Scanner;

/**
 * Parses the {@link Constants#WEB_RESOURCE_DEFAULT_PAGE} bundle header into a list of names.
 * <p>
 * Allowed syntax for this header is (in pseudo-BNF):
 * </p>
 * <pre>
 * header ::= entries
 * 
 * entries ::= entry (',' entries)?
 * 
 * entry ::= default | path '=' default
 * 
 * default ::= URLCHAR+
 * 
 * path ::= '/'? URLCHAR*
 * 
 * URLCHAR ::= all characters accepted in a URL (see RFC 1738).
 * </pre>
 */
public class DefaultPageParser {

    /**
     * Parses a given key into a {@link DefaultPages} instance.
     * 
     * @param key the key to parse, can be <code>null</code> or empty.
     * @return the {@link DefaultPages} instance with the parsed information, never <code>null</code>.
     * @throws InvalidEntryException in case the given key contained an invalid entry.
     */
    public static DefaultPages parseDefaultPages(String key) throws InvalidEntryException {
        DefaultPages defaultPages = new DefaultPages();

        if ((key != null) && !"".equals(key.trim())) {
            Scanner scanner = new Scanner(key);
            scanner.useDelimiter(",\\s*");

            try {
                while (scanner.hasNext()) {
                    String entry = scanner.next().trim();

                    if ("".equals(entry)) {
                        throw new InvalidEntryException(key, entry);
                    }

                    parseDefaultPageEntry(defaultPages, entry);
                }
            } finally {
                scanner.close();
            }

        }

        return defaultPages;
    }

    private static void parseDefaultPageEntry(DefaultPages defaultPages, String entry) throws InvalidEntryException {
        try {
            int idx = entry.indexOf('=');
            if (idx >= 0) {
                String path = entry.substring(0, idx).trim();
                String page = entry.substring(idx + 1).trim();

                defaultPages.addDefault(path, page);
            } else {
                defaultPages.addGlobalDefault(entry);
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidEntryException("Invalid entry!", entry);
        }
    }
}