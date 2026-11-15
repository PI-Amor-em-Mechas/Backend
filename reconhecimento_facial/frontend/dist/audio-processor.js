class PCM16Processor extends AudioWorkletProcessor {
  constructor() {
    super();
    this._targetRate = 16000;
    this._resampleRatio = sampleRate / this._targetRate;
    this._phase = 0;
    this._buffer = new Float32Array(2048);
    this._pos = 0;
  }

  _pushSample(sample) {
    this._buffer[this._pos++] = sample;
    if (this._pos >= this._buffer.length) {
      const pcm16 = new Int16Array(this._buffer.length);
      for (let index = 0; index < this._buffer.length; index += 1) {
        const value = Math.max(-1, Math.min(1, this._buffer[index]));
        pcm16[index] = value < 0 ? value * 0x8000 : value * 0x7fff;
      }
      this.port.postMessage(pcm16.buffer, [pcm16.buffer]);
      this._pos = 0;
    }
  }

  process(inputs) {
    const input = inputs[0];
    if (!input || input.length === 0) return true;
    const channelData = input[0];
    if (this._resampleRatio <= 1.0001) {
      for (let index = 0; index < channelData.length; index += 1) this._pushSample(channelData[index]);
      return true;
    }
    while (this._phase < channelData.length) {
      this._pushSample(channelData[Math.floor(this._phase)]);
      this._phase += this._resampleRatio;
    }
    this._phase -= channelData.length;
    if (this._phase < 0) this._phase = 0;
    return true;
  }
}

registerProcessor("pcm16-processor", PCM16Processor);
