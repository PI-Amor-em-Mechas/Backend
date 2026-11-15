export type Profile = "default" | "admin";

export interface Employee {
  id: string;
  name: string;
  sample_count: number;
  embedding_count: number;
  consent_valid: boolean;
  voiceprint_count: number;
  voiceprint_min_samples: number;
  voiceprint_ready: boolean;
}

export interface RecognitionResult {
  status: string;
  message: string;
  guidance?: string;
  token?: string;
  name?: string;
  employee_id?: string;
  confidence?: number;
  punch_type?: string;
  image?: string;
}
