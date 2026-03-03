package org.hyperledger.identus.walletsdk.ui.agent

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.hyperledger.identus.walletsdk.sampleapp.Sdk

class AgentViewModel(application: Application) : AndroidViewModel(application) {

    private val storedMediatorDid: MutableLiveData<String?> = MutableLiveData(null)
    private val startAgentError: MutableLiveData<String?> = MutableLiveData(null)

    fun storedMediatorDid(): LiveData<String?> {
        return storedMediatorDid
    }

    fun startAgentError(): LiveData<String?> {
        return startAgentError
    }

    fun loadStoredMediatorDid() {
        val sdk = Sdk.getInstance()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                sdk.startPluto(getApplication())
            } catch (e: Exception) {
                if (e.javaClass.name != "org.hyperledger.identus.walletsdk.domain.models.PlutoError\$DatabaseServiceAlreadyRunning") {
                    storedMediatorDid.postValue(null)
                    return@launch
                }
            }
            val mediator = sdk.pluto.getAllMediators().first().firstOrNull()
            storedMediatorDid.postValue(mediator?.mediatorDID?.toString())
        }
    }

    fun startAgent(mediatorDID: String) {
        val sdk = Sdk.getInstance()
        viewModelScope.launch {
            try {
                sdk.startAgent(mediatorDID, getApplication<Application>())
            } catch (e: Throwable) {
                startAgentError.postValue(e.message ?: "Failed to start agent")
            }
        }
    }
}
