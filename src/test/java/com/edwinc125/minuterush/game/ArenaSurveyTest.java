package com.edwinc125.minuterush.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class ArenaSurveyTest {
	@Test
	void distanceMatchesASquareBorder() {
		assertEquals(80, ArenaSurvey.distance(80, -30));
		assertEquals(80, ArenaSurvey.distance(-10, -80));
	}

	@Test
	void biomeCountsOnlyOnceInsideTheBorder() {
		ArenaSurvey<String> survey = new ArenaSurvey<>();
		survey.addBiomeSample("jungle", 300, 0);
		survey.addBiomeSample("jungle", 120, -40); // nearest sample wins
		survey.addBiomeSample("plains", 0, 0);
		survey.markBiomesScanned();
		assertFalse(survey.hasBiomeWithin("jungle"::equals, 80));
		assertTrue(survey.hasBiomeWithin("jungle"::equals, 120));
		assertTrue(survey.hasBiomeWithin("plains"::equals, 80));
		assertFalse(survey.hasBiomeWithin("desert"::equals, 500));
	}

	@Test
	void nothingCountsBeforeTheScanFinishes() {
		ArenaSurvey<String> survey = new ArenaSurvey<>();
		survey.addBiomeSample("plains", 0, 0);
		assertFalse(survey.hasBiomeWithin("plains"::equals, 500));
		assertFalse(survey.hasVillageWithin(500));
	}

	@Test
	void villageCountsOnceSearchedAndInsideTheBorder() {
		ArenaSurvey<String> survey = new ArenaSurvey<>();
		survey.setVillageOffset(-200, 150);
		assertFalse(survey.hasVillageWithin(160));
		assertTrue(survey.hasVillageWithin(200));

		ArenaSurvey<String> none = new ArenaSurvey<>();
		none.markNoVillage();
		assertFalse(none.hasVillageWithin(500));
	}
}
