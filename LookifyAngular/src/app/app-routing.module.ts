import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { SearchResultsComponent } from './search-results/search-results.component';
import { SpeechRecognitionComponent } from './speech-recognition/speech-recognition.component';
import { MediaComponent } from './media/media.component';

const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: "home", component: HomeComponent },
  { path: "search/:value", component: SearchResultsComponent },
  { path: "voice", component: SpeechRecognitionComponent },
  { path: "media", component: MediaComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
