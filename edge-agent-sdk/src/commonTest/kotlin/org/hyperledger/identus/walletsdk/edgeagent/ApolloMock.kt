package org.hyperledger.identus.walletsdk.edgeagent

import io.iohk.atala.prism.didcomm.didpeer.base64.base64UrlDecodedBytes
import org.bouncycastle.util.encoders.Hex
import org.hyperledger.identus.walletsdk.apollo.utils.Ed25519KeyPair
import org.hyperledger.identus.walletsdk.apollo.utils.Ed25519PrivateKey
import org.hyperledger.identus.walletsdk.apollo.utils.Ed25519PublicKey
import org.hyperledger.identus.walletsdk.apollo.utils.Secp256k1PrivateKey
import org.hyperledger.identus.walletsdk.domain.buildingblocks.Apollo
import org.hyperledger.identus.walletsdk.domain.models.Curve
import org.hyperledger.identus.walletsdk.domain.models.Seed
import org.hyperledger.identus.walletsdk.domain.models.SeedWords
import org.hyperledger.identus.walletsdk.domain.models.keyManagement.JWK
import org.hyperledger.identus.walletsdk.domain.models.keyManagement.Key
import org.hyperledger.identus.walletsdk.domain.models.keyManagement.KeyPair
import org.hyperledger.identus.walletsdk.domain.models.keyManagement.PrivateKey
import org.hyperledger.identus.walletsdk.domain.models.keyManagement.PublicKey
import org.hyperledger.identus.walletsdk.domain.models.keyManagement.StorableKey

class ApolloMock : Apollo {
    val validRawBase64UrlSecp256k1Sk = Hex.decode("67E56582298859DDAE725F972992A07C6C4FB9F62A8FFF58CE3CA926A1063530".lowercase())
    val validRawBase64UrlEd25519Sk = "JLIJQ5jlkyqtGmtOth6yggJLLC0zuRhUPiBhd1-rGPs".base64UrlDecodedBytes
    var createRandomMnemonicsReturn: Array<String> = emptyArray()
    var createSeedReturn: Seed = Seed(ByteArray(0))
    var createRandomSeedReturn: SeedWords = SeedWords(emptyArray(), Seed(ByteArray(0)))
    var createKeyPairReturn: KeyPair = Ed25519KeyPair(
        privateKey = Ed25519PrivateKey(ByteArray(0)),
        publicKey = Ed25519PublicKey(ByteArray(0))
    )
    var createPrivateKey: PrivateKey? = null

    override fun createRandomMnemonics(): Array<String> = createRandomMnemonicsReturn

    override fun createSeed(mnemonics: Array<String>, passphrase: String): Seed {
        return createSeedReturn
    }

    override fun createRandomSeed(passphrase: String?): SeedWords {
        return createRandomSeedReturn
    }

    override fun createPrivateKey(properties: Map<String, Any>): PrivateKey {
        val curve = properties["curve"]
        if (curve === Curve.SECP256K1.value) {
            return Secp256k1PrivateKey(validRawBase64UrlSecp256k1Sk)
        } else if (curve === Curve.ED25519.value) {
            return Ed25519PrivateKey(validRawBase64UrlEd25519Sk)
        }
        TODO("Not yet implemented")
    }

    override fun createPublicKey(properties: Map<String, Any>): PublicKey {
        TODO("Not yet implemented")
    }

    override fun isPrivateKeyData(identifier: String, data: ByteArray): Boolean {
        TODO("Not yet implemented")
    }

    override fun isPublicKeyData(identifier: String, data: ByteArray): Boolean {
        TODO("Not yet implemented")
    }

    override fun restorePrivateKey(key: StorableKey): PrivateKey {
        TODO("Not yet implemented")
    }

    override fun restorePrivateKey(restorationIdentifier: String, privateKeyData: String): PrivateKey {
        TODO("Not yet implemented")
    }

    override fun restorePublicKey(key: StorableKey): PublicKey {
        TODO("Not yet implemented")
    }

    override fun restoreKey(key: JWK, index: Int?): Key {
        TODO("Not yet implemented")
    }
}
