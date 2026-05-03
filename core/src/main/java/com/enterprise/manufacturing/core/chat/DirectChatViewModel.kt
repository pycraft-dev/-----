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
import com.enterprise.manufacturing.core.model.BuiltInRoleCodes
import com.enterprise.manufacturing.core.navigation.DirectChatNavArgs
import com.enterprise.manufacturing.core.session.AuthSessionRepository
import com.enterprise.manufacturing.core.session.SessionSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
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

    /**
     * Id текущего пользователя из сессии. [SharingStarted.Eagerly], чтобы при открытии чата сразу
     * подписаться на Room и не зависеть от короткой подписки UI.
     */
    private val _currentUserId: StateFlow<Long?> =
        authSessionRepository.observeSessionSnapshot()
            .map { snap ->
                when (snap) {
                    is SessionSnapshot.Active -> snap.userId
                    else -> null
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = null,
            )

    val currentUserId: StateFlow<Long?> = _currentUserId

    val peerUser: StateFlow<UserEntity?> =
        userDao.observeById(peerUserId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    /**
     * [shareIn], а не [stateIn]: для списков [StateFlow] может не эмитить при `equals` к предыдущему
     * снимку; Room при этом уже обновил таблицу — сообщение «пропадает» до перезахода.
     * Ветка `me == null` не завершается ([awaitCancellation]), иначе [flatMapLatest] теряет активный inner.
     */
    val messages: SharedFlow<List<GeneralChatMessageEntity>> =
        _currentUserId
            .flatMapLatest { me ->
                if (me == null) {
                    flow {
                        emit(emptyList())
                        awaitCancellation()
                    }
                } else {
                    repository.observeDirectMessages(peerUserId, me)
                }
            }
            .shareIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                replay = 1,
            )

    val isAdmin =
        authSessionRepository.observeSessionSnapshot().map { snap ->
            when (snap) {
                is SessionSnapshot.Active -> snap.roleCode == BuiltInRoleCodes.ADMIN
                else -> false
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    private val _chatScreenVisible = MutableStateFlow(false)
    val chatScreenVisible: StateFlow<Boolean> = _chatScreenVisible.asStateFlow()

    private val _recording = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording.asStateFlow()

    private val _playingMessageId = MutableStateFlow<Long?>(null)
    val playingMessageId: StateFlow<Long?> = _playingMessageId.asStateFlow()

    private val _voiceTranscriptFailed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val voiceTranscriptFailed = _voiceTranscriptFailed.asSharedFlow()

    private var mediaPlayer: MediaPlayer? = null

    fun setChatScreenVisible(visible: Boolean) {
        _chatScreenVisible.value = visible
    }

    fun isUserOnline(userId: Long): Boolean {
        val self = _currentUserId.value ?: return false
        return userId == self && _chatScreenVisible.value
    }

    /**
     * Тот же id, что и у [messages]: ждём [currentUserId] из сессии, без второго независимого
     * collect на [observeSessionSnapshot] (из‑за него сообщение могло уйти в Room, а список в UI оставался пустым).
     */
    private suspend fun senderUserIdOrThrow(): Long {
        _currentUserId.value?.let { return it }
        return withTimeoutOrNull(15_000L) { _currentUserId.filterNotNull().first() }
            ?: error("no active session")
    }

    fun sendText(text: String) {
        viewModelScope.launch {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return@launch
            val me = runCatching { senderUserIdOrThrow() }.getOrElse { return@launch }
            repository.sendText(me, peerUserId, trimmed)
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
            val me =
                runCatching { senderUserIdOrThrow() }.getOrElse {
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
            val me = runCatching { senderUserIdOrThrow() }.getOrElse { return@launch }
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

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
        voiceRecorder.discard()
    }
}
