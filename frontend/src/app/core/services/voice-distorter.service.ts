import { Injectable } from '@angular/core';

export type VoiceFilterType = 'NORMAL' | 'ROBOT' | 'HELIUM' | 'DEEP';

export function audioBufferToWav(buffer: AudioBuffer): Blob {
  const numChannels = buffer.numberOfChannels;
  const sampleRate = buffer.sampleRate;
  const format = 1; // PCM
  const bitDepth = 16;
  const bytesPerSample = bitDepth / 8;
  const blockAlign = numChannels * bytesPerSample;

  const length = buffer.length;
  const bufferLength = 44 + length * blockAlign;
  const arrayBuffer = new ArrayBuffer(bufferLength);
  const view = new DataView(arrayBuffer);

  function writeString(offset: number, str: string) {
    for (let i = 0; i < str.length; i++) {
      view.setUint8(offset + i, str.charCodeAt(i));
    }
  }

  // RIFF header
  writeString(0, 'RIFF');
  view.setUint32(4, 36 + length * blockAlign, true);
  writeString(8, 'WAVE');

  // fmt chunk
  writeString(12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, format, true);
  view.setUint16(22, numChannels, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * blockAlign, true);
  view.setUint16(32, blockAlign, true);
  view.setUint16(34, bitDepth, true);

  // data chunk
  writeString(36, 'data');
  view.setUint32(40, length * blockAlign, true);

  // PCM data
  let offset = 44;
  for (let i = 0; i < length; i++) {
    for (let channel = 0; channel < numChannels; channel++) {
      const sample = Math.max(-1, Math.min(1, buffer.getChannelData(channel)[i]));
      const int16 = sample < 0 ? sample * 0x8000 : sample * 0x7FFF;
      view.setInt16(offset, int16, true);
      offset += 2;
    }
  }

  return new Blob([arrayBuffer], { type: 'audio/wav' });
}

function makeDistortionCurve(amount: number = 50): Float32Array {
  const k = amount;
  const nSamples = 44100;
  const curve = new Float32Array(nSamples);
  const deg = Math.PI / 180;
  for (let i = 0; i < nSamples; ++i) {
    const x = (i * 2) / nSamples - 1;
    curve[i] = ((3 + k) * x * 20 * deg) / (Math.PI + k * Math.abs(x));
  }
  return curve;
}

@Injectable({
  providedIn: 'root'
})
export class VoiceDistorterService {
  private mediaRecorder: MediaRecorder | null = null;
  private audioChunks: Blob[] = [];
  private recordingStartTime = 0;
  private currentStream: MediaStream | null = null;

  isSupported(): boolean {
    return typeof window !== 'undefined' &&
      !!navigator?.mediaDevices?.getUserMedia &&
      typeof MediaRecorder !== 'undefined';
  }

  async startRecording(): Promise<void> {
    if (!this.isSupported()) {
      throw new Error("L'enregistrement audio n'est pas pris en charge sur ce navigateur.");
    }

    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    this.currentStream = stream;
    this.audioChunks = [];
    this.recordingStartTime = Date.now();

    const mimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
      ? 'audio/webm;codecs=opus'
      : (MediaRecorder.isTypeSupported('audio/webm') ? 'audio/webm' : '');

    this.mediaRecorder = mimeType ? new MediaRecorder(stream, { mimeType }) : new MediaRecorder(stream);

    this.mediaRecorder.ondataavailable = (event: BlobEvent) => {
      if (event.data && event.data.size > 0) {
        this.audioChunks.push(event.data);
      }
    };

    this.mediaRecorder.start(200); // 200ms time slice
  }

  stopRecording(): Promise<{ blob: Blob; durationSeconds: number }> {
    return new Promise((resolve, reject) => {
      if (!this.mediaRecorder || this.mediaRecorder.state === 'inactive') {
        return reject(new Error('Aucun enregistrement en cours'));
      }

      const durationMs = Date.now() - this.recordingStartTime;
      const durationSeconds = Math.max(1, Math.min(10, Math.round(durationMs / 1000)));

      this.mediaRecorder.onstop = () => {
        const mimeType = this.mediaRecorder?.mimeType || 'audio/webm';
        const rawBlob = new Blob(this.audioChunks, { type: mimeType });
        this.cleanupStream();
        resolve({ blob: rawBlob, durationSeconds });
      };

      this.mediaRecorder.stop();
    });
  }

