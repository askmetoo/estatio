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
package org.estatio.integtests.agreement;

import javax.inject.Inject;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.apache.isis.applib.fixturescripts.FixtureScript;
import org.apache.isis.applib.services.eventbus.AbstractDomainEvent;
import org.apache.isis.applib.services.sudo.SudoService;
import org.apache.isis.applib.services.wrapper.InvalidException;

import org.isisaddons.module.security.dom.role.ApplicationRoleRepository;
import org.isisaddons.module.security.dom.tenancy.ApplicationTenancies;
import org.isisaddons.module.security.dom.tenancy.ApplicationTenancy;

import org.estatio.dom.agreement.AgreementRoleRepository;
import org.estatio.dom.agreement.PartySubscriptions;
import org.estatio.module.party.dom.OrganisationRepository;
import org.estatio.module.party.dom.Party;
import org.estatio.module.party.dom.PartyRepository;
import org.estatio.module.party.dom.PersonRepository;
import org.estatio.dom.roles.EstatioRole;
import org.estatio.fixture.EstatioBaseLineFixture;
import org.estatio.fixture.lease.LeaseForOxfTopModel001Gb;
import org.estatio.fixture.party.OrganisationForTopModelGb;
import org.estatio.fixture.security.users.EstatioAdmin;
import org.estatio.integtests.EstatioIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

public class PartySubscriptions_IntegTest extends EstatioIntegrationTest {

    @Inject
    PartyRepository partyRepository;

    @Inject
    AgreementRoleRepository agreementRoleRepository;

    @Inject
    ApplicationTenancies applicationTenancies;

    @Inject
    PersonRepository personRepository;

    @Inject
    OrganisationRepository organisationRepository;

    @Inject
    PartySubscriptions partySubscriptions;

    public static class OnPartyRemove extends PartySubscriptions_IntegTest {

        Party oldParty;
        Party newParty;

        @Rule
        public ExpectedException expectedException = ExpectedException.none();

        @Before
        public void setUpData() throws Exception {
            runFixtureScript(new FixtureScript() {
                @Override
                protected void execute(ExecutionContext executionContext) {
                    executionContext.executeChild(this, new EstatioBaseLineFixture());
                    executionContext.executeChild(this, new LeaseForOxfTopModel001Gb());
                }
            });

        }

        @Inject ApplicationRoleRepository applicationRoleRepository;

        @Before
        public void setUp() throws Exception {

            assertThat(applicationRoleRepository.allRoles().stream().map(x -> x.getName())).contains("estatio-superuser");



            oldParty = partyRepository.findPartyByReference(OrganisationForTopModelGb.REF);
            // EST-467: shouldn't be using global here.
            ApplicationTenancy applicationTenancy = applicationTenancies.findTenancyByPath("/");
            newParty = organisationRepository.newOrganisation("TEST", false, "Test", applicationTenancy);
        }

        @Test
        public void invalidBecauseNoReplacement() throws Exception {
            // when
            Party.DeleteEvent event = new Party.DeleteEvent();
            event.setSource(oldParty);
            event.setArguments(Lists.newArrayList());
            event.setEventPhase(AbstractDomainEvent.Phase.VALIDATE);
            partySubscriptions.on(event);

            // then
            assertThat(event.isInvalid()).isTrue();
        }

        @Test
        public void executingReplacesParty() throws Exception {
            // when
            Party.DeleteEvent event = new Party.DeleteEvent();
            event.setSource(oldParty);
            event.setArguments(Lists.newArrayList(newParty));
            event.setEventPhase(AbstractDomainEvent.Phase.VALIDATE);
            partySubscriptions.on(event);
            event.setEventPhase(AbstractDomainEvent.Phase.EXECUTING);
            partySubscriptions.on(event);

            // then
            assertThat(agreementRoleRepository.findByParty(oldParty).size()).isEqualTo(0);
            assertThat(agreementRoleRepository.findByParty(newParty).size()).isEqualTo(1);
        }

        @Test
        public void whenVetoingSubscriber() {
            // then
            expectedException.expect(InvalidException.class);

            // WHen
            sudoService.sudo(EstatioAdmin.USER_NAME, Lists.newArrayList(EstatioRole.SUPERUSER.getRoleName()),
                    new Runnable() {
                        @Override public void run() {
                            wrap(oldParty).delete(null);
                        }
                    });
            // when
        }
    }

    @Inject SudoService sudoService;

}
