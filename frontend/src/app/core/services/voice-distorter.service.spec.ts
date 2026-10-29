import { describe, it, expect, beforeEach } from 'vitest';
import { VoiceDistorterService, audioBufferToWav } from './voice-distorter.service';

describe('VoiceDistorterService', () => {
  let service: VoiceDistorterService;

  beforeEach(() => {
    service = new VoiceDistorterService();
  });

  it('devrait être instancié', () => {
    expect(service).toBeTruthy();
  });

  it('devrait convertir un Blob en Base64', async () => {
    const textBlob = new Blob(['hello audio world'], { type: 'text/plain' });
    const base64 = await service.blobToBase64(textBlob);
    expect(base64).toContain('data:text/plain;base64,');
  });

  it('devrait générer un en-tête WAV valide via audioBufferToWav', () => {
    // Créer un mock AudioBuffer
    const mockChannelData = new Float32Array([0.0, 0.5, -0.5, 0.0]);
    const mockBuffer = {
      numberOfChannels: 1,
      sampleRate: 44100,
      length: 4,
      getChannelData: () => mockChannelData
    } as unknown as AudioBuffer;

    const wavBlob = audioBufferToWav(mockBuffer);
    expect(wavBlob.type).toBe('audio/wav');
    expect(wavBlob.size).toBe(44 + 4 * 2); // 44 bytes header + 4 samples * 2 bytes
  });

  it('devrait retourner le blob brut en fallback si Web Audio échoue', async () => {
    const rawBlob = new Blob(['raw audio payload'], { type: 'audio/webm' });
    const result = await service.applyFilter(rawBlob, 'ROBOT');
    expect(result.processedBlob).toBeDefined();
    expect(result.base64Url).toContain('data:audio/webm;base64,');
  });
});
