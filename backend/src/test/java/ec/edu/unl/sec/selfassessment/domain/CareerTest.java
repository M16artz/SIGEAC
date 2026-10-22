package ec.edu.unl.sec.selfassessment.domain;
import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class CareerTest {
    @Test void normalizesCodesAndPreservesIdentityAndPreviousSnapshot() {
        var first=new Career(UUID.randomUUID(),new Career.Data("  car-01  ","Carrera sintética",UUID.randomUUID(),LocalDate.of(2020,1,1),null),Career.Status.ACTIVE,1,UUID.randomUUID(),Instant.now(),false);
        assertThat(first.data().code()).isEqualTo("CAR-01");
        var changed=first.replace(new Career.Data("CAR-01","Otro nombre",UUID.randomUUID(),LocalDate.of(2020,1,1),null));
        assertThat(changed.id()).isEqualTo(first.id()); assertThat(changed.revision()).isEqualTo(2);
        assertThat(first.data().name()).isEqualTo("Carrera sintética");
        assertThat(changed.state(Career.Status.DISABLED).status()).isEqualTo(Career.Status.DISABLED);
    }
    @Test void validatesRequiredUnitAndValidityInterval() {
        assertThatThrownBy(()->new Career.Data("C","Carrera",null,LocalDate.now(),null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new Career.Data("C","Carrera",UUID.randomUUID(),LocalDate.now(),LocalDate.now().minusDays(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new AcademicUnit.Data(" ","Unidad")).isInstanceOf(IllegalArgumentException.class);
    }
}
