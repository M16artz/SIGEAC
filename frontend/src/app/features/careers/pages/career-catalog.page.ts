import {ChangeDetectionStrategy, Component, inject, OnInit, signal, ViewEncapsulation} from '@angular/core';
import {FormsModule, ReactiveFormsModule, FormBuilder, Validators} from '@angular/forms';
import {finalize, forkJoin, Observable} from 'rxjs';
import {ApiProblemError} from '../../../core/http/api-client';
import {AcademicUnit, Career, CareerApi, CatalogPage} from '../data-access/career-api.service';
@Component({
  selector: 'sec-career-catalog',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  templateUrl: './career-catalog.page.html',
  styleUrl: './career-catalog.page.scss'
})
export class CareerCatalogPage implements OnInit {
  private readonly api=inject(CareerApi); private readonly fb=inject(FormBuilder);
  readonly careers=signal<CatalogPage<Career>|null>(null); readonly units=signal<CatalogPage<AcademicUnit>|null>(null);
  readonly editingCareer=signal<Career|null>(null); readonly editingUnit=signal<AcademicUnit|null>(null); readonly history=signal<CatalogPage<Career>|null>(null);
  private historyCareerId='';
  readonly canManage=signal(false); readonly busy=signal(false); readonly error=signal(''); readonly message=signal('');
  readonly loadingCareers=signal(false); readonly loadingUnits=signal(false); readonly loadingHistory=signal(false);
  careerQuery='';unitQuery='';reason='';
  readonly unitForm=this.fb.nonNullable.group({code:['',[Validators.required,Validators.maxLength(100)]],name:['',[Validators.required,Validators.maxLength(300)]]});
  readonly careerForm=this.fb.nonNullable.group({code:['',[Validators.required,Validators.maxLength(100)]],name:['',[Validators.required,Validators.maxLength(300)]],academicUnitId:['',Validators.required],validFrom:['',Validators.required],validUntil:['']});
  ngOnInit(): void {
    this.loadingCareers.set(true);this.loadingUnits.set(true);
    forkJoin({access:this.api.access(),careers:this.api.careers(),units:this.api.units()}).pipe(finalize(()=>{this.loadingCareers.set(false);this.loadingUnits.set(false);})).subscribe({next:result=>{this.canManage.set(result.access.canManage);this.careers.set(result.careers);this.units.set(result.units);},error:error=>this.fail(error)});
  }
  loadCareers(page: number): void {
    this.loadingCareers.set(true);
    this.api.careers(page,this.careerQuery).pipe(finalize(()=>this.loadingCareers.set(false))).subscribe({next:value=>this.careers.set(value),error:error=>this.fail(error)});
  }
  loadUnits(page: number): void {
    this.loadingUnits.set(true);
    this.api.units(page,this.unitQuery).pipe(finalize(()=>this.loadingUnits.set(false))).subscribe({next:value=>this.units.set(value),error:error=>this.fail(error)});
  }
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
  loadHistory(page: number): void {
    this.loadingHistory.set(true);
    this.api.history(this.historyCareerId,page).pipe(finalize(()=>this.loadingHistory.set(false))).subscribe({next:result=>this.history.set(result),error:error=>this.fail(error)});
  }
  formatDate(value: string): string {
    const match=/^(\d{4})-(\d{2})-(\d{2})/.exec(value);
    return match ? `${match[3]}/${match[2]}/${match[1]}` : value;
  }
  unitLabel(id: string): string {
    const unit=this.units()?.items.find(item=>item.id===id);
    return unit ? `${unit.data.code} · ${unit.data.name}` : id;
  }
  private mutate(request: Observable<unknown>,message: string,reset=()=>{}): void {
    if(this.busy())return;this.busy.set(true);this.error.set('');this.message.set('');
    request.pipe(finalize(()=>this.busy.set(false))).subscribe({next:()=>{reset();this.message.set(message);this.history.set(null);this.loadCareers(this.careers()?.page??0);this.loadUnits(this.units()?.page??0);},error:error=>this.fail(error)});
  }
  private fail(error: unknown): void {this.error.set(error instanceof ApiProblemError ? `${error.message} Si hubo un cambio concurrente, recargue el catálogo antes de editar.` : 'No fue posible consultar o modificar el catálogo.');}
}
