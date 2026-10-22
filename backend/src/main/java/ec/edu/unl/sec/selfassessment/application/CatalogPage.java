package ec.edu.unl.sec.selfassessment.application;
import java.util.List;
public record CatalogPage<T>(List<T> items,int page,int size,long totalElements) {
    public CatalogPage {items=List.copyOf(items);}
}