  cancelRecording(): void {
    if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
      try {
        this.mediaRecorder.stop();
      } catch {}
    }
    this.cleanupStream();
    this.audioChunks = [];
  }

  private cleanupStream(): void {
    if (this.currentStream) {
      this.currentStream.getTracks().forEach(track => track.stop());
      this.currentStream = null;
    }
    this.mediaRecorder = null;
  }

  async applyFilter(
    sourceBlob: Blob,
    filterType: VoiceFilterType
  ): Promise<{ processedBlob: Blob; base64Url: string }> {
    const AudioContextClass = typeof window !== 'undefined' 
      ? (window.AudioContext || (window as any).webkitAudioContext) 
      : null;

    if (!AudioContextClass || typeof OfflineAudioContext === 'undefined') {
      // Fallback si Web Audio API non disponible
      const base64Url = await this.blobToBase64(sourceBlob);
      return { processedBlob: sourceBlob, base64Url };
    }

    try {
      const arrayBuffer = await sourceBlob.arrayBuffer();
      const tempCtx = new AudioContextClass();
      const decodedBuffer = await tempCtx.decodeAudioData(arrayBuffer);
      await tempCtx.close();

      const numChannels = decodedBuffer.numberOfChannels;
      const length = decodedBuffer.length;
      const sampleRate = decodedBuffer.sampleRate;

      const offlineCtx = new OfflineAudioContext(numChannels, length, sampleRate);
      const source = offlineCtx.createBufferSource();
      source.buffer = decodedBuffer;

      switch (filterType) {
        case 'ROBOT': {
          const waveShaper = offlineCtx.createWaveShaper();
          waveShaper.curve = makeDistortionCurve(60) as any;
          const bandpass = offlineCtx.createBiquadFilter();
          bandpass.type = 'bandpass';
          bandpass.frequency.value = 1000;
          bandpass.Q.value = 3.0;

          source.connect(waveShaper);
          waveShaper.connect(bandpass);
          bandpass.connect(offlineCtx.destination);
          break;
        }

        case 'HELIUM': {
          const highpass = offlineCtx.createBiquadFilter();
          highpass.type = 'highpass';
          highpass.frequency.value = 1200;

          const peak = offlineCtx.createBiquadFilter();
          peak.type = 'peaking';
          peak.frequency.value = 2200;
          peak.gain.value = 10;

          source.connect(highpass);
          highpass.connect(peak);
          peak.connect(offlineCtx.destination);
          break;
        }

        case 'DEEP': {
          const lowpass = offlineCtx.createBiquadFilter();
          lowpass.type = 'lowpass';
          lowpass.frequency.value = 450;

          const lowShelf = offlineCtx.createBiquadFilter();
          lowShelf.type = 'lowshelf';
          lowShelf.frequency.value = 200;
          lowShelf.gain.value = 12;

          source.connect(lowpass);
          lowpass.connect(lowShelf);
          lowShelf.connect(offlineCtx.destination);
          break;
        }

        case 'NORMAL':
        default:
          source.connect(offlineCtx.destination);
          break;
      }

      source.start(0);
      const renderedBuffer = await offlineCtx.startRendering();
      const wavBlob = audioBufferToWav(renderedBuffer);
      const base64Url = await this.blobToBase64(wavBlob);
      return { processedBlob: wavBlob, base64Url };
    } catch (err) {
      console.warn('Erreur lors de la transformation audio, utilisation de l\'audio brut:', err);
      const base64Url = await this.blobToBase64(sourceBlob);
      return { processedBlob: sourceBlob, base64Url };
    }
  }

  async blobToBase64(blob: Blob): Promise<string> {
    if (typeof FileReader !== 'undefined') {
      return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onloadend = () => resolve(reader.result as string);
        reader.onerror = reject;
        reader.readAsDataURL(blob);
      });
    }
    const buffer = await blob.arrayBuffer();
    let binary = '';
    const bytes = new Uint8Array(buffer);
    const len = bytes.byteLength;
    for (let i = 0; i < len; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    const base64 = typeof btoa !== 'undefined'
      ? btoa(binary)
      : ((globalThis as any).Buffer?.from(binary, 'binary').toString('base64') || '');
    return `data:${blob.type || 'application/octet-stream'};base64,${base64}`;
  }
}
