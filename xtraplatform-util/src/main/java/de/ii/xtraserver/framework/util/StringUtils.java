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
