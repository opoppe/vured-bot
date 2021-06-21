package dev.jonaz.vured.bot.service.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.jonaz.vured.bot.music.AudioPlayerSendHandler
import dev.jonaz.vured.bot.music.AudioLoadResultManager
import dev.jonaz.vured.bot.music.TrackScheduler
import dev.jonaz.vured.bot.service.discord.GuildService
import dev.jonaz.vured.bot.service.discord.StaticMessageService
import dev.jonaz.vured.bot.service.web.PlayerService
import dev.jonaz.vured.util.extensions.genericInject
import dev.jonaz.vured.util.extensions.ifNotTrue
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.managers.AudioManager
import java.awt.Color
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class MusicService {
    private val staticMessageService by genericInject<StaticMessageService>()
    private val guildService by genericInject<GuildService>()
    private val playerService by genericInject<PlayerService>()

    private lateinit var playerManager: DefaultAudioPlayerManager
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var sendHandler: AudioPlayerSendHandler

    private val queue: BlockingQueue<AudioTrack> = LinkedBlockingQueue()

    fun createAudioPlayer() {
        playerManager = DefaultAudioPlayerManager()
        audioPlayer = playerManager.createPlayer()
        sendHandler = AudioPlayerSendHandler(audioPlayer)

        AudioSourceManagers.registerRemoteSources(playerManager)
        audioPlayer.addListener(TrackScheduler)
    }

    fun loadItem(member: Member?, url: String) {
        if (member == null) {
            return
        }

        val audioLoadResult = AudioLoadResultManager(
            member = member
        )

        playerManager.loadItemOrdered(member.guild, url, audioLoadResult)
    }

    fun queue(track: AudioTrack) {
        startTrack(track, true).run {
            this.ifNotTrue { offerToQueue(track) }
        }

        val playingTrack = getAudioPlayer().playingTrack
        val volume = getVolume()

        staticMessageService.build(
            title = playingTrack.info.title,
            description = playingTrack.info.author,
            color = Color.decode("#2F3136"),
            volume = volume,
            audioTrack = playingTrack
        ).also { staticMessageService.set(it) }
    }

    fun getGuildAudioManager(): AudioManager? {
        return guildService.getCurrentGuild()?.audioManager
    }

    fun setVolume(value: Int) {
        audioPlayer.volume = value
    }

    fun getVolume(): Int {
        return audioPlayer.volume
    }

    fun stopTrack() {
        audioPlayer.stopTrack()
    }

    fun offerToQueue(track: AudioTrack) {
        queue.offer(track)
    }

    fun pollQueue(): AudioTrack? {
        return queue.poll()
    }

    fun clearQueue() {
        queue.clear()
    }

    fun startTrack(track: AudioTrack, noInterrupt: Boolean): Boolean {
        return audioPlayer.startTrack(track, noInterrupt)
    }

    fun nextTrack() {
        pollQueue()?.let { startTrack(it, false) }
    }

    fun setPause() = true.also { audioPlayer.isPaused = it }

    fun setResume() = false.also { audioPlayer.isPaused = it }

    fun getQueue() = queue

    fun getSendHandler() = sendHandler

    fun getAudioPlayer() = audioPlayer
}
