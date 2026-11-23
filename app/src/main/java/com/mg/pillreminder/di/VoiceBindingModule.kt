package com.mg.pillreminder.di

import com.mg.pillreminder.domain.voice.SpeechListener
import com.mg.pillreminder.domain.voice.SpeechSpeaker
import com.mg.pillreminder.voice.AndroidSpeechListener
import com.mg.pillreminder.voice.AndroidSpeechSpeaker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

// 스코프를 두지 않아 ViewModel마다 새 인스턴스를 쓴다. TTS 엔진을 ViewModel이 정리할 수 있어야 해서다.
@Module
@InstallIn(ViewModelComponent::class)
interface VoiceBindingModule {

    @Binds
    fun bindSpeechListener(impl: AndroidSpeechListener): SpeechListener

    @Binds
    fun bindSpeechSpeaker(impl: AndroidSpeechSpeaker): SpeechSpeaker
}
