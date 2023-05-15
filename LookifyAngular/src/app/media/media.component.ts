import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { GetresultsService } from '../getresults.service';
import { Router } from '@angular/router';
@Component({
  selector: 'app-media',
  templateUrl: './media.component.html',
  styleUrls: ['./media.component.css']
})
export class MediaComponent implements OnInit {
  @ViewChild('myInput') myInput: any;

  btnWord: string = "Upload Image";
  constructor(private _GetresultsService: GetresultsService, private _router: Router) {
    this.myInput = this.myInput?.nativeElement;
  }
  ngOnInit(): void {
  }
  submitForm(): void {
    this.myInput.nativeElement.click();
  }
  async onFileSelected(event: any) {
    const selectedFile = event.target.files[0];
    let searchval;
    try {
      this.btnWord = "Loading...";
      const response = await this._GetresultsService.getImageSearchResults(selectedFile.name).toPromise();
      searchval = response.text;
      this._router.navigate(['/search', searchval]);
      this._GetresultsService.addToHistory(searchval).subscribe(() => { });
    } catch (error) {
    }
  }
}
