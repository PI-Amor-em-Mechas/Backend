/**
 * AudioWorklet processor: converte Float32 → Int16 (PCM16-LE)
 * com reamostragem para 16 kHz real (mesmo quando o device roda em 48 kHz).
 */
class PCM16Processor extends AudioWorkletProcessor {
  constructor() {
    super();
    this._targetRate = 16000;
    this._resampleRatio = sampleRate / this._targetRate;
    this._phase = 0;
    this._buffer = new Float32Array(8192);
    this._pos = 0;
  }

  _pushSample(sample) {
    this._buffer[this._pos++] = sample;
    if (this._pos >= this._buffer.length) {
      const pcm16 = new Int16Array(this._buffer.length);
      for (let j = 0; j < this._buffer.length; j++) {
        const s = Math.max(-1, Math.min(1, this._buffer[j]));
        pcm16[j] = s < 0 ? s * 0x8000 : s * 0x7fff;
      }
      this.port.postMessage(pcm16.buffer, [pcm16.buffer]);
      this._pos = 0;
    }
  }

  process(inputs) {
    const input = inputs[0];
    if (!input || input.length === 0) return true;

    const channelData = input[0]; // mono
    if (this._resampleRatio <= 1.0001) {
      for (let i = 0; i < channelData.length; i++) {
        this._pushSample(channelData[i]);
      }
      return true;
    }

    while (this._phase < channelData.length) {
      const idx = Math.floor(this._phase);
      this._pushSample(channelData[idx]);
      this._phase += this._resampleRatio;
    }
    this._phase -= channelData.length;

    if (this._phase < 0) {
      this._phase = 0;
    }

    return true;
  }
}

registerProcessor("pcm16-processor", PCM16Processor);
