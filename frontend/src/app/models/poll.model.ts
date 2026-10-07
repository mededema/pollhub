export interface OptionResponse {
  id: number;
  label: string;
}

export interface QuestionResponse {
  id: number;
  text: string;
  sortOrder: number;
  options: OptionResponse[];
}

export interface PollResponse {
  id: number;
  title: string;
  description: string;
  createdAt: string | null;
  expiresAt: string | null;
  createdBy: string;
  questions: QuestionResponse[];
}

export interface QuestionRequest {
  text: string;
  options: string[];
}

export interface PollRequest {
  title: string;
  description: string;
  questions: QuestionRequest[];
  expiresAt: string | null;
}

export interface OptionResult {
  optionId: number;
  label: string;
  votes: number;
}

export interface QuestionResult {
  questionId: number;
  text: string;
  totalVotes: number;
  options: OptionResult[];
}

export interface ResultsResponse {
  pollId: number;
  title: string;
  totalVotes: number;
  questions: QuestionResult[];
}
