import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AutoCompleteService {

  constructor(private http: HttpClient) { }
  getAutocompleteSuggestions(query: string): Observable<any> {
    const apiKey = '3877ca054472f4b97fcfc2e9a979596648e053cb4726880c5378d9ea4fc2acab';
    const endpoint = `https://api.serpwow.com/live/search/google/autocomplete?api_key=${apiKey}&q=${query}`;
    return this.http.get(endpoint);
  }

}
