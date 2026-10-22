import {ChangeDetectionStrategy, Component, inject, OnInit, signal} from '@angular/core';
import {FormsModule, ReactiveFormsModule, FormBuilder, Validators} from '@angular/forms';
import {finalize, forkJoin, Observable} from 'rxjs';
import {ApiProblemError} from '../../../core/http/api-client';
import {AcademicUnit, Career, CareerApi, CatalogPage} from '../data-access/career-api.service';
@Component({selector: 'sec-career-catalog', standalone: true, imports: [FormsModule,ReactiveFormsModule], changeDetection: ChangeDetectionStrategy.OnPush, template: `
  <h2>Catálogo de carreras</h2>
  <p>Unidades académicas y carreras de su ámbito. Desactivar no elimina procesos ni historial.</p>
  @if(error()){<p role="alert">{{error()}}</p>}
  @if(message()){<p role="status">{{message()}}</p>}
  <section aria-labelledby="units-title"><h3 id="units-title">Unidades académicas</h3>
    <label for="unit-search">Buscar unidad</label><input id="unit-search" [(ngModel)]="unitQuery" maxlength="300"><button (click)="loadUnits(0)">Buscar unidades</button>
    @if(canManage()) {
      <form [formGroup]="unitForm" (ngSubmit)="saveUnit()">
        <h4>{{editingUnit() ? 'Editar unidad' : 'Nueva unidad'}}</h4>
        <label for="unit-code">Código de unidad</label><input id="unit-code" formControlName="code" required maxlength="100">
        <label for="unit-name">Nombre de unidad</label><input id="unit-name" formControlName="name" required maxlength="300">
        <button [disabled]="busy() || unitForm.invalid">Guardar unidad</button><button type="button" (click)="resetUnit()">Cancelar edición de unidad</button>
      </form>
    }
    @if(units(); as result){
      <table><caption>Unidades visibles: {{result.totalElements}}</caption><thead><tr><th scope="col">Código</th><th scope="col">Nombre</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead><tbody>
      @for(unit of result.items;track unit.id){<tr><td>{{unit.data.code}}</td><td>{{unit.data.name}}</td><td>{{unit.status}}</td><td>@if(canManage()){<button [disabled]="busy()" (click)="editUnit(unit)">Editar unidad {{unit.data.code}}</button><button [disabled]="busy()" (click)="changeUnitState(unit)">{{unit.status==='ACTIVE' ? 'Desactivar' : 'Reactivar'}} unidad {{unit.data.code}}</button>}</td></tr>}
      </tbody></table>
      <button [disabled]="busy() || result.page===0" (click)="loadUnits(result.page-1)">Unidades anteriores</button><button [disabled]="busy() || (result.page+1)*result.size>=result.totalElements" (click)="loadUnits(result.page+1)">Unidades siguientes</button>
    }
  </section>
  <section aria-labelledby="careers-title"><h3 id="careers-title">Carreras</h3>
    <label for="career-search">Buscar carrera</label><input id="career-search" [(ngModel)]="careerQuery" maxlength="300"><button (click)="loadCareers(0)">Buscar carreras</button>
    @if(canManage()) {
      <form [formGroup]="careerForm" (ngSubmit)="saveCareer()">
        <h4>{{editingCareer() ? 'Editar carrera' : 'Nueva carrera'}}</h4>
        <label for="career-code">Código de carrera</label><input id="career-code" formControlName="code" required maxlength="100">
        <label for="career-name">Nombre de carrera</label><input id="career-name" formControlName="name" required maxlength="300">
        <label for="career-unit">Unidad de la carrera</label><select id="career-unit" formControlName="academicUnitId" required><option value="">Seleccione unidad activa; puede buscarla arriba</option>@for(unit of units()?.items ?? [];track unit.id){@if(unit.status==='ACTIVE'){<option [value]="unit.id">{{unit.data.code}} · {{unit.data.name}}</option>}}</select>
        <label for="career-from">Vigente desde</label><input id="career-from" type="date" formControlName="validFrom" required>
        <label for="career-until">Vigente hasta (opcional)</label><input id="career-until" type="date" formControlName="validUntil">
        <button [disabled]="busy() || careerForm.invalid">Guardar carrera</button><button type="button" (click)="resetCareer()">Cancelar edición de carrera</button>
      </form>
      <label for="state-reason">Motivo de desactivación o reactivación</label><textarea id="state-reason" [(ngModel)]="reason" minlength="10" maxlength="4000"></textarea>
    }
    @if(careers(); as result){
      <table><caption>Carreras visibles: {{result.totalElements}}</caption><thead><tr><th scope="col">Código</th><th scope="col">Nombre</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead><tbody>
      @for(career of result.items;track career.id){<tr><td>{{career.data.code}}</td><td>{{career.data.name}} @if(career.classificationPending){(pendiente de clasificación)}</td><td>{{career.status}}</td><td>
        @if(canManage()){<button [disabled]="busy()" (click)="editCareer(career)">Editar carrera {{career.data.code}}</button><button [disabled]="busy() || reason.trim().length<10" (click)="changeCareerState(career)">{{career.status==='ACTIVE' ? 'Desactivar' : 'Reactivar'}} carrera {{career.data.code}}</button>}
        <button (click)="showHistory(career)">Historial {{career.data.code}}</button>
      </td></tr>}
      </tbody></table>
      <button [disabled]="busy() || result.page===0" (click)="loadCareers(result.page-1)">Carreras anteriores</button><button [disabled]="busy() || (result.page+1)*result.size>=result.totalElements" (click)="loadCareers(result.page+1)">Carreras siguientes</button>
    }
  </section>
  @if(history(); as rows){<section aria-label="Historial de carrera"><h3>Historial conservado</h3><ol>@for(row of rows.items;track row.revision){<li>Revisión {{row.revision}} · {{row.data.code}} · {{row.data.name}} · unidad {{row.data.academicUnitId}} · {{row.status}}</li>}</ol><button [disabled]="rows.page===0" (click)="loadHistory(rows.page-1)">Historial anterior</button><button [disabled]="(rows.page+1)*rows.size>=rows.totalElements" (click)="loadHistory(rows.page+1)">Historial siguiente</button></section>}
`})
export class CareerCatalogPage implements OnInit {
  private readonly api=inject(CareerApi); private readonly fb=inject(FormBuilder);
  readonly careers=signal<CatalogPage<Career>|null>(null); readonly units=signal<CatalogPage<AcademicUnit>|null>(null);
  readonly editingCareer=signal<Career|null>(null); readonly editingUnit=signal<AcademicUnit|null>(null); readonly history=signal<CatalogPage<Career>|null>(null);
  private historyCareerId='';
  readonly canManage=signal(false); readonly busy=signal(false); readonly error=signal(''); readonly message=signal('');
  careerQuery='';unitQuery='';reason='';
  readonly unitForm=this.fb.nonNullable.group({code:['',[Validators.required,Validators.maxLength(100)]],name:['',[Validators.required,Validators.maxLength(300)]]});
  readonly careerForm=this.fb.nonNullable.group({code:['',[Validators.required,Validators.maxLength(100)]],name:['',[Validators.required,Validators.maxLength(300)]],academicUnitId:['',Validators.required],validFrom:['',Validators.required],validUntil:['']});
  ngOnInit(): void {forkJoin({access:this.api.access(),careers:this.api.careers(),units:this.api.units()}).subscribe({next:result=>{this.canManage.set(result.access.canManage);this.careers.set(result.careers);this.units.set(result.units);},error:error=>this.fail(error)});}
  loadCareers(page: number): void {this.api.careers(page,this.careerQuery).subscribe({next:value=>this.careers.set(value),error:error=>this.fail(error)});}
  loadUnits(page: number): void {this.api.units(page,this.unitQuery).subscribe({next:value=>this.units.set(value),error:error=>this.fail(error)});}
  editUnit(value: AcademicUnit): void {this.editingUnit.set(value);this.unitForm.setValue(value.data);}
  resetUnit(): void {this.editingUnit.set(null);this.unitForm.reset();}
  editCareer(value: Career): void {
    this.editingCareer.set(value);this.careerForm.setValue({...value.data,validUntil:value.data.validUntil??''});
    if(!this.units()?.items.some(unit=>unit.id===value.data.academicUnitId))this.api.getUnit(value.data.academicUnitId).subscribe({next:unit=>{const current=this.units();if(current)this.units.set({...current,items:[...current.items,unit]});},error:error=>this.fail(error)});
  }
  resetCareer(): void {this.editingCareer.set(null);this.careerForm.reset();}
  saveUnit(): void {if(this.unitForm.invalid){this.unitForm.markAllAsTouched();return;}this.mutate(this.api.saveUnit(this.unitForm.getRawValue(),this.editingUnit()),'Unidad guardada.',()=>this.resetUnit());}
  saveCareer(): void {
    if(this.careerForm.invalid){this.careerForm.markAllAsTouched();return;}
    const data=this.careerForm.getRawValue();if(data.validUntil && data.validUntil<data.validFrom){this.error.set('La fecha final no puede ser anterior a la inicial.');return;}
    this.mutate(this.api.saveCareer({...data,validUntil:data.validUntil||null},this.editingCareer()),'Carrera guardada.',()=>this.resetCareer());
  }
  changeCareerState(value: Career): void {if(this.reason.trim().length<10){this.error.set('Indique un motivo de al menos 10 caracteres.');return;}this.mutate(this.api.careerState(value,this.reason),'Estado de carrera actualizado.');}
  changeUnitState(value: AcademicUnit): void {if(this.reason.trim().length<10){this.error.set('Indique un motivo de al menos 10 caracteres.');return;}this.mutate(this.api.unitState(value,this.reason),'Estado de unidad actualizado.');}
  showHistory(value: Career): void {this.historyCareerId=value.id;this.loadHistory(0);}
  loadHistory(page: number): void {this.api.history(this.historyCareerId,page).subscribe({next:result=>this.history.set(result),error:error=>this.fail(error)});}
  private mutate(request: Observable<unknown>,message: string,reset=()=>{}): void {
    if(this.busy())return;this.busy.set(true);this.error.set('');this.message.set('');
    request.pipe(finalize(()=>this.busy.set(false))).subscribe({next:()=>{reset();this.message.set(message);this.history.set(null);this.loadCareers(this.careers()?.page??0);this.loadUnits(this.units()?.page??0);},error:error=>this.fail(error)});
  }
  private fail(error: unknown): void {this.error.set(error instanceof ApiProblemError ? `${error.message} Si hubo un cambio concurrente, recargue el catálogo antes de editar.` : 'No fue posible consultar o modificar el catálogo.');}
}
