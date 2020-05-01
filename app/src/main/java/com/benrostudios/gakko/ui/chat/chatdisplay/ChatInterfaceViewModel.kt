package com.benrostudios.gakko.ui.chat.chatdisplay

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.benrostudios.gakko.data.models.ChatMessage
import com.benrostudios.gakko.data.repository.ChatRepository

class ChatInterfaceViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {
    val usrChats = MutableLiveData<List<ChatMessage>>()

    init {
        chatRepository.chatMessages.observeForever {
            usrChats.postValue(it)
        }
    }

    suspend fun receiveMessages(){
        chatRepository.receiveMessage()
    }

    suspend fun sendMessage(message: ChatMessage){
        chatRepository.sendMessage(message)
    }
}