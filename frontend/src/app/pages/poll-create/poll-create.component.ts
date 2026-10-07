import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PollService } from '../../services/poll.service';
import { PollRequest } from '../../models/poll.model';

@Component({
  selector: 'app-poll-create',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './poll-create.component.html',
  styleUrls: ['./poll-create.component.scss']
})
export class PollCreateComponent {
  private pollService = inject(PollService);
  private router = inject(Router);

  question = '';
  options: string[] = ['', ''];
  errorMsg = '';

  addOption(): void {
    if (this.options.length < 6) {
      this.options.push('');
    }
  }

  removeOption(index: number): void {
    if (this.options.length > 2) {
      this.options.splice(index, 1);
    }
  }

  trackByIndex(index: number): number {
    return index;
  }

  submit(): void {
    const validOptions = this.options.map(o => o.trim()).filter(o => o.length > 0);

    if (!this.question.trim()) {
      this.errorMsg = 'Veuillez saisir une question.';
      return;
    }
    if (validOptions.length < 2) {
      this.errorMsg = 'Au moins 2 options valides sont requises.';
      return;
    }

    const payload: PollRequest = {
      question: this.question.trim(),
      options: validOptions,
      expiresAt: null
    };

    this.pollService.createPoll(payload).subscribe({
      next: (created) => {
        this.router.navigate(['/poll', created.id]);
      },
      error: (err) => {
        this.errorMsg = err.error?.message || 'Erreur lors de la création du sondage.';
      }
    });
  }
}
