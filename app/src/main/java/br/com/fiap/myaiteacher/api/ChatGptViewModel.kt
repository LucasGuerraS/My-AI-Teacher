package br.com.fiap.myaiteacher.api

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import br.com.fiap.myaiteacher.data.ApiClient
import br.com.fiap.myaiteacher.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatGptViewModel : ViewModel() {
    private val _messageList = MutableLiveData<MutableList<Message>>()
    val messageList: LiveData<MutableList<Message>> = _messageList

    init {
        _messageList.value = mutableListOf()
    }

    fun addToChat(message: String, sentBy: String, timestamp: String) {
        val currentList = _messageList.value ?: mutableListOf()
        currentList.add(Message(message, sentBy, timestamp))
        _messageList.postValue(currentList)
    }

    fun addResponse(response: String) {
        _messageList.value?.removeAt(_messageList.value?.size?.minus(1) ?: 0)
        addToChat(response, Message.SENT_BY_BOT, getCurrentTimestamp())
    }

    fun callApi(question: String) {
        addToChat("Typing....", Message.SENT_BY_BOT, getCurrentTimestamp())
        val completionRequest = CompletionRequest(
            model = "gpt-3.5-turbo",
            prompt = question,
            max_tokens = 4000
        )
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getCompletion(completionRequest)
                handleApiResponse(response)
            } catch (e: SocketTimeoutException) {
                addResponse("Timeout : $e")
            }
        }
    }

    private suspend fun handleApiResponse(response: Response<CompletionResponse>) {
        withContext(Dispatchers.Main) {
            if (response.isSuccessful) {
                response.body()?.let { completionResponse ->
                    val result = completionResponse.choices.firstOrNull()?.text
                    if (result != null) {
                        addResponse(result.trim())
                    } else {
                        addResponse("No choices found")
                    }
                }
            } else {
                addResponse("Files to get response ${response.errorBody()}")
            }
        }
    }


    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("hh mm a", Locale.getDefault()).format(Date())
        }
}