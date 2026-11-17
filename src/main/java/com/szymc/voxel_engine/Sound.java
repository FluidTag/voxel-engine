package com.szymc.voxel_engine;

import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_decode_filename;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.libc.LibCStdlib.free;

public class Sound {
    private int bufferId;
    private int sourceId;

    private String filePath;
    private boolean isPlaying = false;
    public Sound(String filePath, boolean loops) {
        this.filePath = filePath;

        try (MemoryStack stack = stackPush()) {
            IntBuffer channelsBuffer = stack.mallocInt(1);
            IntBuffer sampleRateBuffer = stack.mallocInt(1);

            ShortBuffer rawAudioBuffer = stb_vorbis_decode_filename(filePath, channelsBuffer, sampleRateBuffer);
            if (rawAudioBuffer == null) {
                throw new IllegalStateException("Could not load sound '" + filePath + "'");
            }

            int channels = channelsBuffer.get();
            int sampleRate = sampleRateBuffer.get();

            int format = -1;
            if (channels == 1) {
                format = AL_FORMAT_MONO16;
            } else if (channels == 2) {
                format = AL_FORMAT_STEREO16;
            }

            bufferId = alGenBuffers();
            alBufferData(bufferId, format, rawAudioBuffer, sampleRate);

            sourceId = alGenSources();
            alSourcei(sourceId, AL_BUFFER, bufferId);
            alSourcei(sourceId, AL_LOOPING, loops ? 1 : 0);
            alSourcei(sourceId, AL_POSITION, 0);
            alSourcef(sourceId, AL_GAIN, 0.3f);

            free(rawAudioBuffer);
        }
    }

    public void delete() {
        alDeleteSources(sourceId);
        alDeleteBuffers(bufferId);
    }

    public void play(float volume, float pitch) {
        int state = alGetSourcei(sourceId, AL_SOURCE_STATE);
        alSourcef(sourceId, AL_GAIN, volume);
        alSourcef(sourceId, AL_PITCH, pitch);

        if (state == AL_STOPPED) {
            isPlaying = false;
            alSourcei(sourceId, AL_POSITION, 0);
        }

        if (!isPlaying) {
            alSourcePlay(sourceId);
            isPlaying = true;
        }
    }

    public void stop() {
        if (isPlaying) {
            alSourceStop(sourceId);
            isPlaying = false;
        }
    }

    public String getFilePath() {
        return this.filePath;
    }

    public boolean isPlaying() {
        int state = alGetSourcei(sourceId, AL_SOURCE_STATE);
        if (state == AL_STOPPED) {
            isPlaying = false;
        }

        return isPlaying;
    }
}
