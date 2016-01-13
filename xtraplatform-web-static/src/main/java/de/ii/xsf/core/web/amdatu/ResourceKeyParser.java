/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ii.xsf.core.web.amdatu;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import org.osgi.framework.Constants;


/**
 * Parse the {@link Constants#WEB_RESOURCE_KEY} header of a bundle manifest.
 * <p>
 * Allowed syntax for this header is (in pseudo-BNF):
 * </p>
 * <pre>
 * header ::= entries
 * 
 * entries ::= entry (',' entries)?
 * 
 * entry ::= alias | alias ';' path | alias ';' path ';' contextId
 * 
 * alias ::= '/'? URLCHAR*
 * 
 * path ::= '/'? URLCHAR*
 * 
 * contextId ::= URLCHAR*
 * 
 * URLCHAR ::= all characters accepted in a URL (see RFC 1738).
 * </pre>
 * <p>
 * The <tt>alias</tt>, <tt>path</tt> and <tt>contextId</tt> have the following semantics:
 * </p>
 * <ol>
 * <li>The <tt>alias</tt> represents the web-alias used to access the resources. This
 * alias is mandatory and may start with a slash (if the leading slash is omitted, it
 * will be prepended automatically);</li>
 * <li>The <tt>path</tt> represents the internal path (relative to the registering
 * bundle!) to access the actual resources. This path is optional, and if omitted the
 * <tt>alias</tt> key will be used as internal path;</li>
 * <li>The <tt>contextId</tt> represents the specific context identifier to use in the
 * registration of the resources. See HttpService specification for more information
 * about the exact semantics of this key. If omitted, it will be defaulted to an empty
 * string.</li>
 * </ol>
 * <p>
 * Multiple entries can be defined, in which the <tt>alias</tt> <em>must</em> be unique
 * in the context of the HTTP service. There is currently no way to tell whether a
 * resource registration succeeded.
 * </p>
 */
public class ResourceKeyParser {
    /**
     * Parses the entries for the given {@link Constants#WEB_RESOURCE_KEY} value.
     * 
     * @param key the string value of the {@link Constants#WEB_RESOURCE_KEY}, can be <code>null</code> or empty.
     * @return a map of entries, the key denotes the context ID and the value is a list of register items.
     * @throws InvalidEntryException when a entry in the given argument is considered invalid.
     */
    public static Map<String, ResourceEntry> getEntries(String key) throws InvalidEntryException {
        Map<String, ResourceEntry> result = new HashMap<>();

        if ((key != null) && !"".equals(key.trim())) {
            Scanner scanner = new Scanner(key);
            scanner.useDelimiter(",\\s*");

            while (scanner.hasNext()) {
                Entry entry = new Entry(scanner.next());
                
                ResourceEntry rEntry = new ResourceEntry(entry.m_alias);
                rEntry.addPath(entry.m_path);
                result.put(entry.m_alias, rEntry);
            }

            scanner.close();
        }

        return result;
    }

    static class Entry {
        final String m_alias;
        final String m_path;

        Entry(String entry) throws InvalidEntryException {
            if (entry == null) {
                throw new InvalidEntryException("Cannot be null!", entry);
            }

            String[] elements = entry.split("\\s*;\\s*");
            if ((elements.length < 1) || (elements.length > 2)) {
                throw new InvalidEntryException("Not enough arguments, at least one at most two arguments expected!", entry);
            }
            String alias = elements[0].trim();
            if ("".equals(alias)) {
                throw new InvalidEntryException("Alias (1st element) cannot be empty!", entry);
            }
            String path = (elements.length > 1) ? elements[1].trim() : alias;
            if ("".equals(path)) {
                throw new InvalidEntryException("Path (2nd element) cannot be empty!", entry);
            }

            m_alias = createValidEntry(alias);
            m_path = createValidEntry(path);
        }

        private static String createValidEntry(String entry) {
            if (!entry.startsWith("/")) {
                return "/" + entry;
            }
            return entry;
        }
    }
}