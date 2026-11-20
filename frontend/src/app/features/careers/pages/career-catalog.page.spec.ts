import {TestBed} from '@angular/core/testing';
import {of,throwError} from 'rxjs';
import {beforeEach,describe,expect,it,vi} from 'vitest';
import {CareerApi,Career,AcademicUnit} from '../data-access/career-api.service';
import {CareerCatalogPage} from './career-catalog.page';
describe('Career catalog',()=>{
  const unit: AcademicUnit={id:'unit-1',revision:1,createdBy:'actor',createdAt:'2026-01-01T00:00:00Z',status:'ACTIVE',data:{code:'U',name:'Unidad sintética'}};
  const career: Career={id:'career-1',revision:3,createdBy:'actor',createdAt:'2026-01-01T00:00:00Z',status:'ACTIVE',classificationPending:false,data:{code:'C',name:'Carrera sintética',academicUnitId:unit.id,validFrom:'2020-01-01',validUntil:null}};
  const api={access:vi.fn(),careers:vi.fn(),units:vi.fn(),getUnit:vi.fn(),history:vi.fn(),saveCareer:vi.fn(),saveUnit:vi.fn(),careerState:vi.fn(),unitState:vi.fn()};
  beforeEach(()=>{vi.resetAllMocks();api.access.mockReturnValue(of({canManage:true}));api.careers.mockReturnValue(of({items:[career],page:0,size:20,totalElements:1}));api.units.mockReturnValue(of({items:[unit],page:0,size:100,totalElements:1}));api.saveCareer.mockReturnValue(of(career));TestBed.configureTestingModule({imports:[CareerCatalogPage],providers:[{provide:CareerApi,useValue:api}]});});
  it('requires the complete career form and validates dates before mutation',()=>{
    const fixture=TestBed.createComponent(CareerCatalogPage);fixture.detectChanges();const page=fixture.componentInstance;
    page.saveCareer();expect(api.saveCareer).not.toHaveBeenCalled();
    page.careerForm.setValue({...career.data,validUntil:'2019-01-01'});page.saveCareer();expect(api.saveCareer).not.toHaveBeenCalled();expect(page.error()).toContain('fecha final');
  });
  it('preserves the current record revision when editing',()=>{
    const fixture=TestBed.createComponent(CareerCatalogPage);fixture.detectChanges();const page=fixture.componentInstance;
    page.editCareer(career);page.careerForm.controls.name.setValue('Nombre actualizado');page.saveCareer();
    expect(api.saveCareer).toHaveBeenCalledWith({...career.data,name:'Nombre actualizado'},career);expect(page.editingCareer()).toBeNull();
  });
  it('hides mutation forms for a read-only actor',()=>{
    api.access.mockReturnValue(of({canManage:false}));const fixture=TestBed.createComponent(CareerCatalogPage);fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#career-code')).toBeNull();expect(fixture.nativeElement.textContent).toContain('Carrera sintética');
  });
  it('requires a state-decision reason and retains failed edits',()=>{
    const fixture=TestBed.createComponent(CareerCatalogPage);fixture.detectChanges();const page=fixture.componentInstance;
    page.changeCareerState(career);expect(api.careerState).not.toHaveBeenCalled();
    page.editCareer(career);api.saveCareer.mockReturnValue(throwError(()=>new Error('stale')));page.saveCareer();
    expect(page.editingCareer()).toEqual(career);expect(page.error()).not.toBe('');expect(page.busy()).toBe(false);
  });
  it('uses explicit semantic action variants without changing state restrictions',()=>{
    const fixture=TestBed.createComponent(CareerCatalogPage);fixture.detectChanges();
    const root=fixture.nativeElement as HTMLElement;
    expect(root.querySelector('[aria-label="Editar carrera C"]')?.classList.contains('sigeac-btn--warning-soft')).toBe(true);
    expect(root.querySelector('[aria-label="Consultar historial de C"]')?.classList.contains('sigeac-btn--success-soft')).toBe(true);
    const deactivate=root.querySelector<HTMLButtonElement>('[aria-label="Desactivar carrera C"]')!;
    expect(deactivate.classList.contains('sigeac-btn--danger-soft')).toBe(true);
    expect(deactivate.disabled).toBe(true);
    fixture.componentInstance.careers.update(value=>({...value!,items:[{...career,status:'DISABLED'}]}));fixture.detectChanges();
    expect(root.querySelector('[aria-label="Reactivar carrera C"]')?.classList.contains('sigeac-btn--success-soft')).toBe(true);
    expect(root.querySelector('[aria-label="Editar unidad U"]')?.classList.contains('sigeac-btn--warning-soft')).toBe(true);
  });
});
