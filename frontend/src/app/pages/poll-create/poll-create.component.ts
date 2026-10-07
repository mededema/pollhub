import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PollService } from '../../services/poll.service';
import { PollRequest, QuestionRequest } from '../../models/poll.model';

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

  title = '';
  description = '';
  questions: { text: string; options: string[] }[] = [
    { text: '', options: ['', ''] }
  ];
  errorMsg = '';

  addQuestion(): void {
    this.questions.push({ text: '', options: ['', ''] });
  }

  removeQuestion(index: number): void {
    if (this.questions.length > 1) {
      this.questions.splice(index, 1);
    }
  }

  addOption(qIndex: number): void {
    if (this.questions[qIndex].options.length < 6) {
      this.questions[qIndex].options.push('');
    }
  }

  removeOption(qIndex: number, oIndex: number): void {
    if (this.questions[qIndex].options.length > 2) {
      this.questions[qIndex].options.splice(oIndex, 1);
    }
  }

  trackByIndex(index: number): number {
    return index;
  }

  submit(): void {
    if (!this.title.trim()) {
      this.errorMsg = 'Veuillez saisir un titre pour le sondage.';
      return;
    }

    const validQuestions: QuestionRequest[] = this.questions
      .filter(q => q.text.trim().length > 0)
      .map(q => ({
        text: q.text.trim(),
        options: q.options.map(o => o.trim()).filter(o => o.length > 0)
      }))
      .filter(q => q.options.length >= 2);

    if (validQuestions.length === 0) {
      this.errorMsg = 'Au moins une question avec 2 options est requise.';
      return;
    }

    const payload: PollRequest = {
      title: this.title.trim(),
      description: this.description.trim(),
      questions: validQuestions,
      expiresAt: null
    };

    this.pollService.createPoll(payload).subscribe({
      next: (created) => this.router.navigate(['/poll', created.id]),
      error: (err) => {
        this.errorMsg = err.error?.message || 'Erreur lors de la creation.';
      }
    });
  }
}
