import {ChangeDetectionStrategy, Component, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {Observable, finalize, forkJoin} from 'rxjs';
import {ProcessActivity, ProcessApi, ProcessAssignment, ProcessTeam, ProcessView, TeamMembership} from '../data-access/process-api.service';
import {ProcessStateControls} from '../ui/process-state-controls.component';
import {AccessSessionService} from '../../access/data-access/access-session.service';

@Component({
  selector: 'sec-process-planning',
  standalone: true,
  imports: [FormsModule, RouterLink, ProcessStateControls],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <p><a routerLink="/processes">Volver a procesos</a></p>
    <h2>Autorizacion y planificacion del proceso</h2>
    @if (error()) { <p role="alert">{{error()}}</p> }
    @if (process(); as current) {
      <p><strong>{{current.code}} - {{current.name}}</strong> ({{current.state}})</p>
      <p>Modelo fijado v{{current.modelVersionNumber}}. El proceso no puede iniciar sin autorizacion competente registrada.</p>
      <sec-process-state-controls [process]="current" (changed)="process.set($event); refresh($event)" />

      <section>
        <h3>Equipos de trabajo</h3>
        @if (current.state === 'PREPARATION' || current.state === 'ACTIVE') {
          <form (ngSubmit)="createTeam()">
            <label for="team-name">Nombre del equipo</label><input id="team-name" name="teamName" [(ngModel)]="teamName" required maxlength="300">
            <label for="team-role">Rol en el proceso</label><input id="team-role" name="teamRole" [(ngModel)]="teamRole" required maxlength="100">
            <label for="team-from">Desde</label><input id="team-from" type="datetime-local" name="teamFrom" [(ngModel)]="teamFrom" required>
            <label for="team-until">Hasta</label><input id="team-until" type="datetime-local" name="teamUntil" [(ngModel)]="teamUntil" required>
            <button [disabled]="busy()">Crear equipo</button>
          </form>
        }
        <ul>@for (team of teams(); track team.id) {
          <li>{{team.name}} - {{team.processRole}} ({{team.validFrom}} a {{team.validUntil}})
            <ul>@for (member of memberships()[team.id] ?? []; track member.id) {
              <li>Miembro {{member.userId}} ({{member.validFrom}} a {{member.validUntil}})</li>
            }</ul>
          </li>
        }</ul>
        @if (current.state === 'PREPARATION' || current.state === 'ACTIVE') {
          <form (ngSubmit)="addMember()">
            <h4>Agregar integrante individual</h4>
            <label for="member-team">Equipo</label><select id="member-team" name="memberTeam" [(ngModel)]="memberTeam" required>
              <option value="">Seleccione un equipo</option>@for (team of teams(); track team.id) {<option [value]="team.id">{{team.name}}</option>}
            </select>
            <label for="member-user">Usuario (UUID)</label><input id="member-user" name="memberUser" [(ngModel)]="memberUser" required>
            <label for="member-role">Asignacion de rol</label><select id="member-role" name="memberRole" [(ngModel)]="memberRole" required>
              <option value="">Seleccione una asignacion</option>@for (a of session.identity()?.assignments ?? []; track a.id) {<option [value]="a.id">{{a.roleName}} - {{a.careerId}}</option>}
            </select>
            <label for="member-from">Desde</label><input id="member-from" type="datetime-local" name="memberFrom" [(ngModel)]="memberFrom" required>
            <label for="member-until">Hasta</label><input id="member-until" type="datetime-local" name="memberUntil" [(ngModel)]="memberUntil" required>
            <button [disabled]="busy() || !memberTeam">Agregar integrante</button>
          </form>
        }
      </section>

      <section>
        <h3>Asignaciones de responsabilidad</h3>
        @if (current.state === 'ACTIVE') {
          <form (ngSubmit)="assign()">
            <label for="assign-indicator">Indicador</label><select id="assign-indicator" name="assignIndicator" [(ngModel)]="assignIndicator" required>
              <option value="">Seleccione un indicador</option>@for (i of current.indicators; track i.id) {<option [value]="i.id">{{i.code}} - {{i.name}}</option>}
            </select>
            <label for="assign-subject">Asignar a</label><select id="assign-subject" name="assignSubject" [(ngModel)]="assignSubject">
              <option value="PERSON">Persona</option><option value="TEAM">Equipo</option>
            </select>
            @if (assignSubject === 'PERSON') {<label for="assign-identity">Identidad (UUID)</label><input id="assign-identity" name="assignIdentity" [(ngModel)]="assignIdentity" required>}
            @else {<label for="assign-team">Equipo</label><select id="assign-team" name="assignTeam" [(ngModel)]="assignTeam" required><option value="">Seleccione un equipo</option>@for (team of teams(); track team.id) {<option [value]="team.id">{{team.name}}</option>}</select>}
            <label for="assign-function">Funcion</label><select id="assign-function" name="assignFunction" [(ngModel)]="assignFunction"><option>RESPONSIBLE</option><option>REVIEWER</option><option>EVALUATOR</option></select>
            <label for="assign-from">Desde</label><input id="assign-from" type="datetime-local" name="assignFrom" [(ngModel)]="assignFrom" required>
            <label for="assign-until">Hasta</label><input id="assign-until" type="datetime-local" name="assignUntil" [(ngModel)]="assignUntil" required>
            <label for="assign-reason">Motivo</label><textarea id="assign-reason" name="assignReason" [(ngModel)]="assignReason" required maxlength="2000"></textarea>
            <button [disabled]="busy()">Asignar</button>
          </form>
        } @else {<p>Las asignaciones requieren un proceso activo.</p>}
        <ul>@for (a of assignments(); track a.id) {
          <li>{{a.function}} - indicador {{a.indicatorId}} - {{a.identityId ? 'persona ' + a.identityId : 'equipo ' + a.teamId}}</li>
        }</ul>
      </section>

      <section>
        <h3>Actividades y plazos</h3>
        @if (current.state === 'PREPARATION' || current.state === 'ACTIVE') {
          <form (ngSubmit)="saveActivity()">
            <label for="activity-title">Titulo</label><input id="activity-title" name="activityTitle" [(ngModel)]="activityTitle" required maxlength="300">
            <label for="activity-indicator">Indicador</label><select id="activity-indicator" name="activityIndicator" [(ngModel)]="activityIndicator" required>
              <option value="">Seleccione un indicador</option>@for (i of current.indicators; track i.id) {<option [value]="i.id">{{i.code}} - {{i.name}}</option>}
            </select>
            <label for="activity-assignment">Asignacion responsable</label><select id="activity-assignment" name="activityAssignment" [(ngModel)]="activityAssignment" required>
              <option value="">Seleccione una asignacion</option>@for (a of assignments(); track a.id) {<option [value]="a.id">{{a.function}} - {{a.indicatorId}}</option>}
            </select>
            <label for="activity-due">Vencimiento</label><input id="activity-due" type="datetime-local" name="activityDue" [(ngModel)]="activityDue" required>
            <label for="activity-dependencies">Dependencias (UUID separados por coma)</label><input id="activity-dependencies" name="activityDependencies" [(ngModel)]="activityDependencies">
            <label for="activity-exception">Motivo de excepcion de plazo (si vence fuera del periodo)</label><textarea id="activity-exception" name="activityException" [(ngModel)]="activityException" maxlength="4000"></textarea>
            <label for="activity-authority">Asignacion autorizadora de la excepcion</label><input id="activity-authority" name="activityAuthority" [(ngModel)]="activityAuthority">
            <label for="activity-basis">Fundamento de la excepcion</label><textarea id="activity-basis" name="activityBasis" [(ngModel)]="activityBasis" maxlength="4000"></textarea>
            <button [disabled]="busy()">{{editingActivity ? 'Actualizar actividad' : 'Crear actividad'}}</button>
            @if (editingActivity) {<button type="button" (click)="cancelEdit()">Cancelar edicion</button>}
          </form>
        }
        <ul>@for (activity of activities(); track activity.id) {
          <li>{{activity.title}} - vence {{activity.dueAt}} - revision {{activity.revision}}
            @if (activity.dependencyIds.length) {<span> - depende de {{activity.dependencyIds.join(', ')}}</span>}
            @if (activity.deadlineExceptionReason) {<span> - excepcion de plazo autorizada</span>}
            <button type="button" (click)="editActivity(activity)">Editar</button>
          </li>
        }</ul>
      </section>
    }
  `
})
export class ProcessPlanningPage implements OnInit {
  readonly process = signal<ProcessView | null>(null);
  readonly teams = signal<readonly ProcessTeam[]>([]);
  readonly assignments = signal<readonly ProcessAssignment[]>([]);
  readonly activities = signal<readonly ProcessActivity[]>([]);
  readonly memberships = signal<Record<string, readonly TeamMembership[]>>({});
  readonly error = signal('');
  readonly busy = signal(false);
  teamName = ''; teamRole = ''; teamFrom = ''; teamUntil = '';
  memberTeam = ''; memberUser = ''; memberRole = ''; memberFrom = ''; memberUntil = '';
  assignIndicator = ''; assignSubject = 'PERSON'; assignIdentity = ''; assignTeam = '';
  assignFunction = 'RESPONSIBLE'; assignFrom = ''; assignUntil = ''; assignReason = '';
  activityTitle = ''; activityIndicator = ''; activityAssignment = ''; activityDue = '';
  activityDependencies = ''; activityException = ''; activityAuthority = ''; activityBasis = '';
  editingActivity: ProcessActivity | null = null;
  private processId = '';

  constructor(private readonly api: ProcessApi, private readonly route: ActivatedRoute,
      readonly session: AccessSessionService) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('processId');
    if (!id) { this.error.set('No se indico el proceso.'); return; }
    this.processId = id;
    this.api.get(id).subscribe({next: process => this.loaded(process),
      error: () => this.error.set('No fue posible consultar el proceso.')});
  }

  refresh(process: ProcessView): void {
    forkJoin({teams: this.api.teams(process.id), assignments: this.api.assignments(process.id),
      activities: this.api.activities(process.id)}).subscribe({
      next: value => {
        this.teams.set(value.teams); this.assignments.set(value.assignments);
        this.activities.set(value.activities); this.loadMemberships(value.teams);
      },
      error: () => this.error.set('No fue posible consultar la planificacion del proceso.')
    });
  }

  createTeam(): void {
    const p = this.process();
    if (!p || !this.teamName.trim() || !this.teamRole.trim() || !this.validRange(this.teamFrom, this.teamUntil)) {
      this.error.set('Complete nombre, rol y un intervalo valido de vigencia del equipo.'); return;
    }
    this.run(this.api.createTeam(p, {name: this.teamName, processRole: this.teamRole,
      validFrom: new Date(this.teamFrom).toISOString(), validUntil: new Date(this.teamUntil).toISOString()}),
      'No fue posible crear el equipo. Verifique competencia, fechas y solapamientos.');
  }

  addMember(): void {
    const p = this.process();
    if (!p || !this.memberTeam || !this.memberUser.trim() || !this.memberRole || !this.validRange(this.memberFrom, this.memberUntil)) {
      this.error.set('Complete equipo, usuario, asignacion de rol y vigencia del integrante.'); return;
    }
    this.run(this.api.addMember(p, this.memberTeam, {userId: this.memberUser, roleAssignmentId: this.memberRole,
      validFrom: new Date(this.memberFrom).toISOString(), validUntil: new Date(this.memberUntil).toISOString()}),
      'No fue posible agregar el integrante. Verifique vigencia, equipo y competencia.');
  }

  assign(): void {
    const p = this.process();
    if (!p || !this.assignIndicator || !this.validRange(this.assignFrom, this.assignUntil) || !this.assignReason.trim()
        || (this.assignSubject === 'PERSON' ? !this.assignIdentity.trim() : !this.assignTeam)) {
      this.error.set('Complete indicador, sujeto, vigencia y motivo de la asignacion.'); return;
    }
    const command: Record<string, unknown> = {indicatorId: this.assignIndicator, function: this.assignFunction,
      validFrom: new Date(this.assignFrom).toISOString(), validTo: new Date(this.assignUntil).toISOString(),
      reason: this.assignReason};
    if (this.assignSubject === 'PERSON') command['identityId'] = this.assignIdentity;
    else command['teamId'] = this.assignTeam;
    this.run(this.api.assign(p, command), 'No fue posible asignar. Verifique que exactamente una persona o equipo quede asignado.');
  }

  saveActivity(): void {
    const p = this.process();
    if (!p || !this.activityTitle.trim() || !this.activityIndicator || !this.activityAssignment || !this.activityDue) {
      this.error.set('Complete titulo, indicador, asignacion y vencimiento de la actividad.'); return;
    }
    const command = {title: this.activityTitle, indicatorId: this.activityIndicator, requiredSourceId: null,
      assignmentId: this.activityAssignment, dueAt: new Date(this.activityDue).toISOString(),
      dependencyIds: this.activityDependencies.split(',').map(value => value.trim()).filter(value => value.length > 0),
      exceptionReason: this.activityException.trim() || null,
      exceptionAuthorityAssignmentId: this.activityAuthority.trim() || null,
      exceptionLegalBasis: this.activityBasis.trim() || null};
    const request = this.editingActivity
      ? this.api.updateActivity(p, this.editingActivity.id, command)
      : this.api.createActivity(p, command);
    this.run(request, 'No fue posible guardar la actividad. Verifique plazos, dependencias y excepciones autorizadas.');
  }

  editActivity(activity: ProcessActivity): void {
    this.editingActivity = activity; this.activityTitle = activity.title;
    this.activityIndicator = activity.indicatorId; this.activityAssignment = activity.assignmentId;
    this.activityDue = this.toLocal(activity.dueAt);
    this.activityDependencies = activity.dependencyIds.join(', ');
    this.activityException = activity.deadlineExceptionReason ?? '';
    this.activityAuthority = activity.deadlineExceptionAuthorityAssignmentId ?? '';
    this.activityBasis = activity.deadlineExceptionLegalBasis ?? '';
  }

  cancelEdit(): void {
    this.editingActivity = null; this.activityTitle = ''; this.activityIndicator = '';
    this.activityAssignment = ''; this.activityDue = ''; this.activityDependencies = '';
    this.activityException = ''; this.activityAuthority = ''; this.activityBasis = '';
  }

  private loaded(process: ProcessView): void {
    this.process.set(process);
    this.teamFrom = this.toLocal(process.periodStart + 'T00:00:00Z');
    this.teamUntil = this.toLocal(process.periodEnd + 'T23:59:00Z');
    this.memberFrom = this.teamFrom; this.memberUntil = this.teamUntil;
    this.assignFrom = this.teamFrom; this.assignUntil = this.teamUntil;
    this.refresh(process);
  }

  private loadMemberships(teams: readonly ProcessTeam[]): void {
    if (!teams.length) { this.memberships.set({}); return; }
    forkJoin(teams.map(team => this.api.memberships(team.id))).subscribe({
      next: lists => this.memberships.set(Object.fromEntries(teams.map((team, index) => [team.id, lists[index]]))),
      error: () => this.error.set('No fue posible consultar los integrantes de los equipos.')
    });
  }

  private run(request: Observable<ProcessView>, message: string): void {
    if (this.busy()) return;
    this.busy.set(true); this.error.set('');
    request.pipe(finalize(() => this.busy.set(false))).subscribe({
      next: process => { this.process.set(process); this.refresh(process); this.cancelEdit(); },
      error: () => this.error.set(message)
    });
  }

  private validRange(from: string, until: string): boolean {
    const start = new Date(from), end = new Date(until);
    return Number.isFinite(start.getTime()) && Number.isFinite(end.getTime()) && start < end;
  }

  private toLocal(value: string): string {
    const date = new Date(value);
    const offset = date.getTimezoneOffset() * 60000;
    return new Date(date.getTime() - offset).toISOString().slice(0, 16);
  }
}
