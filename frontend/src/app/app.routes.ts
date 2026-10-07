import { Routes } from '@angular/router';
import { PollListComponent } from './pages/poll-list/poll-list.component';
import { PollCreateComponent } from './pages/poll-create/poll-create.component';
import { PollDetailComponent } from './pages/poll-detail/poll-detail.component';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
  { path: '', component: PollListComponent },
  { path: 'create', component: PollCreateComponent, canActivate: [authGuard] },
  { path: 'poll/:id', component: PollDetailComponent },
  { path: '**', redirectTo: '' }
];
