/*
 *
 *  Copyright 2012-2014 Eurocommercial Properties NV
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the
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
package org.estatio.module.turnover.integtests;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import org.apache.isis.applib.fixturescripts.FixtureScript;
import org.apache.isis.applib.services.registry.ServiceRegistry2;

import org.estatio.module.currency.dom.Currency;
import org.estatio.module.currency.fixtures.enums.Currency_enum;
import org.estatio.module.lease.dom.occupancy.Occupancy;
import org.estatio.module.lease.fixtures.lease.enums.Lease_enum;
import org.estatio.module.turnover.dom.Frequency;
import org.estatio.module.turnover.dom.Type;
import org.estatio.module.turnover.dom.aggregate.AggregationPeriod;
import org.estatio.module.turnover.dom.aggregate.TurnoverAggregateForPeriod;
import org.estatio.module.turnover.dom.aggregate.TurnoverAggregateToDate;
import org.estatio.module.turnover.dom.aggregate.TurnoverAggregateToDateRepository;
import org.estatio.module.turnover.dom.aggregate.TurnoverAggregation;
import org.estatio.module.turnover.dom.aggregate.TurnoverAggregationRepository;

import static org.assertj.core.api.Assertions.assertThat;

public class TurnoverAggregateToDateRepository_IntegTest extends TurnoverModuleIntegTestAbstract {

    @Before
    public void setupData() {
        runFixtureScript(new FixtureScript() {
            @Override
            protected void execute(ExecutionContext executionContext) {
                executionContext.executeChild(this, Currency_enum.EUR.builder());
                executionContext.executeChild(this, Lease_enum.OxfTopModel001Gb.builder());
            }
        });
    }

    @Test
    public void find_or_create_works() throws Exception {

        // given
        final Occupancy occupancy = Lease_enum.OxfTopModel001Gb.findUsing(serviceRegistry2).getOccupancies().first();
        final LocalDate date = new LocalDate(2019, 1, 1);
        final Type type = Type.PRELIMINARY;
        final Frequency frequency = Frequency.MONTHLY;
        final Currency euro = Currency_enum.EUR.findUsing(serviceRegistry2);
        TurnoverAggregation aggregation = turnoverAggregationRepository
                .findOrCreate(occupancy, date, type, frequency, euro);

        // when
        final TurnoverAggregateToDate aggregate = turnoverAggregateToDateRepository
                .findOrCreate(aggregation);

        // then
        assertThat(turnoverAggregateToDateRepository.listAll()).hasSize(1);
        assertThat(aggregate.getAggregation()).isEqualTo(aggregation);

        // and when (again)
        final TurnoverAggregateToDate aggregate2 = turnoverAggregateToDateRepository
                .findOrCreate(aggregation);

        // then still
        assertThat(turnoverAggregateToDateRepository.listAll()).hasSize(1);
        assertThat(aggregate2).isEqualTo(aggregate);

        // and when
        TurnoverAggregation aggregation2 = turnoverAggregationRepository
                .findOrCreate(occupancy, date.plusMonths(1), type, frequency, euro);
        final TurnoverAggregateToDate aggregate3 = turnoverAggregateToDateRepository
                .findOrCreate(aggregation2);

        // then
        assertThat(turnoverAggregateToDateRepository.listAll()).hasSize(2);
        assertThat(aggregate3).isNotEqualTo(aggregate);
    }

    @Inject TurnoverAggregateToDateRepository turnoverAggregateToDateRepository;

    @Inject TurnoverAggregationRepository turnoverAggregationRepository;

    @Inject ServiceRegistry2 serviceRegistry2;
    
}