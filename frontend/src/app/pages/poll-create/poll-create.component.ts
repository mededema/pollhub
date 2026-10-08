import { Component, inject, Injector, afterNextRender, ChangeDetectorRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PollService } from '../../services/poll.service';
import { PollRequest, QuestionRequest } from '../../models/poll.model';

@Component({
  selector: 'app-poll-create',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './poll-create.component.html',
  styleUrls: ['./poll-create.component.scss']
})
export class PollCreateComponent {
  private pollService = inject(PollService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  private injector = inject(Injector);

  // limites cote interface, alignees avec les contraintes imposees par le serveur
  readonly titleMaxLength = 120;
  readonly descriptionMaxLength = 250;
  readonly questionMaxLength = 200;
  readonly optionMaxLength = 100;

  title = '';
  description = '';
  questions: { text: string; options: string[] }[] = [
    { text: '', options: ['', ''] }
  ];
  errorMsg = '';
  submitting = false;
  // passe a vrai des la premiere tentative d'envoi, pour reveler les erreurs de champ
  submitted = false;

  addQuestion(): void {
    this.questions.push({ text: '', options: ['', ''] });
    const newIndex = this.questions.length - 1;
    this.focusElementById(`question-input-${newIndex}`);
  }

  removeQuestion(index: number): void {
    if (this.questions.length > 1) {
      this.questions.splice(index, 1);
    }
  }

  addOption(qIndex: number): void {
    if (this.questions[qIndex].options.length < 6) {
      this.questions[qIndex].options.push('');
      const newOptionIndex = this.questions[qIndex].options.length - 1;
      this.focusElementById(`option-input-${qIndex}-${newOptionIndex}`);
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

  // nombre d'options non vides, utilise pour les messages d'aide sous chaque question
  filledOptionsCount(q: { text: string; options: string[] }): number {
    return q.options.filter(o => o.trim().length > 0).length;
  }

  // avertit quand une question a du texte mais pas assez d'options pour etre retenue a l'envoi
  showQuestionWarning(q: { text: string; options: string[] }): boolean {
    return this.submitted && q.text.trim().length > 0 && this.filledOptionsCount(q) < 2;
  }

  submit(): void {
    // évite un double envoi si on clique plusieurs fois
    if (this.submitting) return;

    this.submitted = true;

    if (!this.title.trim()) {
      this.errorMsg = 'Veuillez saisir un titre pour le sondage.';
      return;
    }

    // une question à moitié remplie bloque l'envoi plutôt que d'être ignorée en silence
    const hasIncomplete = this.questions.some(q => {
      const hasText = q.text.trim().length > 0;
      const filled = this.filledOptionsCount(q);
      return (hasText && filled < 2) || (!hasText && filled > 0);
    });
    if (hasIncomplete) {
      this.errorMsg = 'Une question est incomplète : renseignez son texte et au moins deux options, ou supprimez-la.';
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

    this.errorMsg = '';
    this.submitting = true;

    const payload: PollRequest = {
      title: this.title.trim(),
      description: this.description.trim(),
      questions: validQuestions,
      expiresAt: null
    };

    this.pollService.createPoll(payload).subscribe({
      next: (created) => this.router.navigate(['/poll', created.id]),
      error: (err) => {
        this.submitting = false;
        this.errorMsg = err.error?.message || 'Erreur lors de la création.';
        this.cdr.markForCheck();
      }
    });
  }

    // Entrée dans un champ de saisie ne doit pas publier le sondage
  onEnter(event: Event): void {
    if ((event.target as HTMLElement).tagName === 'INPUT') {
      event.preventDefault();
    }
  }


  // place le focus sur un champ juste apres son insertion dans le DOM
  private focusElementById(id: string): void {
    afterNextRender(() => {
      document.getElementById(id)?.focus();
    }, { injector: this.injector });
  }
}
