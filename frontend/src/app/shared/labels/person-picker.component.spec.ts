import {TestBed} from '@angular/core/testing';
import {signal} from '@angular/core';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {EntityNamesService} from './entity-names.service';
import {AccessSessionService} from '../../features/access/data-access/access-session.service';
import {PersonPickerComponent} from './person-picker.component';

describe('PersonPickerComponent', () => {
  afterEach(() => TestBed.resetTestingModule());
  function setup(people: readonly unknown[] = []) {
    TestBed.configureTestingModule({imports: [PersonPickerComponent], providers: [
      {provide: EntityNamesService, useValue: {users: signal(people)}},
      {provide: AccessSessionService, useValue: {identity: signal({id: 'self-id', displayName: 'Persona autenticada'})}}
    ]});
    const fixture = TestBed.createComponent(PersonPickerComponent);
    fixture.componentRef.setInput('controlId', 'person'); fixture.detectChanges();
    return fixture;
  }
  it('shows returned names and emits unchanged internal identifiers', () => {
    const fixture = setup([{id: 'private-user-id', displayName: 'Nombre desde catálogo', username: 'persona'}]);
    const emit = vi.fn(); fixture.componentInstance.valueChange.subscribe(emit);
    const select = (fixture.nativeElement as HTMLElement).querySelector('select')!;
    expect(select.textContent).toContain('Nombre desde catálogo');
    expect(select.textContent).not.toContain('private-user-id');
    select.value = 'private-user-id'; select.dispatchEvent(new Event('change'));
    expect(emit).toHaveBeenCalledWith('private-user-id');
  });
  it('preserves an explicit reference workflow without exposing it in the ordinary picker', () => {
    const fixture = setup(); const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelector('details')?.open).toBe(false);
    fixture.componentRef.setInput('value', 'authorized-reference'); fixture.detectChanges();
    expect(root.querySelector('select')?.required).toBe(false);
    expect(root.querySelector('input')?.value).toBe('authorized-reference');
  });
});
