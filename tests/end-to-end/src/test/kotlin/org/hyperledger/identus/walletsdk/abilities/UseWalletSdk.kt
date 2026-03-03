package org.hyperledger.identus.walletsdk.abilities

import com.jayway.jsonpath.JsonPath
import io.iohk.atala.automation.utils.Logger
import io.restassured.RestAssured
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.serenitybdd.screenplay.Ability
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.HasTeardown
import net.serenitybdd.screenplay.Question
import net.serenitybdd.screenplay.SilentInteraction
import org.hyperledger.identus.walletsdk.apollo.ApolloImpl
import org.hyperledger.identus.walletsdk.castor.CastorImpl
import org.hyperledger.identus.walletsdk.castor.resolvers.PrismDIDApiResolver
import org.hyperledger.identus.walletsdk.configuration.DbConnectionInMemory
import org.hyperledger.identus.walletsdk.configuration.Environment
import org.hyperledger.identus.walletsdk.domain.models.ApiImpl
import org.hyperledger.identus.walletsdk.domain.models.DID
import org.hyperledger.identus.walletsdk.domain.models.Message
import org.hyperledger.identus.walletsdk.domain.models.Seed
import org.hyperledger.identus.walletsdk.domain.models.httpClient
import org.hyperledger.identus.walletsdk.edgeagent.EdgeAgent
import org.hyperledger.identus.walletsdk.edgeagent.helpers.AgentOptions
import org.hyperledger.identus.walletsdk.edgeagent.helpers.Experiments
import org.hyperledger.identus.walletsdk.edgeagent.mediation.BasicMediatorHandler
import org.hyperledger.identus.walletsdk.edgeagent.protocols.ProtocolType
import org.hyperledger.identus.walletsdk.mercury.MercuryImpl
import org.hyperledger.identus.walletsdk.mercury.resolvers.DIDCommWrapper
import org.hyperledger.identus.walletsdk.pluto.PlutoImpl
import org.hyperledger.identus.walletsdk.pollux.PolluxImpl
import org.lighthousegames.logging.KmLogging
import org.lighthousegames.logging.LogLevel
import java.util.Base64
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

class UseWalletSdk : Ability, HasTeardown {

    companion object {
        // Retrieve the ability from the Actor
        private fun asAbility(actor: Actor): UseWalletSdk {
            if (actor.abilityTo(UseWalletSdk::class.java) != null) {
                val ability = actor.abilityTo(UseWalletSdk::class.java)
                if (!ability.isInitialized) {
                    ability.initialize()
                }
            }
            return actor.abilityTo(UseWalletSdk::class.java) ?: throw ActorCannotUseWalletSdk(actor)
        }

        // Questions exposed for Serenity assertions
        fun credentialOfferStackSize(): Question<Int> {
            return Question.about("credential offer stack").answeredBy {
                asAbility(it).context.credentialOfferStack.size
            }
        }

        fun issuedCredentialStackSize(): Question<Int> {
            return Question.about("issued credential stack").answeredBy {
                asAbility(it).context.issuedCredentialStack.size
            }
        }

        fun proofOfRequestStackSize(): Question<Int> {
            return Question.about("proof of request stack").answeredBy {
                asAbility(it).context.proofRequestStack.size
            }
        }

        fun revocationStackSize(): Question<Int> {
            return Question.about("revocation messages stack").answeredBy {
                asAbility(it).context.revocationStack.size
            }
        }

        fun presentationStackSize(): Question<Int> {
            return Question.about("presentation messages stack").answeredBy {
                asAbility(it).context.presentationStack.size
            }
        }

        // Execute block to interact with the SDK context safely
        fun execute(callback: suspend (sdk: SdkContext) -> Unit): SilentInteraction {
            return object : SilentInteraction() {
                override fun <T : Actor> performAs(actor: T) {
                    val asActor = asAbility(actor)
                    runBlocking {
                        callback(asActor.context)
                    }
                }
            }
        }
    }

    private val logger = Logger.get<UseWalletSdk>()
    lateinit var context: SdkContext
        private set

    // Track received message IDs to prevent duplicates (matching TS MessageQueue logic)
    private val receivedMessages = Collections.synchronizedList(mutableListOf<String>())

    var isInitialized = false
        private set

    private var fetchJob: Job? = null

    /**
     * Standard initialization with a random seed
     */
    fun initialize() {
        createSdk(null)
        startPluto()
        startSdk()
        listenToMessages()
        isInitialized = true
    }

    /**
     * Recover wallet flow (Backup/Restore)
     */
    fun recoverWallet(seed: Seed, jwe: String) {
        // 1. Re-create SDK with the specific seed
        createSdk(seed)
        startPluto()

        // 2. Perform Recovery
        runBlocking {
            context.sdk.recoverWallet(jwe)
        }

        // 3. Start usual operations
        startSdk()
        listenToMessages()
        isInitialized = true
    }

