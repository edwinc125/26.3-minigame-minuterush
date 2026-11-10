package com.edwinc125.minuterush.task;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class MathCatalog {
	private MathCatalog() {}

	static List<TaskFactory.Selection> generate(Random random) {
		List<TaskFactory.Selection> tasks = new ArrayList<>();
		for (boolean subtract : new boolean[] {false, true}) {
			String operation = subtract ? "subtract" : "add";
			binary(tasks, "simple-" + operation + "-1d-1d", n(random, 1, 9), n(random, 1, 9), subtract);
			int a = n(random, 1, 9), b = n(random, 1, 9), c = n(random, 1, 9);
			add(tasks, "simple-" + operation + "-three", a + symbol(subtract) + b + symbol(subtract) + c,
				subtract ? a - b - c : a + b + c);
			a = n(random, 1, 9); b = n(random, 10, 99);
			binary(tasks, "simple-" + operation + "-1d-2d", a, b, subtract);
			binary(tasks, "simple-" + operation + "-2d-1d", b, a, subtract);
			binary(tasks, "simple-" + operation + "-2d-2d", n(random, 10, 99), n(random, 10, 99), subtract);
			a = n(random, 100, 999); b = n(random, 10, 99);
			binary(tasks, "easy-" + operation + "-3d-2d", a, b, subtract);
			binary(tasks, "easy-" + operation + "-2d-3d", b, a, subtract);
			binary(tasks, "easy-" + operation + "-3d-3d", n(random, 100, 999), n(random, 100, 999), subtract);
			a = n(random, 10, 99); b = n(random, 10, 19); c = n(random, 1, 5);
			add(tasks, "easy-" + operation + "-multiply", a + symbol(subtract) + "(" + b + " * " + c + ")",
				subtract ? a - b * c : a + b * c);
			add(tasks, "easy-multiply-" + operation, "(" + b + " * " + c + ")" + symbol(subtract) + a,
				subtract ? b * c - a : b * c + a);
			b = n(random, 10, 99); c = divisor(random, b);
			add(tasks, "easy-" + operation + "-divide", a + symbol(subtract) + "(" + b + " / " + c + ")",
				subtract ? a - b / c : a + b / c);
			add(tasks, "easy-divide-" + operation, "(" + b + " / " + c + ")" + symbol(subtract) + a,
				subtract ? b / c - a : b / c + a);
		}
		int a = n(random, 1, 9), b = n(random, 1, 9);
		add(tasks, "simple-multiply-1d", a + " * " + b, a * b);
		a = n(random, 10, 19); b = n(random, 1, 5);
		add(tasks, "simple-multiply-2d-1d", a + " * " + b, a * b);
		add(tasks, "simple-multiply-1d-2d", b + " * " + a, a * b);
		a = n(random, 20, 99); b = n(random, 6, 9);
		add(tasks, "easy-multiply-2d-1d", a + " * " + b, a * b);
		a = n(random, 10, 25); b = n(random, 10, 25);
		add(tasks, "easy-multiply-2d-2d", a + " * " + b, a * b);
		a = n(random, 1, 9); b = new int[] {1, 2, 4, 5, 8}[random.nextInt(5)];
		add(tasks, "simple-divide-decimal", a + " / " + b,
			BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b)));
		division(tasks, random, "simple-divide-2d-1d", 10, 99, 1, 9);
		division(tasks, random, "easy-divide-3d-1d", 100, 999, 1, 9);
		division(tasks, random, "easy-divide-3d-2d", 100, 999, 10, 99);
		a = n(random, 1, 99); b = n(random, 1, 99); int c = n(random, 1, 99);
		add(tasks, "easy-mixed-add-subtract", a + " + " + b + " - " + c, a + b - c);
		add(tasks, "easy-mixed-subtract-add", a + " - " + b + " + " + c, a - b + c);
		return tasks;
	}

	private static int n(Random random, int min, int max) {
		return min + random.nextInt(max - min + 1);
	}

	private static String symbol(boolean subtract) {
		return subtract ? " - " : " + ";
	}

	private static void binary(List<TaskFactory.Selection> tasks, String id, int a, int b, boolean subtract) {
		add(tasks, id, a + symbol(subtract) + b, subtract ? a - b : a + b);
	}

	private static int divisor(Random random, int number) {
		List<Integer> factors = new ArrayList<>();
		for (int d = 1; d <= 9; d++) if (number % d == 0) factors.add(d);
		return factors.get(random.nextInt(factors.size()));
	}

	private static void division(List<TaskFactory.Selection> tasks, Random random, String id,
			int min, int max, int minDivisor, int maxDivisor) {
		int d = n(random, minDivisor, maxDivisor);
		int quotient = n(random, (min + d - 1) / d, max / d);
		add(tasks, id, (quotient * d) + " / " + d, quotient);
	}

	private static void add(List<TaskFactory.Selection> tasks, String id, String expression, int answer) {
		add(tasks, id, expression, BigDecimal.valueOf(answer));
	}

	private static void add(List<TaskFactory.Selection> tasks, String id, String expression, BigDecimal answer) {
		tasks.add(new TaskFactory.Selection("math-" + id, new MathTask(expression, answer), 0.35, "maths"));
	}
}
