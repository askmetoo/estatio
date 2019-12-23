/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.estatio.module.turnover;

import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Sets;

import org.apache.isis.applib.Module;
import org.apache.isis.applib.ModuleAbstract;
import org.apache.isis.applib.fixturescripts.FixtureScript;

import org.incode.module.fixturesupport.dom.scripts.TeardownFixtureAbstract;

import org.estatio.module.lease.EstatioLeaseModule;
import org.estatio.module.turnover.dom.Turnover;
import org.estatio.module.turnover.dom.TurnoverReportingConfig;
import org.estatio.module.turnover.dom.aggregate.PurchaseCountAggregateForPeriod;
import org.estatio.module.turnover.dom.aggregate.TurnoverAggregateForPeriod;
import org.estatio.module.turnover.dom.aggregate.TurnoverAggregateToDate;
import org.estatio.module.turnover.dom.aggregate.TurnoverAggregation;

@XmlRootElement(name = "module")
public final class EstatioTurnoverModule extends ModuleAbstract {

    public EstatioTurnoverModule(){}

    @Override
    public Set<Module> getDependencies() {
        return Sets.newHashSet(new EstatioLeaseModule());
    }

    @Override
    public FixtureScript getTeardownFixture() {
        return new TeardownFixtureAbstract() {
            @Override
            protected void execute(final ExecutionContext executionContext) {
                deleteFrom(Turnover.class);
                deleteFrom(TurnoverReportingConfig.class);
                deleteFrom(TurnoverAggregateForPeriod.class);
                deleteFrom(TurnoverAggregateToDate.class);
                deleteFrom(PurchaseCountAggregateForPeriod.class);
                deleteFrom(TurnoverAggregation.class);
            }
        };
    }
}
