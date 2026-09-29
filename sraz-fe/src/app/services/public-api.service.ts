import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/** Veřejné API pro stránky otevírané odkazem z e-mailu (bez přihlášení do aplikace). */

export type RegistrationStatus = 'IN' | 'OUT' | 'WAITLIST';
export type Position = 'PLAYER' | 'GOALIE';
export type MemberType = 'REGULAR' | 'SUBSTITUTE';

export interface PublicTeam {
  id: number;
  name: string;
  color: string | null;
}

export interface PublicInvitation {
  groupName: string;
  eventName: string;
  startsAt: string;
  durationMinutes: number;
  signupDeadline: string;
  eventStatus: string;
  note: string | null;
  venue: { name: string; address: string | null; mapUrl: string | null; latitude: number | null; longitude: number | null } | null;
  playerName: string;
  position: Position | null;
  myStatus: RegistrationStatus | null;
  myTeamId: number | null;
  teams: PublicTeam[];
  summary: { players: number; maxPlayers: number; goalies: number; maxGoalies: number; waitlist: number };
  roster: { name: string; status: RegistrationStatus; position: Position; teamId: number | null }[];
  signupOpen: boolean;
  expired: boolean;
}

export interface PublicGroupInvite {
  groupName: string;
  invitedBy: string | null;
  email: string;
  memberType: MemberType;
  position: Position;
}

@Injectable({ providedIn: 'root' })
export class PublicApiService {
  private http = inject(HttpClient);

  invitation(token: string): Observable<PublicInvitation> {
    return this.http.get<PublicInvitation>(`/api/public/invitations/${token}`);
  }

  respond(token: string, status: RegistrationStatus, teamId: number | null): Observable<PublicInvitation> {
    return this.http.post<PublicInvitation>(`/api/public/invitations/${token}`, { status, teamId });
  }

  groupInvite(token: string): Observable<PublicGroupInvite> {
    return this.http.get<PublicGroupInvite>(`/api/public/group-invites/${token}`);
  }

  respondGroupInvite(token: string, accept: boolean): Observable<{ status: string }> {
    return this.http.post<{ status: string }>(`/api/public/group-invites/${token}`, { accept });
  }
}

/** Chybová zpráva z odpovědi backendu ({ message }) nebo obecný text. */
export function errorMessage(error: unknown, fallback = 'Něco se nepovedlo, zkuste to prosím znovu.'): string {
  const body = (error as { error?: { message?: string } })?.error;
  return body?.message ?? fallback;
}
