package org.hyperledger.identus.walletsdk.castor

import kotlinx.coroutines.test.runTest
import org.hyperledger.identus.walletsdk.apollo.ApolloImpl
import org.hyperledger.identus.walletsdk.castor.did.DIDParser
import org.hyperledger.identus.walletsdk.domain.models.CastorError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DIDParserTest {

    @Test
    fun it_should_test_valid_DIDs() {
        val didExample1 = "did:aaaaaa:aa:aaa"
        val didExample2 = "did:prism01:b2.-_%11:b4._-%11"
        val didExample3 =
            "did:prism:b6c0c33d701ac1b9a262a14454d1bbde3d127d697a76950963c5fd930605:Cj8KPRI7CgdtYXN0ZXIwEAFKLgoJc2VmsxEiECSTjyV7sUfCr_ArpN9rvCwR9fRMAhcsr_S7ZRiJk4p5k"

        val parsedDID1 = DIDParser.parse(didExample1)
        val parsedDID2 = DIDParser.parse(didExample2)
        val parsedDID3 = DIDParser.parse(didExample3)

        assertEquals(parsedDID1.schema, "did")
        assertEquals(parsedDID1.method, "aaaaaa")
        assertEquals(parsedDID1.methodId, "aa:aaa")

        assertEquals(parsedDID2.schema, "did")
        assertEquals(parsedDID2.method, "prism01")
        assertEquals(parsedDID2.methodId, "b2.-_%11:b4._-%11")

        assertEquals(parsedDID3.schema, "did")
        assertEquals(parsedDID3.method, "prism")
        assertEquals(
            parsedDID3.methodId,
            "b6c0c33d701ac1b9a262a14454d1bbde3d127d697a76950963c5fd930605:Cj8KPRI7CgdtYXN0ZXIwEAFKLgoJc2VmsxEiECSTjyV7sUfCr_ArpN9rvCwR9fRMAhcsr_S7ZRiJk4p5k"
        )
    }

    @Test
    fun it_should_test_invalid_DIDs() {
        val didExample1 = "idi:aaaaaa:aa:aaa"
        val didExample2 = "did:-prism-:aaaaa:aaaa"
        val didExample3 = "did:prism:aaaaaaaaaaa::"
        val didExample4 = "did::prism:aaaaaaaaaaa:aaaa"
        val didExample5 = "did:prism::aaaaaaaaaaa:bbbb"

        assertFailsWith(
            exceptionClass = CastorError.InvalidDIDString::class,
            block = {
                DIDParser.parse(didExample1)
            }
        )

        assertFailsWith(
            exceptionClass = CastorError.InvalidDIDString::class,
            block = {
                DIDParser.parse(didExample2)
            }
        )

        assertFailsWith(
            exceptionClass = CastorError.InvalidDIDString::class,
            block = {
                DIDParser.parse(didExample3)
            }
        )

        assertFailsWith(
            exceptionClass = CastorError.InvalidDIDString::class,
            block = {
                DIDParser.parse(didExample4)
            }
        )

        assertFailsWith(
            exceptionClass = CastorError.InvalidDIDString::class,
            block = {
                DIDParser.parse(didExample5)
            }
        )
    }

    @Test
    fun test() {
        val did =
            "did:peer:2.Ez6LSmWXS6oahBFuVKi2HZ6bWe521uLdiDoPub9ExYM22ybj9.Vz6MkigNVs7sPs4i38uYqiBzbMJ5YKzfK3Re11e3mdL5YpqpV"
        val parsedDID = DIDParser.parse(did)
        assertEquals("peer", parsedDID.method)
        assertEquals(
            "2.Ez6LSmWXS6oahBFuVKi2HZ6bWe521uLdiDoPub9ExYM22ybj9.Vz6MkigNVs7sPs4i38uYqiBzbMJ5YKzfK3Re11e3mdL5YpqpV",
            parsedDID.methodId
        )
    }

    @Test
    fun testResolveLongPrismDID() = runTest {
        val did = "did:prism:cd71c0f451d5f7c9afa2fbba9a75c4a7548392df189c796f162fa3f85c62d1eb:CokCCoYCElwKB21hc3RlcjAQAUJPCglzZWNwMjU2azESIGNrl5zOnGcqrXEIooE0HLqLHJ6XHygMkeueXoYQWhkgGiDWBpNGD8LUKqOydRTUvXxD4SSnGA-9vNX4hYbk5lxIwRJACg9hdXRoZW50aWNhdGlvbjAQBEorCgdFZDI1NTE5EiB2bl_YZ1HkFoelHEHxtMSW8T_SAgLanjTgyzH2zAn3XhJkCg9hdXRoZW50aWNhdGlvbjEQBEJPCglzZWNwMjU2azESIGtJzR15uDjvzeJS4dq4hMWSh-6kn7lEPV3-EmUCkgSSGiDLCZhLdn-iyGecJs5YxexFTERVO1VKs8s8FSQZV8LNAQ"
        val apollo = ApolloImpl()
        val castor = CastorImpl(apollo)
        val resolved = castor.resolveDID(did)
//        print(resolved)
    }
}
