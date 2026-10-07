import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PollService } from '../../services/poll.service';
import { AuthService } from '../../auth/auth.service';
import { PollResponse, ResultsResponse } from '../../models/poll.model';
import { interval, Subscription } from 'rxjs';

@Component({
  selector: 'app-poll-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './poll-detail.component.html',
  styleUrls: ['./poll-detail.component.scss']
})
export class PollDetailComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private pollService = inject(PollService);
  private cdr = inject(ChangeDetectorRef);
  public authService = inject(AuthService);

  poll: PollResponse | null = null;
  results: ResultsResponse | null = null;
  canSeeResults = false;
  messages: Map<number, string> = new Map();
  errors: Map<number, boolean> = new Map();
  private refreshSub?: Subscription;
  pollId = 0;

  ngOnInit(): void {
    this.pollId = Number(this.route.snapshot.paramMap.get('id'));
    if (this.pollId) {
      this.loadPoll();
      if (this.authService.isLoggedIn()) {
        this.loadResults();
        this.refreshSub = interval(3000).subscribe(() => this.loadResults());
      }
    }
  }

  ngOnDestroy(): void {
    this.refreshSub?.unsubscribe();
  }

  loadPoll(): void {
    this.pollService.getPoll(this.pollId).subscribe({
      next: (p) => {
        this.poll = p;
        this.cdr.markForCheck();
      },
      error: (err) => console.error('Erreur loadPoll', err)
    });
  }

  loadResults(): void {
    this.pollService.getResults(this.pollId).subscribe({
      next: (r) => {
        this.results = r;
        this.canSeeResults = true;
        this.cdr.markForCheck();
      },
      error: (err) => {
        if (err.status === 403) {
          this.canSeeResults = false;
        }
        this.cdr.markForCheck();
      }
    });
  }

  vote(questionId: number, optionId: number): void {
    this.pollService.vote(questionId, optionId).subscribe({
      next: () => {
        this.messages.set(questionId, 'Vote enregistre !');
        this.errors.set(questionId, false);
        this.canSeeResults = true;
        this.loadResults();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.errors.set(questionId, true);
        if (err.status === 409) {
          this.messages.set(questionId, 'Vous avez deja vote pour cette question.');
          this.canSeeResults = true;
          this.loadResults();
        } else if (err.status === 429) {
          this.messages.set(questionId, 'Vous votez trop vite !');
        } else {
          this.messages.set(questionId, 'Erreur lors du vote.');
        }
        this.cdr.markForCheck();
      }
    });
  }

  deletePoll(): void {
    if (confirm('Supprimer ce sondage ?')) {
      this.pollService.deletePoll(this.pollId).subscribe({
        next: () => this.router.navigate(['/']),
        error: (err) => {
          console.error('Erreur suppression', err);
          alert(err.status === 403
            ? "Vous n'etes pas le proprietaire de ce sondage."
            : 'Erreur lors de la suppression.');
        }
      });
    }
  }

  calcPercent(votes: number, total: number): number {
    if (total === 0) return 0;
    return Math.round((votes / total) * 100);
  }
}
