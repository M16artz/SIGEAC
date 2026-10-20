package ec.edu.unl.sec.shared.domain;

public record OptimisticLockVersion(long value) {
    public OptimisticLockVersion {
        if (value < 0) {
            throw new IllegalArgumentException("Optimistic lock version cannot be negative");
        }
    }

    public OptimisticLockVersion next() {
        return new OptimisticLockVersion(Math.addExact(value, 1));
    }
}
