/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraserver.framework.util;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * a helper for strings
 *
 * @author fischer
 */
public class StringUtils {

    /**
     * creates an md5 hash from the input string
     *
     * @param input
     * @return md5 from input
     */
    public static String createMD5Hash(String input) {
        if (input == null) {
            return null;
        }

        HashFunction hf = Hashing.md5();
        HashCode hc = hf.newHasher()
                .putString(input, Charsets.UTF_8)
                .hash();

        return hc.toString();
    }

    /**
     * creates a string from an InputStream
     *
     * @param is the inutstream
     * @return the string read from input
     */
    public static String getStringFromInputStream(InputStream is) {
        String text = "";
        try (final InputStreamReader inr = new InputStreamReader(is)) {
            text = CharStreams.toString(inr);
        } catch (IOException e) {
        }
        
        return text;
    }
}
