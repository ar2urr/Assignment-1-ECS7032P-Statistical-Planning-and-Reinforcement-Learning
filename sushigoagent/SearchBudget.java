package sushigoagent;

import players.PlayerConstants;
import java.util.function.LongSupplier;

/** Tracks simulation calls and state copies, and checks elapsed search time. */
final class SearchBudget {
    private final PlayerConstants type;
    private final long limit;
    private final long started;
    private final long usableNanos;
    private final LongSupplier clock;
    long iterations;
    long fmCalls;
    long copyCalls;
    private long longestIterationNanos;

    SearchBudget(BasicMCTSParams params, long started, LongSupplier clock) {
        this.type = params.budgetType;
        this.limit = params.budget;
        this.started = started;
        this.clock = clock;
        usableNanos = Math.max(0L, (long) params.budget - params.breakMS) * 1_000_000L;
    }

    boolean hasTime() {
        return type != PlayerConstants.BUDGET_TIME || clock.getAsLong() - started < usableNanos;
    }

    boolean canSpend(long copies, long advances) {
        if (!hasTime()) return false;
        return switch (type) {
            case BUDGET_ITERATIONS -> iterations < limit;
            case BUDGET_FM_CALLS -> fmCalls + advances <= limit;
            case BUDGET_COPY_CALLS -> copyCalls + copies <= limit;
            case BUDGET_FMANDCOPY_CALLS -> fmCalls + copyCalls + advances + copies <= limit;
            case BUDGET_TIME -> true;
        };
    }

    boolean canStartIteration() {
        if (limit == 0 || !canSpend(1, 1)) return false;
        if (type != PlayerConstants.BUDGET_TIME) return true;
        long remaining = usableNanos - (clock.getAsLong() - started);
        return remaining > 2 * longestIterationNanos;
    }

    boolean reserveCopy() {
        if (!canSpend(1, 0)) return false;
        copyCalls++;
        return true;
    }

    boolean reserveAdvance() {
        if (!canSpend(0, 1)) return false;
        fmCalls++;
        return true;
    }

    void completeIteration(long durationNanos) {
        iterations++;
        longestIterationNanos = Math.max(longestIterationNanos, durationNanos);
    }
}
