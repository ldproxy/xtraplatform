/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.app

import spock.lang.Specification

import java.nio.charset.StandardCharsets

/**
 * Whatever {@code encrypt} produces, {@code decrypt} has to read back. The empty value is the
 * boundary: its encryption is the nonce and the tag and nothing else, which is the shortest valid
 * stored value — a length check that treated it as too short made an encrypted empty string
 * unreadable, and with it every feature that carried one.
 */
class EncryptionImplSpec extends Specification {

    // test-only 32-byte key
    static final String KEY = Base64.encoder.encodeToString(new byte[32])

    private EncryptionImpl encryption() {
        new EncryptionImpl(KEY)
    }

    private static String roundTrip(EncryptionImpl e, String plaintext) {
        new String(e.decrypt(e.encrypt(plaintext.getBytes(StandardCharsets.UTF_8))),
                StandardCharsets.UTF_8)
    }

    def 'the empty value survives the round trip'() {
        given:
        def e = encryption()

        expect:
        roundTrip(e, '') == ''
    }

    def 'the encryption of the empty value is exactly nonce plus tag'() {
        given:
        def e = encryption()

        when:
        def encrypted = e.encrypt(''.getBytes(StandardCharsets.UTF_8))

        then: 'the shortest valid stored value, so the length check must accept it'
        encrypted.length == EncryptionImpl.NONCE_LENGTH + (int) (EncryptionImpl.TAG_LENGTH_BITS / 8)
    }

    def 'a value shorter than nonce plus tag is still rejected'() {
        given:
        def e = encryption()
        def tooShort = new byte[EncryptionImpl.NONCE_LENGTH + (int) (EncryptionImpl.TAG_LENGTH_BITS / 8) - 1]

        when:
        e.decrypt(tooShort, ' for a spec')

        then:
        def ex = thrown(IllegalStateException)
        ex.message.contains('too short')
    }

    def 'ordinary values survive the round trip'() {
        given:
        def e = encryption()

        expect:
        roundTrip(e, value) == value

        where:
        value << ['x', 'Jean', ' ', 'a longer value with Ümläute and spaces', '1946-06-05']
    }

    def 'a corrupted value is rejected rather than silently decoded'() {
        given:
        def e = encryption()
        def encrypted = e.encrypt('Jean'.getBytes(StandardCharsets.UTF_8))
        encrypted[encrypted.length - 1] = (byte) (encrypted[encrypted.length - 1] ^ 0xFF)

        when:
        e.decrypt(encrypted, ' for a spec')

        then:
        thrown(IllegalStateException)
    }
}
