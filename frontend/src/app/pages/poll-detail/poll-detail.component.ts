import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef, ViewChild, ElementRef } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PollService } from '../../services/poll.service';
import { AuthService } from '../../auth/auth.service';
import { PollResponse, ResultsResponse, QuestionResult } from '../../models/poll.model';import { interval, Subscription } from 'rxjs';

@Component({
  selector: 'app-poll-detail',
  standalone: true,
  imports: [RouterLink, DatePipe],
  templateUrl: './poll-detail.component.html',
  styleUrls: ['./poll-detail.component.scss']
})
export class PollDetailComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private pollService = inject(PollService);
  private cdr = inject(ChangeDetectorRef);
  public authService = inject(AuthService);

  @ViewChild('deleteDialog') deleteDialogRef?: ElementRef<HTMLDialogElement>;
  poll: PollResponse | null = null;
  results: ResultsResponse | null = null;
  canSeeResults = false;
  loadError = false;
  messages: Map<number, string> = new Map();
  errors: Map<number, boolean> = new Map();
  private refreshSub?: Subscription;
  pollId = 0;

  // questions dont le vote est en cours d'envoi, pour desactiver leurs boutons
  votingQuestionIds = new Set<number>();

  // etat de la boite de confirmation de suppression
  deleting = false;
  deleteErrorMsg = '';

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

      error: () => {
        this.loadError = true;
        this.cdr.markForCheck();
      }

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
    this.votingQuestionIds.add(questionId);
    this.cdr.markForCheck();

    this.pollService.vote(questionId, optionId).subscribe({
      next: () => {
        this.messages.set(questionId, 'Vote enregistre');
        this.errors.set(questionId, false);
        this.canSeeResults = true;
        this.votingQuestionIds.delete(questionId);
        this.loadResults();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.errors.set(questionId, true);
        this.votingQuestionIds.delete(questionId);
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

  // vrai si l'utilisateur courant est le createur du sondage
  isOwner(): boolean {
    return !!this.poll && this.poll.createdBy === this.authService.getUsername();
  }

  // recupere le resultat d'une question precise dans la reponse globale
  getQuestionResult(questionId: number): QuestionResult | undefined {
    return this.results?.questions.find(q => q.questionId === questionId);
  }

  openDeleteDialog(): void {
    this.deleteErrorMsg = '';
    this.deleteDialogRef?.nativeElement.showModal();
  }

  closeDeleteDialog(): void {
    this.deleteDialogRef?.nativeElement.close();
  }

  deletePoll(): void {
    this.deleting = true;
    this.deleteErrorMsg = '';
    this.cdr.markForCheck();

    this.pollService.deletePoll(this.pollId).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => {
        console.error('Erreur suppression', err);
        this.deleting = false;
        this.deleteErrorMsg = err.status === 403
          ? "Vous n'etes pas le proprietaire de ce sondage."
          : 'Erreur lors de la suppression.';
        this.cdr.markForCheck();
      }
    });
  }

  calcPercent(votes: number, total: number): number {
    if (total === 0) return 0;
    return Math.round((votes / total) * 100);
  }
}
