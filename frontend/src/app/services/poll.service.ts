import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PollRequest, PollResponse, ResultsResponse } from '../models/poll.model';

@Injectable({
  providedIn: 'root'
})
export class PollService {
  private http = inject(HttpClient);
  private apiUrl = '/api';

  getPolls(): Observable<PollResponse[]> {
    return this.http.get<PollResponse[]>(`${this.apiUrl}/polls`);
  }

  getPoll(id: number): Observable<PollResponse> {
    return this.http.get<PollResponse>(`${this.apiUrl}/polls/${id}`);
  }

  createPoll(poll: PollRequest): Observable<PollResponse> {
    return this.http.post<PollResponse>(`${this.apiUrl}/polls`, poll);
  }

  vote(pollId: number, optionId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/votes/polls/${pollId}/options/${optionId}`, {});
  }

  getResults(pollId: number): Observable<ResultsResponse> {
    return this.http.get<ResultsResponse>(`${this.apiUrl}/polls/${pollId}/results`);
  }
}
