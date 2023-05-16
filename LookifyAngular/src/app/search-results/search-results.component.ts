import { SearchResult } from './../search-results';
import { Component, ElementRef, OnInit, Renderer2, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { GetresultsService } from '../getresults.service';
import { observable } from 'rxjs';
declare var webkitSpeechRecognition: any;
@Component({
  selector: 'app-search-results',
  templateUrl: './search-results.component.html',
  styleUrls: ['./search-results.component.css']
})
// <reference types="webkit"/>
export class SearchResultsComponent implements OnInit {

  @ViewChild('audioPlayer') audioPlayer: any;
  searchSuggestions: Array<string> = ["electro physics", "quantum physics", "statistical physics", "micro physics"];
  searchSuggestionsMatching: Array<string> = new Array<string>();
  QueryString: string = "";
  CurrentQueryString: string = "";
  PrevQueryString: string = "";
  QuerySearchResults: any = null;
  page: number = 1;
  count: number = 0;
  tableSize: number = 10;
  tableSizes: any = [5, 10, 15, 20];
  selectedIndex = 0;
  voiceSearch: boolean = false;
  public transcript: string = '';
  takentime: number = 0;
  recognition = new webkitSpeechRecognition();
  constructor(private _route: ActivatedRoute, private _GetresultsService: GetresultsService, private router: Router) {
    this.audioPlayer = this.audioPlayer?.nativeElement;
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
    this.QueryString = this._route.snapshot.paramMap.get('value') ?? '';
    this.CurrentQueryString = this.QueryString;
    this.getresults();
  }
  onTableDataChange(event: any): void {
    this.page = event;
    this.getresults();
  }
  onTableSizeChange(event: any): void {
    this.tableSize = event.target.value;
    this.page = 1;
    this.getresults();
  }
  ngOnInit(): void {

  }
  openLink(link: string) {
    window.open(link, '_blank');
  }
  boldTitleInDescription(bolded: string[], description: string): string {
    const words = description.split(/[\s,.;=|&]+/);
    const boldedWords = words.map(word => bolded.includes(word) ? '<strong>' + word + '</strong>' : word);
    return boldedWords.join(' ');
  }
  getresults(): void {
    let starttime = new Date().getTime();
    this._GetresultsService.getSearchResults(this.QueryString).subscribe((data) => {
      this.QuerySearchResults = data;
      this.takentime = new Date().getTime() - starttime
    });
  }
  onEnter(event: any) {
    if (this.CurrentQueryString == '') {
      alert("Please enter a valid search query");



    }


    else {
      if (this.CurrentQueryString == 'cattle' || this.CurrentQueryString == 'cow') {
        this.playSound();
      }
      this.QueryString = this.CurrentQueryString;
      this.router.navigate(['/search', this.QueryString]);
      this._GetresultsService.addToHistory(this.QueryString).subscribe(() => { });
      this.getresults();


    }
  }

  playSound() {
    const audio: HTMLAudioElement = this.audioPlayer.nativeElement;
    audio.load();
    audio.play();
  }
  startRecognition() {
    // Start the recognition engine.
    this.transcript = "";
    this.recognition.start();
    this.voiceSearch = true;
  }
  stopRecognition() {
    // Stop the recognition engine.
    this.recognition.stop();
    this.voiceSearch = false;
    // don't forget to add to sugesstions db
    this.CurrentQueryString = this.transcript;
    this.QueryString = this.CurrentQueryString;
    if (this.QueryString == '') {
      alert("Please enter a valid search query");
    }
    else {
      this.router.navigate(['/search', this.transcript]);
      this._GetresultsService.addToHistory(this.transcript).subscribe(() => { });
      this.getresults();
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
      if (this.selectedIndex + 1 > this.searchSuggestionsMatching.length - 1) {
        this.QueryString = this.PrevQueryString;
        return;
      }
      else {
        this.selectedIndex = Math.min(this.selectedIndex + 1, this.searchSuggestionsMatching.length - 1);
        this.QueryString = this.searchSuggestionsMatching[this.selectedIndex];
      }
    } else if (event.key === 'ArrowUp') {
      if (this.selectedIndex - 1 < 0) {
        this.QueryString = this.PrevQueryString;
        return;
      }
      else {
        this.selectedIndex = Math.max(this.selectedIndex - 1, 0);
        this.QueryString = this.searchSuggestionsMatching[this.selectedIndex];
      }
    }
  }
  onChange(): void {
    if (this.QueryString == "") {
      this.searchSuggestionsMatching = [];
    }
    else {

      this.searchSuggestionsMatching = [];
      this.searchSuggestionsMatching.push(this.QueryString);
      this.searchSuggestions.forEach((item) => {
        if (item.includes(this.QueryString)) {
          this.searchSuggestionsMatching.push(item);
        }
      })
    }
  }
  onSubmit(choice: any) {
    if (choice == '') {
      alert("Please enter a valid search query");
    }
    else {
      // don't forget to add to sugesstions db
      this.router.navigate(['/search', choice]);
      this._GetresultsService.addToHistory(choice).subscribe(() => { });
    }
  }
}
