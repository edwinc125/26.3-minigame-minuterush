package com.edwinc125.minuterush.task;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class MathTask extends ChatTask {
	private static final Pattern DECIMAL = Pattern.compile("[+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)");
	private final String answer;

	public MathTask(String expression, BigDecimal answer) {
		super("Type the answer in chat: " + expression, ignored -> false);
		this.answer = answer.stripTrailingZeros().toPlainString();
	}

	public boolean accepts(String input) {
		if (input == null || !DECIMAL.matcher(input.trim()).matches()) {
			return false;
		}
		try {
			return new BigDecimal(input.trim()).compareTo(new BigDecimal(answer)) == 0;
		} catch (NumberFormatException ignored) {
			return false;
		}
	}

	@Override
	boolean matches(String message) {
		return accepts(message);
	}

	public String answer() {
		return answer;
	}
}
