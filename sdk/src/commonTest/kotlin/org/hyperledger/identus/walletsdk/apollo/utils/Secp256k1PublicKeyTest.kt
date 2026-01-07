package org.hyperledger.identus.walletsdk.apollo.utils

import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class Secp256k1PublicKeyTest {

    @Test
    fun testInitializationFromXY() {
        val xBase64 = "usDW8CoGk6v0-I4v_2yFv76e-5-9k2aTxXzTTAAadwE"
        val yBase64 = "ExK4tY7ZgCilKyY1W-7vFvK5u-1-9k2aTxXzTTAAadw"

        // These are fake but valid-length values for testing the mechanics
        // 32 bytes each
        val x = ByteArray(32) { 1 }
        val y = ByteArray(32) { 2 }

        val key = Secp256k1PublicKey(x, y)

        // Expected internal raw bytes: 0x04 + x + y
        val expectedRaw = byteArrayOf(0x04) + x + y

        assertContentEquals(expectedRaw, key.raw)
        assertEquals(key.raw.size, 65) // 1 + 32 + 32
    }

    @Test
    fun testInitializationMatchesNative() {
        // Create dummy x and y
        val x = ByteArray(32) { 0xAA.toByte() }
        val y = ByteArray(32) { 0xBB.toByte() }

        // Create key via new constructor
        val keyFromXY = Secp256k1PublicKey(x, y)

        // Create key via native constructor (primary)
        val raw = byteArrayOf(0x04) + x + y
        val keyFromNative = Secp256k1PublicKey(raw)

        // assertions
        assertContentEquals(keyFromNative.raw, keyFromXY.raw)
    }
}