    /**
     * Initializes the SDK with a specific seed (or generates one)
     */
    fun createSdk(initialSeed: Seed? = null) {
        val api = ApiImpl(httpClient())
        val apollo = ApolloImpl()
        val castor = CastorImpl(apollo)
        val pluto = PlutoImpl(DbConnectionInMemory())
        val pollux = PolluxImpl(apollo, castor, api)
        val didcommWrapper = DIDCommWrapper(castor, pluto, apollo)
        val mercury = MercuryImpl(castor, didcommWrapper, api)

        castor.addResolver(
            PrismDIDApiResolver(
                apollo = apollo,
                resolverBaseUrl = Environment.agent.url,
                api = api
            )
        )

        // Parse Mediator DID from OOB URL
        val mediatorDidString = getMediatorDidThroughOob()
        val mediatorDid = DID(mediatorDidString)

        val store = BasicMediatorHandler.PlutoMediatorRepositoryImpl(pluto)
        val handler = BasicMediatorHandler(mediatorDid, mercury, store)

        val seed = initialSeed ?: apollo.createRandomSeed().seed

        val sdk = EdgeAgent(
            apollo = apollo,
            castor = castor,
            pluto = pluto,
            mercury = mercury,
            pollux = pollux,
            seed = seed,
            api = api,
            mediatorHandler = handler,
            agentOptions = AgentOptions(
                experiments = Experiments(
                    liveMode = false
                )
            )
        )

        // TS registers Anoncreds plugin here.
        // In Kotlin SDK, ensure you have the anoncreds-plugin dependency in build.gradle.
        // It is often auto-detected or registered internally by EdgeAgent if on classpath.

        KmLogging.setLogLevel(LogLevel.Warn)

        this.context = SdkContext(sdk)
    }

    private fun startPluto() {
        runBlocking {
            context.sdk.pluto.start()
        }
    }

    private fun startSdk() {
        runBlocking {
            context.sdk.start()
        }
        // Start fetching messages (polling)
        context.sdk.startFetchingMessages(1)
    }

    private fun listenToMessages() {
        // Cancel previous job if exists
        fetchJob?.cancel()

        fetchJob = CoroutineScope(Dispatchers.Default).launch {
            context.sdk.handleReceivedMessagesEvents().collect { messageList: List<Message> ->
                messageList.forEach { message ->
                    // Prevent duplicate processing
                    if (receivedMessages.contains(message.id)) {
                        return@forEach
                    }
                    receivedMessages.add(message.id)

                    // Route message to appropriate stack based on PIURI
                    when (message.piuri) {
                        ProtocolType.DidcommOfferCredential.value -> context.credentialOfferStack.add(message)
                        ProtocolType.DidcommRequestPresentation.value -> context.proofRequestStack.add(message)
                        ProtocolType.DidcommIssueCredential.value -> context.issuedCredentialStack.add(message)
                        ProtocolType.PrismRevocation.value -> context.revocationStack.add(message)
                        ProtocolType.DidcommPresentation.value -> context.presentationStack.add(message)
                        ProtocolType.ProblemReport.value -> logger.error("Received problem report: ${message.body}")
                        else -> logger.debug("Received unhandled message type: ${message.piuri}")
                    }
                }
            }
        }
    }

    override fun tearDown() {
        if (isInitialized) {
            context.sdk.stopFetchingMessages()
            runBlocking { context.sdk.stop() }
            fetchJob?.cancel()
            isInitialized = false
        }
    }

    private fun getMediatorDidThroughOob(): String {
        // Fetch Mediator OOB URL from Environment config
        val response = RestAssured.get(Environment.mediator.url)
        val oob = response.body.asString()

        // Logic to extract DID from OOB URL (similar to TS logic)
        // Splits by "?_oob=" and decodes Base64
        val parts = oob.split("?_oob=")
        if (parts.size < 2) throw IllegalStateException("Invalid OOB format from mediator: $oob")

        val encodedData = parts[1]
        val decodedData = String(Base64.getDecoder().decode(encodedData))
        val json = JsonPath.parse(decodedData)
        return json.read("from")
    }
}

/**
 * Context holder for the Agent and its message stacks.
 * Using CopyOnWriteArrayList for thread safety during frequent writes/reads in tests.
 */
data class SdkContext(
    val sdk: EdgeAgent,
    val credentialOfferStack: MutableList<Message> = CopyOnWriteArrayList(),
    val proofRequestStack: MutableList<Message> = CopyOnWriteArrayList(),
    val issuedCredentialStack: MutableList<Message> = CopyOnWriteArrayList(),
    val revocationStack: MutableList<Message> = CopyOnWriteArrayList(),
    val presentationStack: MutableList<Message> = CopyOnWriteArrayList()
)

class ActorCannotUseWalletSdk(actor: Actor) :
    Throwable("The actor [${actor.name}] does not have the ability to use wallet-sdk")
