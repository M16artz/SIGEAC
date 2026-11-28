import {ChangeDetectionStrategy, Component, Injectable, effect, inject, input, signal} from '@angular/core';
import {Observable, catchError, map, of, shareReplay} from 'rxjs';
import {EvidenceApi} from '../../features/evidence/data-access/evidence-api.service';
import {AccessSessionService} from '../../features/access/data-access/access-session.service';
import {RequiredSourceState} from '../../features/evidence/data-access/evidence.models';

@Injectable({providedIn: 'root'})
class EvidenceReferenceNames {
  private readonly api = inject(EvidenceApi);
  private readonly session = inject(AccessSessionService);
  private readonly cache = new Map<string, Observable<string>>();
  private readonly sources = new Map<string, Observable<readonly RequiredSourceState[]>>();
  constructor() { effect(() => {this.session.identity(); this.cache.clear(); this.sources.clear();}); }
  read(version: string, source: string): Observable<string> {
    const key = `${version}:${source}`;
    let result = this.cache.get(key);
    if (!result) {
      if (source && !this.sources.has(version)) this.sources.set(version, this.api.requiredSources(version).pipe(shareReplay({bufferSize: 1, refCount: true})));
      result = (source ? this.sources.get(version)!.pipe(map(sources => sources.find(item => item.id === source)?.name ?? 'Fuente no disponible'))
        : this.api.getVersion(version).pipe(map(resource => `${resource.body.title} (versión ${resource.body.revisionNumber})`)))
        .pipe(catchError(() => of(source ? 'Fuente no disponible' : 'Evidencia no disponible')), shareReplay({bufferSize: 1, refCount: true}));
      this.cache.set(key, result);
    }
    return result;
  }
}

/** Readable references only. HTTP identifiers never become rendered text. */
@Component({selector: 'sec-evidence-reference', standalone: true, template: '{{label()}}', changeDetection: ChangeDetectionStrategy.OnPush})
export class EvidenceReferenceComponent {
  readonly versionId = input.required<string>();
  readonly sourceId = input('');
  readonly label = signal('Consultando referencia…');
  private readonly names = inject(EvidenceReferenceNames);
  private readonly session = inject(AccessSessionService);
  constructor() {
    effect(onCleanup => {
      const identity = this.session.identity();
      if (!identity) { this.label.set('Referencia no disponible'); return; }
      this.label.set('Consultando referencia…');
      const request = this.names.read(this.versionId(), this.sourceId()).subscribe(value => this.label.set(value));
      onCleanup(() => request.unsubscribe());
    });
  }
}
