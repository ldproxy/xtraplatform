/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.api.exceptions;

import java.util.Locale;
import org.forgerock.i18n.LocalizableMessageDescriptor;

/**
 *
 * @author fischer
 */
public class MessageCompiler {
            
    public static String compileMessage(Object m, Object... args) {
        Locale locale = new Locale("en");
        if (args.length == 0) {
            return ((LocalizableMessageDescriptor.Arg0) m).get().toString(locale);
        } else if (args.length == 1) {
            return ((LocalizableMessageDescriptor.Arg1) m).get(args[0]).toString(locale);
        } else if (args.length == 2) {
            return ((LocalizableMessageDescriptor.Arg2) m).get(args[0], args[1]).toString(locale);
        } else if (args.length == 3) {
            return ((LocalizableMessageDescriptor.Arg3) m).get(args[0], args[1], args[2]).toString(locale);
        } else if (args.length == 4) {
            return ((LocalizableMessageDescriptor.Arg4) m).get(args[0], args[1], args[2], args[3]).toString(locale);
        } else if (args.length == 5) {
            return ((LocalizableMessageDescriptor.Arg5) m).get(args[0], args[1], args[2], args[3], args[4]).toString(locale);
        } else if (args.length == 6) {
            return ((LocalizableMessageDescriptor.Arg6) m).get(args[0], args[1], args[2], args[3], args[4], args[5]).toString(locale);
        } else if (args.length == 7) {
            return ((LocalizableMessageDescriptor.Arg7) m).get(args[0], args[1], args[2], args[3], args[4], args[5], args[6]).toString(locale);
        } else if (args.length == 8) {
            return ((LocalizableMessageDescriptor.Arg8) m).get(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]).toString(locale);
        } else if (args.length == 9) {
            return ((LocalizableMessageDescriptor.Arg9) m).get(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]).toString(locale);
        } else {
            return ((LocalizableMessageDescriptor.ArgN) m).get(args).toString(locale);
        }
    }
}
