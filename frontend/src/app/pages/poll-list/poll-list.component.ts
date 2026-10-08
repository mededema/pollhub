import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PollService } from '../../services/poll.service';
import { AuthService } from '../../auth/auth.service';
import { PollResponse } from '../../models/poll.model';

@Component({
  selector: 'app-poll-list',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './poll-list.component.html',
  styleUrls: ['./poll-list.component.scss']
})
export class PollListComponent implements OnInit {
  private pollService = inject(PollService);
  private cdr = inject(ChangeDetectorRef);
  public authService = inject(AuthService);
  polls: PollResponse[] = [];
  loading = true;
  errorMsg = '';

  // tableau utilise uniquement pour afficher un nombre fixe de squelettes
  skeletonItems = [0, 1, 2, 3, 4, 5];

  ngOnInit(): void {
    this.pollService.getPolls().subscribe({
      next: (data) => {
        this.polls = data;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.errorMsg = 'Impossible de charger les sondages pour le moment.';
        this.cdr.markForCheck();
      }
    });
  }

  trackById(index: number, poll: PollResponse): number {
    return poll.id;
  }
}
