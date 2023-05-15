import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
@Injectable({
  providedIn: 'root'
})
export class GetresultsService {

  constructor(private _http: HttpClient) { }
  getSearchResults(query: string): Observable<any> {
    const endpoint = `http://localhost:8080/${query}`;
    return this._http.get(endpoint);
  }
  getImageSearchResults(imgPath: string): Observable<any> {
    const endpoint = `http://localhost:8080/path/${imgPath}`;
    return this._http.get(endpoint);
  }
  getAllHistory(): Observable<any> {
    const endpoint = `http://localhost:8080/history/all`;
    return this._http.get(endpoint);
  }
  addToHistory(query: string): Observable<any> {
    const endpoint = `http://localhost:8080/history/add/${query}`;
    return this._http.post(endpoint, null);
  }
}
