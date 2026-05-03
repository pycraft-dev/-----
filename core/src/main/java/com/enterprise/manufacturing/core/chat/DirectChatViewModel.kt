package com.enterprise.manufacturing.core.chat

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.manufacturing.core.chat.data.GeneralChatRepository
import com.enterprise.manufacturing.core.chat.media.VoiceMessageTranscriber
import com.enterprise.manufacturing.core.chat.media.VoiceRecorder
import com.enterprise.manufacturing.core.db.dao.UserDao
import com.enterprise.manufacturing.core.db.entity.GeneralChatMessageEntity
import com.enterprise.manufacturing.core.db.entity.UserEntity
import com.enterprise.manufacturing.core.model.UserRole
import com.enterprise.manufacturing.core.navigation.DirectChatNavArgs
import com.enterprise.manufacturing.core.session.AuthSessionRepository
import com.enterprise.manufacturing.core.session.SessionSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DirectChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    private val repository: GeneralChatRepository,
    private val authSessionRepository: AuthSessionRepository,
    userDao: UserDao,
) : ViewModel() {

    private val peerUserId: Long =
        checkNotNull(savedStateHandle.get<Long>(DirectChatNavArgs.PeerUserId))

    /** Для синхронизации Supabase из UI. */
    val conversationPeerUserId: Long = peerUserId

    private val voiceRecorder = VoiceRecorder(appContext)

    val peerUser: StateFlow<UserEntity?> =
        userDao.observeById(peerUserId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val messages: StateFlow<List<GeneralChatMessageEntity>> =
        authSessionRepository.observeSessionSnapshot()
            .map { snap -> (snap as? SessionSnapshot.Active)?.userId }
            .distinctUntilChanged()
            .flatMapLatest { me ->
                if (me == null) {
                    flowOf(emptyList())
                } else {
                    repository.observeDirectMessages(peerUserId, me)
                }
            }
            .stateIn(
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

    private val _voiceTranscriptFailed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val voiceTranscriptFailed = _voiceTranscriptFailed.asSharedFlow()

    private var mediaPlayer: MediaPlayer? = null

    init {
        viewModelScope.launch {
            authSessionRepository.observeSessionSnapshot().collect { snap ->
                val active = snap as? SessionSnapshot.Active
                _currentUserId.value = active?.userId
            }
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
            val me = activeUserId() ?: return@launch
            repository.sendText(me, peerUserId, text)
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
            val me = activeUserId() ?: run {
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
            repository.sendVoiceMessage(me, peerUserId, file, dur)
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
            val me = activeUserId() ?: return@launch
            repository.sendFileMessage(me, peerUserId, uri, caption)
        }
    }

    fun mergeTranscriptFromSpeech(messageId: Long, spokenText: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.mergeTranscript(messageId, spokenText.orEmpty())
        }
    }

    fun transcribeVoiceMessage(messageId: Long, audioPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(audioPath)
            if (!file.exists()) return@launch
            val text =
                VoiceMessageTranscriber.transcribe(
                    appContext,
                    file,
                    Locale.forLanguageTag("ru-RU"),
                )
            if (!text.isNullOrBlank()) {
                repository.setTranscript(messageId, text)
            } else {
                _voiceTranscriptFailed.emit(Unit)
            }
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
        val snap =
            authSessionRepository.observeSessionSnapshot()
                .first { it !is SessionSnapshot.Loading }
        return (snap as? SessionSnapshot.Active)?.userId
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
        voiceRecorder.discard()
    }
}
