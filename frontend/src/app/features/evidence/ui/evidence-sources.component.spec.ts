import {TestBed} from '@angular/core/testing';
import {describe, expect, it, vi} from 'vitest';
import {EvidenceSourcesComponent} from './evidence-sources.component';

describe('EvidenceSourcesComponent', () => {
  function render() {
    const fixture = TestBed.createComponent(EvidenceSourcesComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('keeps files and external references in separate collections without claiming reachability', () => {
    const fixture = render();
    fixture.componentRef.setInput('files', [{
      id: 'file-1', originalName: 'synthetic.txt', declaredMediaType: 'text/plain',
      detectedMediaType: 'text/plain', sizeBytes: 9, sha256: 'a'.repeat(64), status: 'AVAILABLE'
    }]);
    fixture.componentRef.setInput('references', [{
      id: 'ref-1', uri: 'https://synthetic.invalid/a', description: 'Declaración sintética'
    }]);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('[data-file-id="file-1"]')?.textContent).toContain('synthetic.txt');
    expect(element.querySelector('[data-reference-id="ref-1"]')?.textContent).toContain('synthetic.invalid');
    expect(element.textContent).toContain('no se comprueba su disponibilidad en red');
  });

  it('announces upload progress and emits the selected file', () => {
    const fixture = render();
    const emitted = vi.fn();
    fixture.componentInstance.upload.subscribe(emitted);
    fixture.componentRef.setInput('progress', 40);
    fixture.detectChanges();
    const progress = (fixture.nativeElement as HTMLElement).querySelector('[role="progressbar"]');
    expect(progress?.getAttribute('aria-valuenow')).toBe('40');

    const input = (fixture.nativeElement as HTMLElement).querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(['synthetic'], 'synthetic.txt', {type: 'text/plain'});
    Object.defineProperty(input, 'files', {value: [file]});
    input.dispatchEvent(new Event('change'));

    expect(emitted).toHaveBeenCalledWith(file);
  });

  it('rejects structural URI errors and emits add/update reference commands', () => {
    const fixture = render();
    const emitted = vi.fn();
    fixture.componentInstance.saveReference.subscribe(emitted);

    fixture.componentInstance.uri.set('relative/path');
    fixture.componentInstance.description.set('Descripción');
    fixture.componentInstance.submitReference();

    expect(emitted).not.toHaveBeenCalled();
    expect(fixture.componentInstance.invalid()).toBe(true);

    fixture.componentInstance.uri.set('https://synthetic.invalid/document');
    fixture.componentInstance.submitReference();
    expect(emitted).toHaveBeenCalledWith({
      id: null, uri: 'https://synthetic.invalid/document', description: 'Descripción'
    });
  });

  it('emits file and reference removals and shows safe errors', () => {
    const fixture = render();
    const fileRemoved = vi.fn(), referenceRemoved = vi.fn();
    fixture.componentInstance.removeFile.subscribe(fileRemoved);
    fixture.componentInstance.removeReference.subscribe(referenceRemoved);
    fixture.componentRef.setInput('files', [{
      id: 'file-1', originalName: 'synthetic.txt', declaredMediaType: 'text/plain',
      detectedMediaType: 'text/plain', sizeBytes: 9, sha256: 'a'.repeat(64), status: 'AVAILABLE'
    }]);
    fixture.componentRef.setInput('references', [{id: 'ref-1', uri: 'urn:synthetic:a', description: 'A'}]);
    fixture.componentRef.setInput('error', 'El tipo de archivo no está permitido.');
    fixture.detectChanges();

    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('[data-remove-file]')!.click();
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('[data-remove-reference]')!.click();

    expect(fileRemoved).toHaveBeenCalledWith('file-1');
    expect(referenceRemoved).toHaveBeenCalledWith('ref-1');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('no está permitido');
  });
});
