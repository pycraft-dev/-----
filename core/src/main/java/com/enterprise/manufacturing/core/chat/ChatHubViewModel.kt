package com.enterprise.manufacturing.core.chat

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.manufacturing.core.chat.data.GeneralChatRepository
import com.enterprise.manufacturing.core.chat.media.VoiceRecorder
import com.enterprise.manufacturing.core.db.dao.UserDao
import com.enterprise.manufacturing.core.model.UserRole
import com.enterprise.manufacturing.core.session.AuthSessionRepository
import com.enterprise.manufacturing.core.session.SessionSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ChatHubViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: GeneralChatRepository,
    private val authSessionRepository: AuthSessionRepository,
    userDao: UserDao,
) : ViewModel() {

    private val voiceRecorder = VoiceRecorder(context)

    val messages =
        repository.observeMessages().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val users =
        userDao.observeAll().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val isAdmin =
        authSessionRepository.observeSessionSnapshot().map { snap ->
            (snap as? SessionSnapshot.Active)?.role == UserRole.ADMIN
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    private val _currentUserId = MutableStateFlow<Long?>(null)
    val currentUserId: StateFlow<Long?> = _currentUserId.asStateFlow()

    private val _chatScreenVisible = MutableStateFlow(false)
    val chatScreenVisible: StateFlow<Boolean> = _chatScreenVisible.asStateFlow()

    private val _recording = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording.asStateFlow()

    private val _playingMessageId = MutableStateFlow<Long?>(null)
    val playingMessageId: StateFlow<Long?> = _playingMessageId.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    init {
        viewModelScope.launch {
            val snap = authSessionRepository.observeSessionSnapshot().first()
            val active = snap as? SessionSnapshot.Active
            _currentUserId.value = active?.userId
        }
    }

    fun setChatScreenVisible(visible: Boolean) {
        _chatScreenVisible.value = visible
    }

    fun isUserOnline(userId: Long): Boolean {
        val self = _currentUserId.value ?: return false
        return userId == self && _chatScreenVisible.value
    }

    fun sendText(text: String) {
        viewModelScope.launch {
            val uid = activeUserId() ?: return@launch
            repository.sendText(uid, text)
        }
    }

    fun startRecording() {
        if (_recording.value) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                voiceRecorder.start()
                _recording.value = true
            }
        }
    }

    fun stopRecordingAndSend() {
        if (!_recording.value) return
        viewModelScope.launch(Dispatchers.IO) {
            val uid = activeUserId() ?: run {
                voiceRecorder.discard()
                _recording.value = false
                return@launch
            }
            val pair = voiceRecorder.stop()
            _recording.value = false
            if (pair == null) return@launch
            val (file, dur) = pair
            if (dur < 300L) {
                file.delete()
                return@launch
            }
            repository.sendVoiceMessage(uid, file, dur)
        }
    }

    fun cancelRecording() {
        viewModelScope.launch(Dispatchers.IO) {
            voiceRecorder.discard()
            _recording.value = false
        }
    }

    fun sendAttachment(uri: Uri, caption: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = activeUserId() ?: return@launch
            repository.sendFileMessage(uid, uri, caption)
        }
    }

    fun mergeTranscriptFromSpeech(messageId: Long, spokenText: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.mergeTranscript(messageId, spokenText.orEmpty())
        }
    }

    fun togglePlayVoice(messageId: Long, path: String) {
        viewModelScope.launch {
            if (_playingMessageId.value == messageId) {
                stopPlayback()
                return@launch
            }
            stopPlayback()
            withContext(Dispatchers.IO) {
                runCatching {
                    val mp = MediaPlayer()
                    mp.setDataSource(path)
                    mp.prepare()
                    mp.start()
                    mp.setOnCompletionListener {
                        _playingMessageId.value = null
                        it.release()
                        if (mediaPlayer === it) mediaPlayer = null
                    }
                    mediaPlayer = mp
                    _playingMessageId.value = messageId
                }
            }
        }
    }

    fun stopPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
        _playingMessageId.value = null
    }

    private suspend fun activeUserId(): Long? {
        val snap = authSessionRepository.observeSessionSnapshot().first()
        return (snap as? SessionSnapshot.Active)?.userId
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
        voiceRecorder.discard()
    }
}
