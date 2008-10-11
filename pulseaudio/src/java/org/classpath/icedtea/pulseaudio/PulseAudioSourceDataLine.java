/* PulseAudioSourceDataLine.java
   Copyright (C) 2008 Red Hat, Inc.

This file is part of IcedTea.

IcedTea is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License as published by
the Free Software Foundation, version 2.

IcedTea is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with IcedTea; see the file COPYING.  If not, write to
the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version.
 */

package org.classpath.icedtea.pulseaudio;

import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.AudioPermission;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class PulseAudioSourceDataLine extends PulseAudioDataLine implements
		SourceDataLine, PulseAudioPlaybackLine {

	private PulseAudioMuteControl muteControl;
	private PulseAudioVolumeControl volumeControl;
	private boolean muted;
	private float volume;

	public PulseAudioSourceDataLine(EventLoop eventLoop, AudioFormat[] formats,
			AudioFormat defaultFormat) {

		this.supportedFormats = formats;
		this.eventLoop = eventLoop;
		this.lineListeners = new ArrayList<LineListener>();
		this.defaultFormat = defaultFormat;
		this.currentFormat = defaultFormat;
		this.volume = PulseAudioVolumeControl.MAX_VOLUME;
	}

	@Override
	synchronized public void open(AudioFormat format, int bufferSize)
			throws LineUnavailableException {

		/* check for permmission to play audio */
		AudioPermission perm = new AudioPermission("play", null);
		perm.checkGuard(null);

		super.open(format, bufferSize);

		volumeControl = new PulseAudioVolumeControl(this, eventLoop);
		controls.add(volumeControl);
		muteControl = new PulseAudioMuteControl(this, volumeControl);
		controls.add(muteControl);

		PulseAudioMixer parentMixer = PulseAudioMixer.getInstance();
		parentMixer.addSourceLine(this);

	}

	@Override
	public void open(AudioFormat format) throws LineUnavailableException {
		open(format, DEFAULT_BUFFER_SIZE);
	}

	public byte[] native_setVolume(float value) {
		synchronized (eventLoop.threadLock) {
			return stream.native_setVolume(value);
		}
	}

	public boolean isMuted() {
		return muted;
	}

	public void setMuted(boolean value) {
		muted = value;
	}

	public float getVolume() {
		return this.volume;
	}

	synchronized public void setVolume(float value) {
		this.volume = value;

	}

	protected void connectLine(int bufferSize, Stream masterStream)
			throws LineUnavailableException {
		StreamBufferAttributes bufferAttributes = new StreamBufferAttributes(

		bufferSize, bufferSize / 4, bufferSize / 8,
				((bufferSize / 10) > 100 ? bufferSize / 10 : 100), 0);

		if (masterStream != null) {
			synchronized (eventLoop.threadLock) {
				stream.connectForPlayback(Stream.DEFAULT_DEVICE,
						bufferAttributes, masterStream.getStreamPointer());
			}
		} else {
			synchronized (eventLoop.threadLock) {
				stream.connectForPlayback(Stream.DEFAULT_DEVICE,
						bufferAttributes, null);
			}
		}
	}

	@Override
	public int write(byte[] data, int offset, int length) {
		// can't call write() without open()ing first, but can call write()
		// without start()ing
		synchronized (this) {
			writeInterrupted = false;
		}

		if (!isOpen) {
			throw new IllegalStateException("must call open() before write()");
		}

		int frameSize = currentFormat.getFrameSize();
		if (length % frameSize != 0) {
			throw new IllegalArgumentException(
					"amount of data to write does not represent an integral number of frames");
		}
		if (length < 0) {
			throw new IllegalArgumentException("length is negative");
		}

		if (length + offset > data.length) {
			throw new ArrayIndexOutOfBoundsException(length + offset);
		}

		int position = offset;
		int remainingLength = length;
		int availableSize = 0;

		int sizeWritten = 0;

		boolean interrupted = false;

		while (remainingLength != 0) {

			synchronized (eventLoop.threadLock) {

				do {
					if (writeInterrupted) {
						return sizeWritten;
					}

					if (availableSize == -1) {
						return sizeWritten;
					}
					availableSize = stream.getWritableSize();

					if (availableSize == 0) {
						try {
							eventLoop.threadLock.wait(100);
						} catch (InterruptedException e) {
							// ignore for now
							interrupted = true;
						}

					}

				} while (availableSize == 0);

				if (availableSize > remainingLength) {
					availableSize = remainingLength;
				}

				// only write entire frames, so round down avialableSize to
				// a
				// multiple of frameSize
				availableSize = (availableSize / frameSize) * frameSize;

				synchronized (this) {
					if (writeInterrupted) {
						return sizeWritten;
					}
					/* write a little bit of the buffer */
					stream.write(data, position, availableSize);
				}

				sizeWritten += availableSize;
				position += availableSize;
				remainingLength -= availableSize;

				framesSinceOpen += availableSize / frameSize;

			}
		}

		// all the data should have been played by now
		assert (sizeWritten == length);

		if (interrupted) {
			Thread.currentThread().interrupt();
		}

		return sizeWritten;
	}

	public int available() {
		synchronized (eventLoop.threadLock) {
			return stream.getWritableSize();
		}
	};

	public int getFramePosition() {
		return (int) framesSinceOpen;
	}

	public long getLongFramePosition() {
		return framesSinceOpen;
	}

	public long getMicrosecondPosition() {

		float frameRate = currentFormat.getFrameRate();
		float time = framesSinceOpen / frameRate; // seconds
		long microseconds = (long) (time * 1000);
		return microseconds;
	}

	@Override
	public void drain() {
		if (!isOpen) {
			throw new IllegalStateException(
					"Line must be open before it can be drain()ed");

		}

		synchronized (this) {
			writeInterrupted = true;
		}

		do {
			synchronized (this) {
				if (!isOpen) {
					return;
				}
				if (getBytesInBuffer() == 0) {
					return;
				}
				if (isStarted || !isOpen) {
					break;
				}
				try {
					this.wait(100);
				} catch (InterruptedException e) {
					return;
				}
			}
		} while (!isStarted);

		Operation operation;

		synchronized (eventLoop.threadLock) {
			operation = stream.drain();
		}

		operation.waitForCompletion();
		operation.releaseReference();

	}

	@Override
	public void flush() {
		if (!isOpen) {
			throw new IllegalStateException(
					"Line must be open before it can be flush()ed");
		}
		synchronized (this) {
			writeInterrupted = true;
		}

		Operation operation;
		synchronized (eventLoop.threadLock) {
			operation = stream.flush();
		}

		operation.waitForCompletion();
		operation.releaseReference();

	}

	@Override
	synchronized public void close() {

		/* check for permmission to play audio */
		AudioPermission perm = new AudioPermission("play", null);
		perm.checkGuard(null);

		if (!isOpen) {
			throw new IllegalStateException("not open so cant close");
		}

		writeInterrupted = true;

		PulseAudioMixer parent = PulseAudioMixer.getInstance();
		parent.removeSourceLine(this);

		super.close();
	}
	
	public javax.sound.sampled.Line.Info getLineInfo() {
		return new DataLine.Info(SourceDataLine.class, supportedFormats,
				StreamBufferAttributes.MIN_VALUE,
				StreamBufferAttributes.MAX_VALUE);
	}

}