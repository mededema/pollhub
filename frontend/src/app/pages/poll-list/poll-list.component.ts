import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PollService } from '../../services/poll.service';
import { AuthService } from '../../auth/auth.service';
import { PollResponse } from '../../models/poll.model';

@Component({
  selector: 'app-poll-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './poll-list.component.html',
  styleUrls: ['./poll-list.component.scss']
})
export class PollListComponent implements OnInit {
  private pollService = inject(PollService);
  private cdr = inject(ChangeDetectorRef);
  public authService = inject(AuthService);
  polls: PollResponse[] = [];
  loading = true;

  ngOnInit(): void {
    this.pollService.getPolls().subscribe({
      next: (data) => {
        this.polls = data;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }
}
