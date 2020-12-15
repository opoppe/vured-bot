package com.vacegaming.musicbot.core.reaction

import com.vacegaming.musicbot.core.ChannelManager
import com.vacegaming.musicbot.core.music.TrackScheduler
import net.dv8tion.jda.api.entities.Member

object NextReaction {
    const val emote = "U+23ed"

    fun execute(member: Member) {
        ChannelManager.sendLog("Next", member = member)
        TrackScheduler.nextTrack()
    }
}