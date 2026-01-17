import { Component, inject, OnInit } from '@angular/core';
import { UserService } from '../../service/user.service';

@Component({
  selector: 'app-welcome',
  imports: [],
  templateUrl: './welcome.html',
  styleUrl: './welcome.scss'
})
export class Welcome implements OnInit {
  private userService = inject(UserService);

  ngOnInit(): void {
    console.log('Welcome :: ngOnInit ...');
    this.userService.profile().subscribe(profile => {
      console.log('Welcome :: ngOnInit :: User profile:', profile);
    });
  }
}
