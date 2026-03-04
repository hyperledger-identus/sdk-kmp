@file:Suppress("ktlint:standard:import-ordering")

package org.hyperledger.identus.walletsdk.sampleapp.ui.messages

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hyperledger.identus.walletsdk.db.AppDatabase
import org.hyperledger.identus.walletsdk.db.DatabaseClient
import org.hyperledger.identus.walletsdk.domain.DIDCOMM_MESSAGING
import org.hyperledger.identus.walletsdk.domain.models.AnoncredsInputFieldFilter
import org.hyperledger.identus.walletsdk.domain.models.AnoncredsPresentationClaims
import org.hyperledger.identus.walletsdk.domain.models.Credential
import org.hyperledger.identus.walletsdk.domain.models.CredentialType
import org.hyperledger.identus.walletsdk.domain.models.DID
import org.hyperledger.identus.walletsdk.domain.models.DIDDocument
import org.hyperledger.identus.walletsdk.domain.models.KeyPurpose
import org.hyperledger.identus.walletsdk.domain.models.Message
import org.hyperledger.identus.walletsdk.domain.models.ProvableCredential
import org.hyperledger.identus.walletsdk.domain.models.RequestedAttributes
import org.hyperledger.identus.walletsdk.apollo.utils.Ed25519KeyPair
import org.hyperledger.identus.walletsdk.apollo.utils.Secp256k1KeyPair
import org.hyperledger.identus.walletsdk.edgeagent.DIDCOMM1
import org.hyperledger.identus.walletsdk.edgeagent.EdgeAgentError
import org.hyperledger.identus.walletsdk.edgeagent.protocols.ProtocolType
import org.hyperledger.identus.walletsdk.edgeagent.protocols.issueCredential.IssueCredential
import org.hyperledger.identus.walletsdk.edgeagent.protocols.issueCredential.OfferCredential
import org.hyperledger.identus.walletsdk.edgeagent.protocols.proofOfPresentation.RequestPresentation
import org.hyperledger.identus.walletsdk.sampleapp.Sdk
import org.hyperledger.identus.walletsdk.sampleapp.db.Message as MessageEntity
import org.hyperledger.identus.walletsdk.sampleapp.db.PendingProofRequest

class MessagesViewModel(application: Application) : AndroidViewModel(application) {

