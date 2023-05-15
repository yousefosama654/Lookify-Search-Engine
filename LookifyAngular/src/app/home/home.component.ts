import { SearchResult } from './../search-results';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { GetresultsService } from '../getresults.service';

declare var webkitSpeechRecognition: any;
@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})

export class HomeComponent implements OnInit {
  searchSuggestions: Set<string> = new Set<string>();
  searchSuggestionsMatching: Set<string> = new Set<string>();
  QueryString: string = "";
  PrevQueryString: string = "";
  selectedIndex = 0;
  voiceSearch: boolean = false;
  public transcript: string = '';
  recognition = new webkitSpeechRecognition();
  constructor(private _router: Router, private _GetresultsService: GetresultsService) {
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
  onEnter(event: any) {
    if (this.QueryString == '') {
      alert("Please enter a valid search query");
    }
    else {
      // don't forget to add to sugesstions db
      this._router.navigate(['/search', this.QueryString]);
      this._GetresultsService.addToHistory(this.QueryString).subscribe(() => { });
    }
  }
  onSubmit(choice: any) {
    if (choice == '') {
      alert("Please enter a valid search query");
    }
    else {
      // don't forget to add to sugesstions db
      this._router.navigate(['/search', choice]);
      this._GetresultsService.addToHistory(choice).subscribe(() => { });
    }
  }
  logHoveredText(item: string) {
    this.PrevQueryString = this.QueryString;
    this.QueryString = item;
  }
  leaveHoveredText() {
    this.QueryString = this.PrevQueryString;
  }
  onKeyDown(event: KeyboardEvent) {
    this.PrevQueryString = this.QueryString;
    if (event.key === 'ArrowDown') {
      if (this.selectedIndex + 1 > this.searchSuggestionsMatching.size - 1) {
        this.QueryString = this.PrevQueryString;
        return;
      }
      else {
        this.selectedIndex = Math.min(this.selectedIndex + 1, this.searchSuggestionsMatching.size - 1);
        const myArray = Array.from(this.searchSuggestionsMatching);
        this.QueryString = myArray[this.selectedIndex];
      }
    } else if (event.key === 'ArrowUp') {
      if (this.selectedIndex - 1 < 0) {
        this.QueryString = this.PrevQueryString;
        return;
      }
      else {
        this.selectedIndex = Math.max(this.selectedIndex - 1, 0);
        const myArray = Array.from(this.searchSuggestionsMatching);
        this.QueryString = myArray[this.selectedIndex];
      }
    }
  }
  ngOnInit(): void {
    this._GetresultsService.getAllHistory().subscribe((data) => {
      this.searchSuggestions = new Set<string>(data);
    })
  }
  onChange(): void {
    if (this.QueryString == "") {
      this.searchSuggestionsMatching = new Set<string>();
    }
    else {
      this.searchSuggestionsMatching = new Set<string>();
      this.searchSuggestionsMatching.add(this.QueryString);
      this.searchSuggestions.forEach((item) => {
        if (item.includes(this.QueryString)) {
          this.searchSuggestionsMatching.add(item);
        }
      })
    }
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
    // don't forget to add to sugesstions db
    this._router.navigate(['/search', this.transcript]);
    this._GetresultsService.addToHistory(this.transcript).subscribe(() => { });
  }
}
