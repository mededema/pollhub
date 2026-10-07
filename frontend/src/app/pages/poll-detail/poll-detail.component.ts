import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
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
  private pollService = inject(PollService);
  public authService = inject(AuthService);

  poll: PollResponse | null = null;
  results: ResultsResponse | null = null;
  selectedOptionId: number | null = null;
  message = '';
  isError = false;
  private refreshSub?: Subscription;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadPoll(id);
      this.loadResults(id);

      // Rafraîchissement automatique des votes toutes les 3 secondes (temps réel)
      this.refreshSub = interval(3000).subscribe(() => this.loadResults(id));
    }
  }

  ngOnDestroy(): void {
    this.refreshSub?.unsubscribe();
  }

  loadPoll(id: number): void {
    this.pollService.getPoll(id).subscribe(p => this.poll = p);
  }

  loadResults(id: number): void {
    this.pollService.getResults(id).subscribe(r => this.results = r);
  }

  vote(optionId: number): void {
    if (!this.authService.isLoggedIn()) {
      this.authService.login();
      return;
    }

    if (!this.poll) return;

    this.pollService.vote(this.poll.id, optionId).subscribe({
      next: () => {
        this.message = '✅ Votre vote a bien été enregistré !';
        this.isError = false;
        this.loadResults(this.poll!.id);
      },
      error: (err) => {
        this.isError = true;
        if (err.status === 409) {
          this.message = '⚠️ Vous avez déjà voté pour ce sondage.';
        } else if (err.status === 429) {
          this.message = '⏳ Vous votez trop vite. Ralentissez !';
        } else {
          this.message = '❌ Erreur lors du vote.';
        }
      }
    });
  }

  calcPercent(votes: number): number {
    if (!this.results || this.results.totalVotes === 0) return 0;
    return Math.round((votes / this.results.totalVotes) * 100);
  }
}
