/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.optimizer;

import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.tools.RuleSet;
import org.apache.calcite.tools.RuleSets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom rule set for query optimization. Provides a flexible way to define, combine, and manage Calcite optimization
 * rules.
 */
public class CustomRuleSet {

    private final List<RelOptRule> rules;
    private final Convention convention;

    /**
     * Create a rule set with default optimization rules.
     */
    public CustomRuleSet() {
        this.convention = Convention.NONE;
        this.rules = createDefaultRules();
    }

    /**
     * Create a rule set with default rules and specific convention.
     *
     * @param convention
     *            the calling convention for physical planning
     */
    public CustomRuleSet(Convention convention) {
        this.convention = convention;
        this.rules = createDefaultRules();
    }

    /**
     * Create a custom rule set with specific rules.
     *
     * @param rules
     *            the specific rules to include
     */
    public CustomRuleSet(List<RelOptRule> rules) {
        this.convention = Convention.NONE;
        this.rules = new ArrayList<>(rules);
    }

    /**
     * Create a custom rule set with specific rules and convention.
     *
     * @param rules
     *            the specific rules to include
     * @param convention
     *            the calling convention
     */
    public CustomRuleSet(List<RelOptRule> rules, Convention convention) {
        this.convention = convention;
        this.rules = new ArrayList<>(rules);
    }

    /**
     * Create default set of optimization rules. For now, return empty list to avoid rule conflicts. Rules can be added
     * via addRule() or addRules() methods.
     */
    private List<RelOptRule> createDefaultRules() {
        // Return empty list to avoid StackOverflowError from conflicting rules
        // Users can add specific rules via addRule() or addRules() as needed
        return new ArrayList<>();
    }

    /**
     * Add a single rule to the rule set.
     *
     * @param rule
     *            the optimization rule to add
     * @return this rule set for chaining
     */
    public CustomRuleSet addRule(RelOptRule rule) {
        if (rule != null && !rules.contains(rule)) {
            rules.add(rule);
        }
        return this;
    }

    /**
     * Add multiple rules to the rule set.
     *
     * @param rules
     *            the optimization rules to add
     * @return this rule set for chaining
     */
    public CustomRuleSet addRules(List<RelOptRule> rules) {
        if (rules != null) {
            for (RelOptRule rule : rules) {
                addRule(rule);
            }
        }
        return this;
    }

    /**
     * Get a copy of all rules in this rule set.
     *
     * @return unmodifiable list of optimization rules
     */
    public List<RelOptRule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    /**
     * Convert to Calcite RuleSet for use in FrameworkConfig.
     *
     * @return RuleSet containing all rules
     */
    public RuleSet toRuleSet() {
        return RuleSets.ofList(rules);
    }

    /**
     * Get the number of rules in this set.
     *
     * @return rule count
     */
    public int size() {
        return rules.size();
    }

    /**
     * Create a copy of this rule set.
     *
     * @return new CustomRuleSet with same rules
     */
    public CustomRuleSet copy() {
        return new CustomRuleSet(new ArrayList<>(rules), convention);
    }

    /**
     * Merge with another rule set (union).
     *
     * @param other
     *            the other rule set
     * @return new CustomRuleSet with combined rules
     */
    public CustomRuleSet merge(CustomRuleSet other) {
        List<RelOptRule> mergedRules = new ArrayList<>(this.rules);
        for (RelOptRule rule : other.rules) {
            if (!mergedRules.contains(rule)) {
                mergedRules.add(rule);
            }
        }
        return new CustomRuleSet(mergedRules, this.convention);
    }

    @Override
    public String toString() {
        return "CustomRuleSet{" + "ruleCount=" + rules.size() + ", convention=" + convention + ", rules="
                + rules.stream().map(r -> r.getClass().getSimpleName()).collect(Collectors.joining(", ")) + '}';
    }
}
