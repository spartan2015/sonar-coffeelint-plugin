package org.sonar.plugins.coffeelint.model;

import java.util.ArrayList;
import java.util.List;

public class CoffeelintViolation {
    private List<Violation> violations = new ArrayList<>();

	public List<Violation> getViolations() {
		return violations;
	}

	public void setViolations(List<Violation> violations) {
		this.violations = violations;
	}
}