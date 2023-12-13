package io.iohk.atala.prism.walletsdk.prismagent.protocols.prismOnboarding

import io.iohk.atala.prism.walletsdk.prismagent.PrismAgentError
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Represents an onboarding invitation in Prism.
 *
 * This class is responsible for parsing and storing the information from a Prism onboarding invitation.
 *
 * @constructor Creates a PrismOnboardingInvitation object from a JSON string representation of the invitation.
 * @param jsonString The JSON string representation of the invitation.
 * @throws PrismAgentError.InvitationIsInvalidError If the JSON string is invalid and cannot be parsed.
 */
class PrismOnboardingInvitation
@Throws(PrismAgentError.InvitationIsInvalidError::class)
constructor(jsonString: String) {
    var body: Body

    init {
        val json = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
        }
        body = try {
            json.decodeFromString(jsonString)
        } catch (e: Throwable) {
            throw PrismAgentError.InvitationIsInvalidError()
        }
    }

    /**
     * Represents the body of an invitation in the PrismOnboardingInvitation class.
     *
     * @property type The type of the invitation.
     * @property onboardEndpoint The onboard endpoint of the invitation.
     * @property from The sender of the invitation.
     */
    @Serializable
    data class Body(
        val type: String,
        val onboardEndpoint: String,
        val from: String
    )
}
