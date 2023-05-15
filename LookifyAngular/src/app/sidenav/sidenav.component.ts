import { SideNavToggle } from './../side-nav-toggle';
import { NavData } from './../nav-data';
import { Component, EventEmitter, HostListener, OnInit, Output } from '@angular/core';
import type { GoogleAutocompleteParameters } from "serpapi";
import { getJson } from "serpapi";
import { AutoCompleteService } from '../auto-complete.service';
import { animate, animation, keyframes, style, transition, trigger } from '@angular/animations'
@Component({
  selector: 'app-sidenav',
  templateUrl: './sidenav.component.html',
  styleUrls: ['./sidenav.component.scss'],
  animations: [
    trigger('fadeInOut', [
      transition(':enter', [
        style({ opacity: 0 }),
        animate('350ms',
          style({ opacity: 1 })
        )
      ]),
      transition(':leave', [
        style({ opacity: 1 }),
        animate('350ms',
          style({ opacity: 0 })
        )
      ])
    ]),
    trigger('rotate', [
      transition(':enter', [
        animate('1000ms',
          keyframes([
            style({ transform: 'rotate(0deg)', offset: '0' }),
            style({ transform: 'rotate(2turn)', offset: '1' }),
          ])
        )
      ])
    ])
  ]
})
export class SidenavComponent implements OnInit {
  @Output() onToggleSideNav = new EventEmitter<SideNavToggle>();
  collapsed: boolean = false;
  screenWidth: number = 0;
  navdata: Array<NavData> = [
    {
      routerlink: "/home", label: "Home", icon: 'fal fa-home fa-2x'
    },
    {
      routerlink: "/", label: "History", icon: 'fal fa-chart-bar'
    },
    {
      routerlink: "/media", label: "Media", icon: 'fal fa-camera'
    },
  ];
  constructor(_AutoCompleteService: AutoCompleteService) {
  }

  @HostListener('window:resize', ['$event'])
  onResize(event: any) {
    this.screenWidth = window.innerWidth;
    if (this.screenWidth <= 768) {
      this.collapsed = false;
      this.onToggleSideNav.emit({ collapsed: this.collapsed, screenWidth: this.screenWidth });
    }
  }
  ngOnInit() {
    this.screenWidth = window.innerWidth;
  }
  toggleCollapse() {
    this.collapsed = !this.collapsed;
    this.onToggleSideNav.emit({ collapsed: this.collapsed, screenWidth: this.screenWidth });
  }
  closeSidenav() {
    this.collapsed = false;
    this.onToggleSideNav.emit({ collapsed: this.collapsed, screenWidth: this.screenWidth });
  }
}
