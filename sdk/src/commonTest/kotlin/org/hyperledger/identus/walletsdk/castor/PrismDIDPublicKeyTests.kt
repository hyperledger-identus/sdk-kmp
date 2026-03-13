package org.hyperledger.identus.walletsdk.castor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hyperledger.identus.protos.PublicKey
import org.hyperledger.identus.walletsdk.apollo.utils.Ed25519KeyPair
import org.hyperledger.identus.walletsdk.apollo.utils.Ed25519PrivateKey
import org.hyperledger.identus.walletsdk.apollo.utils.Ed25519PublicKey
import org.hyperledger.identus.walletsdk.castor.did.prismdid.PrismDIDPublicKey
import org.hyperledger.identus.walletsdk.castor.did.prismdid.id
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class PrismDIDPublicKeyTests {

    @Test
    fun it_should_return_master_without_index_for_master_key_id() {
        // Master key ID should be "master" (no index suffix) per spec normalization
        val masterKeyId = PrismDIDPublicKey.Usage.MASTER_KEY.id(0)
        assertEquals("master", masterKeyId)
        // Master key ID should be the same regardless of the index parameter
        val masterKeyId1 = PrismDIDPublicKey.Usage.MASTER_KEY.id(1)
        assertEquals("master", masterKeyId1)
    }

    @Test
    fun it_should_return_indexed_id_for_other_key_types() {
        assertEquals("issuing0", PrismDIDPublicKey.Usage.ISSUING_KEY.id(0))
        assertEquals("issuing1", PrismDIDPublicKey.Usage.ISSUING_KEY.id(1))
        assertEquals("authentication0", PrismDIDPublicKey.Usage.AUTHENTICATION_KEY.id(0))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Ignore("PrismDIDPublicKey requires Secp256k1Lib to be an interface in order to mock its result. Once that is done this test can be added back.")
    @Test
    fun it_should_parse_proto_toPrismDIDPublicKey() = runTest {
        val apollo = ApolloMock()
        val seed = apollo.createRandomSeed(passphrase = "mnemonics").seed
        val keyPair = Ed25519KeyPair(
            privateKey = Ed25519PrivateKey(ByteArray(0)),
            publicKey = Ed25519PublicKey(ByteArray(0))
        )

        val publicKey = PrismDIDPublicKey(
            apollo = ApolloMock(),
            id = PrismDIDPublicKey.Usage.MASTER_KEY.id(0),
            usage = PrismDIDPublicKey.Usage.MASTER_KEY,
            keyData = keyPair.publicKey
        )
        val protoData = publicKey.toProto()
        val proto = PublicKey(
            id = protoData.id,
            usage = protoData.usage,
            keyData = protoData.keyData
        )
        val parsedPublicKey = PrismDIDPublicKey(
            apollo = apollo,
            proto = proto
        )
        assertEquals(parsedPublicKey.id, "master")
        assertContentEquals(parsedPublicKey.keyData.raw, publicKey.keyData.raw)
        assertEquals(parsedPublicKey.usage, publicKey.usage)
    }
}
