import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PollService } from '../../services/poll.service';
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
  polls: PollResponse[] = [];
  loading = true;

  ngOnInit(): void {
    this.pollService.getPolls().subscribe({
      next: (data) => {
        this.polls = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }
}
