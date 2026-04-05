package org.hyperledger.identus.walletsdk.castor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hyperledger.identus.protos.PublicKey
import org.hyperledger.identus.apollo.base64.base64UrlEncoded
import org.hyperledger.identus.walletsdk.apollo.ApolloImpl
import org.hyperledger.identus.walletsdk.apollo.utils.Ed25519KeyPair
import org.hyperledger.identus.walletsdk.apollo.utils.Ed25519PrivateKey
import org.hyperledger.identus.walletsdk.apollo.utils.Ed25519PublicKey
import org.hyperledger.identus.walletsdk.apollo.utils.Secp256k1PublicKey
import org.hyperledger.identus.walletsdk.castor.did.prismdid.PrismDIDPublicKey
import org.hyperledger.identus.walletsdk.castor.did.prismdid.id
import org.hyperledger.identus.walletsdk.castor.shared.CastorShared
import org.hyperledger.identus.walletsdk.domain.models.Curve
import org.hyperledger.identus.walletsdk.domain.models.KeyPurpose
import org.hyperledger.identus.walletsdk.domain.models.keyManagement.CurveKey
import org.hyperledger.identus.walletsdk.domain.models.keyManagement.DerivationPathKey
import org.hyperledger.identus.walletsdk.domain.models.keyManagement.SeedKey
import org.hyperledger.identus.walletsdk.domain.models.keyManagement.TypeKey
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    @Test
    fun spec_test_vector_raw_seed_produces_expected_DID() = runTest {
        // Spec test vector from:
        // https://github.com/input-output-hk/prism-did-method-spec/blob/main/extensions/deterministic-prism-did-generation-proposal.md#examples--test-vector
        val specSeedHex = "3b32a5049f2b4e3af31ec5c1ae75fada1ad2eb8be5accf56ada343ad89eeb083208e538b3b97836e3bd7048c131421bf5bea9e3a1d25812a2d831e2bab89e058"
        val specSeedBytes = specSeedHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        val apollo = ApolloImpl()

        // Derive master key at m/29'/29'/0'/1'/0'
        val masterPrivateKey = apollo.createPrivateKey(
            mapOf(
                TypeKey().property to "EC",
                CurveKey().property to Curve.SECP256K1.value,
                SeedKey().property to specSeedBytes.base64UrlEncoded,
                DerivationPathKey().property to "m/29'/29'/0'/1'/0'"
            )
        )

        // Verify the compressed public key matches the spec test vector
        val publicKey = masterPrivateKey.publicKey() as Secp256k1PublicKey
        val compressedPubKeyHex = publicKey.getEncodedCompressed().joinToString("") { "%02x".format(it) }
        assertEquals("023f7c75c9e5fba08fea1640d6faa3f8dc0151261d2b56026d46ddcbe1fc5a5bbb", compressedPubKeyHex)

        // Create DID via CastorShared.createPrismDID — master-key-only CreateDID
        val did = CastorShared.createPrismDID(
            apollo = apollo,
            keys = listOf(KeyPurpose.MASTER to publicKey),
            services = null
        )

        // Extract canonical DID (short-form: did:prism:<hash>)
        val parts = did.toString().split(":")
        val canonicalDID = "${parts[0]}:${parts[1]}:${parts[2]}"

        // Verify the canonical DID matches the spec test vector exactly
        val expectedCanonicalDID = "did:prism:35fbaf7f8a68e927feb89dc897f4edc24ca8d7510261829e4834d931e947e6ca"
        assertEquals(expectedCanonicalDID, canonicalDID)

        // Verify determinism: same key → same DID
        val did2 = CastorShared.createPrismDID(
            apollo = apollo,
            keys = listOf(KeyPurpose.MASTER to publicKey),
            services = null
        )
        assertEquals(did.toString(), did2.toString())
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
