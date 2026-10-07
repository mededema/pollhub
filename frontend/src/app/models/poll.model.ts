export interface OptionResponse {
  id: number;
  label: string;
}

export interface PollResponse {
  id: number;
  question: string;
  createdAt: string | null;
  expiresAt: string | null;
  createdBy: string;
  options: OptionResponse[];
}

export interface PollRequest {
  question: string;
  options: string[];
  expiresAt: string | null;
}

export interface OptionResult {
  optionId: number;
  label: string;
  votes: number;
}

export interface ResultsResponse {
  pollId: number;
  question: string;
  totalVotes: number;
  results: OptionResult[];
}
