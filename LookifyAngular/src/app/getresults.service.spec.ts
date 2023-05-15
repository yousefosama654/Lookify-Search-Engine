import { TestBed } from '@angular/core/testing';

import { GetresultsService } from './getresults.service';

describe('GetresultsService', () => {
  let service: GetresultsService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(GetresultsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
