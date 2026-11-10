package com.edwinc125.minuterush.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.edwinc125.minuterush.task.TaskDifficulty;
import net.minecraft.ChatFormatting;
import org.junit.jupiter.api.Test;

class TaskColorTest {
	@Test
	void mapsEveryTaskDifficultyToItsBossbarTextColor() {
		assertEquals(ChatFormatting.AQUA, MinuteRushGame.taskColor(TaskDifficulty.SIMPLE));
		assertEquals(ChatFormatting.GREEN, MinuteRushGame.taskColor(TaskDifficulty.EASY));
		assertEquals(ChatFormatting.YELLOW, MinuteRushGame.taskColor(TaskDifficulty.MEDIUM));
		assertEquals(ChatFormatting.RED, MinuteRushGame.taskColor(TaskDifficulty.HARD));
	}
}
