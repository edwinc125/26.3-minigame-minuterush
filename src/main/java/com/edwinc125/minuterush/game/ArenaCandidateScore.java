package com.edwinc125.minuterush.game;

import java.util.List;
import java.util.Optional;

record ArenaCandidateScore(int biomeDiversity, int structureCount, double landRatio, double score) {
	static final double LOW_SCORE_CUTOFF = 53.0;
	static final int POOL_SIZE = 5;
	static final int LOW_SCORE_REEVALUATION_COUNT = POOL_SIZE - 1;
	private static final int BIOME_SCORE_CAP = 24;
	private static final int STRUCTURE_SCORE_CAP = 40;

	static Optional<ArenaCandidateScore> fromSamples(int biomeDiversity, int structureCount,
			int waterSamples, int totalSamples) {
		if (biomeDiversity < 0 || structureCount < 0 || waterSamples < 0 || totalSamples < 1
				|| waterSamples > totalSamples) {
			throw new IllegalArgumentException("Invalid arena candidate survey metrics");
		}
		double landRatio = 1.0 - (double) waterSamples / totalSamples;
		if (landRatio < 0.5) {
			return Optional.empty();
		}

		double biomeScore = Math.min(1.0, (double) biomeDiversity / BIOME_SCORE_CAP);
		double structureScore = Math.min(1.0, (double) structureCount / STRUCTURE_SCORE_CAP);
		double landScore = Math.min(1.0, Math.max(0.0, (landRatio - 0.5) / 0.5));
		double score = (biomeScore + structureScore + landScore) * (100.0 / 3.0);
		return Optional.of(new ArenaCandidateScore(biomeDiversity, structureCount, landRatio, score));
	}

	boolean isLow() {
		return score < LOW_SCORE_CUTOFF;
	}

	static boolean shouldReevaluatePool(List<ArenaCandidateScore> scores) {
		return scores.size() == POOL_SIZE
			&& scores.stream().filter(ArenaCandidateScore::isLow).count() >= LOW_SCORE_REEVALUATION_COUNT;
	}
}
