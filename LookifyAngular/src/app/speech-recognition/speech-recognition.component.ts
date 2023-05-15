import { Component } from '@angular/core';

declare var webkitSpeechRecognition: any;
@Component({
  selector: 'app-speech-recognition',
  templateUrl: `./speech-recognition.component.html`,
  styleUrls: ['./speech-recognition.component.css'],
})
export class SpeechRecognitionComponent {
  voiceSearch: boolean = true;
  public transcript: string = '';
  recognition = new webkitSpeechRecognition();
  constructor() {
    this.recognition.continuous = true;
    this.recognition.interimResults = true;
    // This event is triggered when the recognition engine returns a result.
    this.recognition.onresult = (event: any) => {
      // Get the most recent result from the event.
      const result = event.results[event.resultIndex];
      if (result.isFinal) {
        // If the result is final, append it to the transcript.
        this.transcript += result[0].transcript;
      }
    };
  }
  startRecognition() {
    // Start the recognition engine.
    this.recognition.start();
    this.voiceSearch = true;
  }
  stopRecognition() {
    // Stop the recognition engine.
    this.recognition.stop();
    this.voiceSearch = false;
    console.log(this.transcript);
  }
}
