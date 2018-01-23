/*
 * This file is part of the PSL software.
 * Copyright 2011-2015 University of Maryland
 * Copyright 2013-2017 The Regents of the University of California
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.linqs.psl.application.learning.weight.search.initialweight;

import org.linqs.psl.application.learning.weight.search.GridSearch;
import org.linqs.psl.application.learning.weight.WeightLearningApplication;
import org.linqs.psl.config.ConfigBundle;
import org.linqs.psl.database.Database;
import org.linqs.psl.model.Model;
import org.linqs.psl.model.rule.Rule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * GridSearch over the initial weights and then run weight learning like normal.
 *
 * TODO(eriq): It would be great if we could construct the internal WLA on our side instead
 * of relying on it to be passed in (we have to ensure that the caller uses the same rules and DBs).
 */
public class InitialWeightGridSearch extends GridSearch {
	private static final Logger log = LoggerFactory.getLogger(InitialWeightGridSearch.class);

	/**
	 * The weight lerning application that we will invoke at each location.
	 */
	private WeightLearningApplication internalWLA;

	public InitialWeightGridSearch(Model model, WeightLearningApplication internalWLA, Database rvDB, Database observedDB, ConfigBundle config) {
		this(model.getRules(), internalWLA, rvDB, observedDB, config);
	}

	/**
	 * The WeightLearningApplication should not have had initGroundModel() called yet.
	 */
	public InitialWeightGridSearch(List<Rule> rules, WeightLearningApplication internalWLA, Database rvDB, Database observedDB, ConfigBundle config) {
		super(rules, rvDB, observedDB, config);

		this.internalWLA = internalWLA;
	}

	@Override
	protected void initGroundModel() {
		super.initGroundModel();

		// Init the internal WLA.
		internalWLA.initGroundModel(
			this.reasoner,
			this.groundRuleStore,
			this.termStore,
			this.termGenerator,
			this.atomManager,
			this.trainingMap
		);
	}

	@Override
	protected double inspectLocation(double[] weights) {
		// Just have the internal WLA learn and then get the loss as the score.
		internalWLA.learn();

		// Save the learned weights.
		for (int i = 0; i < mutableRules.size(); i++) {
			weights[i] = mutableRules.get(i).getWeight();
		}

		return internalWLA.computeLoss();
	}

	@Override
	public void close() {
		super.close();
		internalWLA.close();
	}
}