    private var messages: MutableLiveData<List<Message>> = MutableLiveData()
    private val pendingProofRequests: MutableLiveData<List<Message>> = MutableLiveData(emptyList())
    private val issuedCredentials: ArrayList<String> = arrayListOf()
    private val processedOffers: ArrayList<String> = arrayListOf()
    private val processedProofRequests: ArrayList<String> = arrayListOf()
    private val pendingProofRequestIds: MutableSet<String> = mutableSetOf()
    private val db: AppDatabase = DatabaseClient.getInstance()
    private val revokedCredentialsNotified: MutableList<Credential> = mutableListOf()
    private var revokedCredentials: MutableLiveData<List<Credential>> = MutableLiveData()
    private var errorLiveData: MutableLiveData<String> = MutableLiveData()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            db.messageDao().isMessageRead("")
            pendingProofRequestIds.addAll(db.proofRequestDao().getAllIds())
        }
    }

    private fun insertMessages(list: List<Message>) {
        list.forEach { msg ->
            db.messageDao()
                .insertMessage(MessageEntity(messageId = msg.id, isRead = false))
        }
    }

    fun messagesStream(): LiveData<List<Message>> {
        viewModelScope.launch(Dispatchers.IO) {
            val sdk = Sdk.getInstance()
            // Use pluto.getAllMessages() to show both sent and received messages
            sdk.pluto.getAllMessages().collect { list ->
                insertMessages(list)
                messages.postValue(list)
                refreshPendingFromMessages(list)
                // Only process received messages (for auto-handling offers, credentials, etc.)
                val receivedMessages = list.filter { it.direction == Message.Direction.RECEIVED }
                processMessages(receivedMessages)
            }
        }
        return messages
    }

    fun sendMessage(toDID: DID? = null) {
        CoroutineScope(Dispatchers.Default).launch {
            val sdk = Sdk.getInstance()
            val did = sdk.agent.createNewPeerDID(
                arrayOf(
                    DIDDocument.Service(
                        DIDCOMM1,
                        arrayOf(DIDCOMM_MESSAGING),
                        DIDDocument.ServiceEndpoint(sdk.handler.mediatorDID.toString())
                    )
                ),
                true
            )
            val time = LocalDateTime.now()
            val message = Message(
                piuri = ProtocolType.BasicMessage.value,
                from = did,
                to = toDID ?: did,
                body = "{\"msg\":\"This is a new test message ${time}\"}"
            )
            sdk.agent.sendMessage(message)
        }
    }

    fun sendVerificationRequest(toDID: String) {
        CoroutineScope(Dispatchers.Default).launch {
            val sdk = Sdk.getInstance()
            // JWT presentation request
//            sdk.agent.initiatePresentationRequest(
//                type = CredentialType.JWT,
//                toDID = DID(toDID),
//                presentationClaims = JWTPresentationClaims(
//                    claims = mapOf(
//                        "emailAddress" to InputFieldFilter(
//                            type = "string",
//                            pattern = "cristian.castro@iohk.io"
//                        )
//                    )
//                ),
//                domain = "domain",
//                challenge = "challenge"
//            )

            // Anoncreds presentation request
            sdk.agent.initiatePresentationRequest(
                type = CredentialType.ANONCREDS_PROOF_REQUEST,
                toDID = DID(toDID),
                presentationClaims = AnoncredsPresentationClaims(
                    predicates = mapOf(
                        "0_age" to AnoncredsInputFieldFilter(
                            type = "string",
                            name = "age",
                            gte = 18
                        )
                    ),
                    attributes = mapOf(
                        "0_name" to RequestedAttributes(
                            "name",
                            setOf("name"),
                            emptyMap(),
                            null
                        )
                    )
                )
            )
        }
    }

    fun pendingProofRequests(): LiveData<List<Message>> = pendingProofRequests

    fun preparePresentationProof(
        credential: Credential,
        message: Message,
        disclosingClaims: List<String>? = null
    ) {
        val sdk = Sdk.getInstance()
        sdk.agent.let { agent ->
            sdk.mercury.let { mercury ->
                viewModelScope.launch {
                    if (credential is ProvableCredential) {
                        try {
                            val presentation = agent.preparePresentationForRequestProof(
                                RequestPresentation.fromMessage(message),
                                credential,
                                disclosingClaims
                            )
                            sdk.agent.sendMessage(presentation.makeMessage())
                            removePendingProofRequest(message.id)
                        } catch (e: EdgeAgentError.CredentialNotValidForPresentationRequest) {
                            errorLiveData.postValue(e.message)
                        } catch (e: Exception) {
                            errorLiveData.postValue(e.message ?: "Unknown error")
                        }
                    }
                }
            }
        }
    }

    fun revokedCredentialsStream(): LiveData<List<Credential>> {
        viewModelScope.launch {
            Sdk.getInstance().agent.let {
                it.observeRevokedCredentials().collect { list ->
                    val newRevokedCredentials = list.filter { newCredential ->
                        revokedCredentialsNotified.none { notifiedCredential ->
                            notifiedCredential.id == newCredential.id
                        }
                    }
                    if (newRevokedCredentials.isNotEmpty()) {
                        revokedCredentialsNotified.addAll(newRevokedCredentials)
                        revokedCredentials.postValue(newRevokedCredentials)
                    } else {
                        revokedCredentials.postValue(emptyList())
                    }
                }
            }
        }
        return revokedCredentials
    }

    fun handlePresentation(uiMessage: UiMessage): LiveData<String> {
        val liveData = MutableLiveData<String>()
        val handler = CoroutineExceptionHandler { _, exception ->
            liveData.postValue(exception.message)
        }
        viewModelScope.launch(handler) {
            messages.value?.find { it.id == uiMessage.id }?.let { message ->
                val sdk = Sdk.getInstance()
                val valid = sdk.agent.handlePresentation(message)
                if (valid) {
                    liveData.postValue("Valid!")
                } else {
                    liveData.postValue("Not valid!")
                }
            }
        }
        return liveData
    }

    fun streamError(): LiveData<String> = errorLiveData

    private suspend fun processMessages(messages: List<Message>) {
        val sdk = Sdk.getInstance()
        val messageIds: List<String> = messages.map { it.id }
        val messagesReadStatus =
            db.messageDao().areMessagesRead(messageIds).associate { it.messageId to it.isRead }
        messages.forEach { message ->
            if (messagesReadStatus[message.id] == false) {
                sdk.agent.let { agent ->
                    sdk.pluto.let { pluto ->
                        sdk.mercury.let { mercury ->
                            if (message.piuri == ProtocolType.DidcommOfferCredential.value) {
                                message.thid?.let {
                                    if (!processedOffers.contains(it)) {
                                        processedOffers.add(it)
                                        viewModelScope.launch {
                                            val offer = OfferCredential.fromMessage(message)
                                            // Choose key type based on credential format (matching e2e test logic)
                                            val format = offer.attachments.firstOrNull()?.format.orEmpty()
                                            val subjectDID = when {
                                                format.contains("anoncred", ignoreCase = true) -> {
                                                    // AnonCreds: use default DID
                                                    agent.createNewPrismDID()
                                                }
                                                format.contains("sd-jwt", ignoreCase = true) -> {
                                                    // SD-JWT: use Ed25519 key
                                                    val authKeyPair = Ed25519KeyPair.generateKeyPair()
                                                    agent.createNewPrismDID(
                                                        keys = listOf(Pair(KeyPurpose.AUTHENTICATION, authKeyPair.privateKey))
                                                    )
                                                }
                                                else -> {
                                                    // JWT and others: use Secp256k1 key
                                                    val authKeyPair = Secp256k1KeyPair.generateKeyPair()
                                                    agent.createNewPrismDID(
                                                        keys = listOf(Pair(KeyPurpose.AUTHENTICATION, authKeyPair.privateKey))
                                                    )
                                                }
                                            }
                                            val request =
                                                agent.prepareRequestCredentialWithIssuer(
                                                    subjectDID,
                                                    offer
                                                )
                                            agent.sendMessage(request.makeMessage())
                                        }
                                    }
                                }
                            }
                            if (message.piuri == ProtocolType.DidcommIssueCredential.value) {
                                message.thid?.let {
                                    if (!issuedCredentials.contains(it)) {
                                        issuedCredentials.add(it)
                                        viewModelScope.launch {
                                            agent.processIssuedCredentialMessage(
                                                IssueCredential.fromMessage(
                                                    message
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            if (message.piuri == ProtocolType.DidcommRequestPresentation.value && message.direction == Message.Direction.RECEIVED) {
                                message.thid?.let {
                                    if (!processedProofRequests.contains(it)) {
                                        processedProofRequests.add(it)
                                        addPendingProofRequest(message)
                                    }
                                }
                            }
                        }

                        db.messageDao()
                            .updateMessage(MessageEntity(messageId = message.id, isRead = true))
                    }
                }
            }
        }
    }

    private fun addPendingProofRequest(message: Message) {
        synchronized(pendingProofRequestIds) {
            if (pendingProofRequestIds.contains(message.id)) return
            pendingProofRequestIds.add(message.id)
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.proofRequestDao().insertPending(
                PendingProofRequest(
                    messageId = message.id,
                    thid = message.thid,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        val current = pendingProofRequests.value.orEmpty()
        pendingProofRequests.postValue(current + message)
    }

    private fun removePendingProofRequest(messageId: String) {
        val removed = synchronized(pendingProofRequestIds) {
            pendingProofRequestIds.remove(messageId)
        }
        if (removed) {
            viewModelScope.launch(Dispatchers.IO) {
                db.proofRequestDao().deletePending(messageId)
            }
        }
        val current = pendingProofRequests.value.orEmpty()
        pendingProofRequests.postValue(current.filterNot { it.id == messageId })
    }

    private fun refreshPendingFromMessages(messages: List<Message>) {
        if (pendingProofRequestIds.isEmpty()) {
            pendingProofRequests.postValue(emptyList())
            return
        }
        val pending = messages.filter { pendingProofRequestIds.contains(it.id) }
        pendingProofRequests.postValue(pending)
    }
}
