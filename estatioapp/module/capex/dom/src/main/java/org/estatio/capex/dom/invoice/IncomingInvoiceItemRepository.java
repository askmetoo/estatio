package org.estatio.capex.dom.invoice;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import com.google.common.collect.Lists;

import org.joda.time.LocalDate;

import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.query.QueryDefault;
import org.apache.isis.applib.services.jdosupport.IsisJdoSupport;
import org.apache.isis.applib.services.registry.ServiceRegistry2;
import org.apache.isis.applib.services.repository.RepositoryService;

import org.estatio.capex.dom.project.Project;
import org.estatio.capex.dom.util.PeriodUtil;
import org.estatio.dom.asset.FixedAsset;
import org.estatio.dom.asset.Property;
import org.estatio.dom.budgeting.budgetitem.BudgetItem;
import org.estatio.dom.charge.Charge;
import org.estatio.dom.invoice.Invoice;
import org.estatio.module.party.dom.Party;
import org.estatio.module.tax.dom.Tax;

@DomainService(
        nature = NatureOfService.DOMAIN,
        repositoryFor = IncomingInvoiceItem.class
)
public class IncomingInvoiceItemRepository {

    @Programmatic
    public IncomingInvoiceItem findByInvoiceAndChargeAndSequence(final IncomingInvoice invoice, final Charge charge, final BigInteger sequence) {
        return repositoryService.uniqueMatch(
                new QueryDefault<>(
                        IncomingInvoiceItem.class,
                        "findByInvoiceAndChargeAndSequence",
                        "invoice", invoice,
                        "charge", charge,
                        "sequence", sequence
                ));
    }

    @Programmatic
    public IncomingInvoiceItem create(
            final BigInteger sequence,
            final IncomingInvoice invoice,
            final IncomingInvoiceType incomingInvoiceType,
            final Charge charge,
            final String description,
            final BigDecimal netAmount,
            final BigDecimal vatAmount,
            final BigDecimal grossAmount,
            final Tax tax,
            final LocalDate dueDate,
            final LocalDate startDate,
            final LocalDate endDate,
            final FixedAsset<?> fixedAsset,
            final Project project,
            final BudgetItem budgetItem) {
        final IncomingInvoiceItem invoiceItem = new IncomingInvoiceItem(sequence, invoice, incomingInvoiceType, charge, description, netAmount, vatAmount, grossAmount, tax, dueDate, startDate, endDate, fixedAsset, project, budgetItem);
        serviceRegistry2.injectServicesInto(invoiceItem);
        repositoryService.persistAndFlush(invoiceItem);

        invoice.invalidateApproval();

        return invoiceItem;
    }

    @Programmatic
    public IncomingInvoiceItem upsert(
            final BigInteger sequence,
            final IncomingInvoice invoice,
            final IncomingInvoiceType incomingInvoiceType,
            final Charge charge,
            final String description,
            final BigDecimal netAmount,
            final BigDecimal vatAmount,
            final BigDecimal grossAmount,
            final Tax tax,
            final LocalDate dueDate,
            final LocalDate startDate,
            final LocalDate endDate,
            final FixedAsset<?> fixedAsset,
            final Project project,
            final BudgetItem budgetItem) {
        IncomingInvoiceItem invoiceItem = findByInvoiceAndChargeAndSequence(invoice, charge, sequence);
        if (invoiceItem == null) {
            invoiceItem = create(
                    sequence,
                    invoice,
                    incomingInvoiceType,
                    charge,
                    description,
                    netAmount,
                    vatAmount,
                    grossAmount,
                    tax,
                    dueDate,
                    startDate,
                    endDate,
                    fixedAsset,
                    project,
                    budgetItem);
        } else {
            updateInvoiceItem(
                    invoiceItem,
                    sequence,
                    description,
                    netAmount,
                    vatAmount,
                    grossAmount,
                    tax,
                    dueDate,
                    startDate,
                    endDate,
                    fixedAsset,
                    project,
                    budgetItem);
        }

        return invoiceItem;
    }

    private void updateInvoiceItem(
            final IncomingInvoiceItem invoiceItem,
            final BigInteger sequence,
            final String description,
            final BigDecimal netAmount,
            final BigDecimal vatAmount,
            final BigDecimal grossAmount,
            final Tax tax,
            final LocalDate dueDate,
            final LocalDate startDate,
            final LocalDate endDate,
            final FixedAsset<?> fixedAsset,
            final Project project,
            final BudgetItem budgetItem){
        invoiceItem.setSequence(sequence);
        invoiceItem.setDescription(description);
        invoiceItem.setNetAmount(netAmount);
        invoiceItem.setVatAmount(vatAmount);
        invoiceItem.setGrossAmount(grossAmount);
        invoiceItem.setTax(tax);
        invoiceItem.setDueDate(dueDate);
        invoiceItem.setStartDate(startDate);
        invoiceItem.setEndDate(endDate);
        invoiceItem.setFixedAsset(fixedAsset);
        invoiceItem.setProject(project);
        invoiceItem.setBudgetItem(budgetItem);
    }

    @Programmatic
    public IncomingInvoiceItem addItem(
            final IncomingInvoice incomingInvoice,
            final IncomingInvoiceType type,
            final Charge charge,
            final String description,
            final BigDecimal netAmount,
            final BigDecimal vatAmount,
            final BigDecimal grossAmount,
            final Tax tax,
            final LocalDate dueDate,
            final String period,
            final FixedAsset<?> fixedAsset,
            final Project project,
            final BudgetItem budgetItem){
        final BigInteger sequence = incomingInvoice.nextItemSequence();
        return upsert(
            sequence,
            incomingInvoice,
            type,
            charge,
            description,
            netAmount,
            vatAmount,
            grossAmount,
            tax,
            dueDate,
            PeriodUtil.yearFromPeriod(period).startDate(),
            PeriodUtil.yearFromPeriod(period).endDate(),
            fixedAsset,
            project,
            budgetItem);
    }

    @Programmatic
    public void mergeItems(final IncomingInvoiceItem sourceItem, final IncomingInvoiceItem targetItem) {
        if (sourceItem==null || targetItem==null) return;
        if (sourceItem.equals(targetItem)) return;
        targetItem.addAmounts(sourceItem.getNetAmount(), sourceItem.getVatAmount(), sourceItem.getGrossAmount());
        sourceItem.removeItem();
        targetItem.invalidateApproval();
    }

    @Programmatic
    public List<IncomingInvoiceItem> findBySeller(final Party seller) {
        return repositoryService.allMatches(
                new QueryDefault<>(
                        IncomingInvoiceItem.class,
                        "findBySeller",
                        "seller", seller
                ));
    }

    @Programmatic
    public List<IncomingInvoiceItem> findByProjectAndCharge(final Project project, final Charge charge) {
        return repositoryService.allMatches(
                new QueryDefault<>(
                        IncomingInvoiceItem.class,
                        "findByProjectAndCharge",
                        "project", project,
                        "charge", charge
                ));
    }

    @Programmatic
    public List<IncomingInvoiceItem> findByBudgetItem(final BudgetItem budgetItem) {
        return repositoryService.allMatches(
                new QueryDefault<>(
                        IncomingInvoiceItem.class,
                        "findByBudgetItem",
                        "budgetItem", budgetItem
                ));
    }

    @Programmatic
    public List<IncomingInvoiceItem> findByProject(final Project project) {
        return repositoryService.allMatches(
                new QueryDefault<>(
                        IncomingInvoiceItem.class,
                        "findByProject",
                        "project", project
                ));
    }

    @Programmatic
    public List<IncomingInvoiceItem> listAll() {
        return repositoryService.allInstances(IncomingInvoiceItem.class);
    }

    @Programmatic
    public List<IncomingInvoiceItem> invoiceItemsNotOnProjectItem(final Project project){
        List<Charge> chargesOnProject = Lists.newArrayList(project.getItems()).stream().map(x->x.getCharge()).collect(Collectors.toList());
        return findByProject(project).stream()
                .filter(x->!chargesOnProject.contains(x.getCharge()))
                .collect(Collectors.toList());
    }

    @Programmatic
    public List<IncomingInvoiceItem> findCompletedOrLaterByFixedAssetAndReportedDate(
            final Property property,
            final LocalDate reportedDate) {

        final List<IncomingInvoiceItem> items = repositoryService.allMatches(
                new QueryDefault<>(
                        IncomingInvoiceItem.class,
                        "findByFixedAssetAndReportedDate",
                        "fixedAsset", property,
                        "reportedDate", reportedDate
                ));

        return filterByCompletedOrLaterInvoices(items, reportedDate);
    }

    @Programmatic
    public List<IncomingInvoiceItem> findCompletedOrLaterByFixedAssetAndIncomingInvoiceTypeAndReportedDate(
            final Property property,
            final IncomingInvoiceType incomingInvoiceType,
            final LocalDate reportedDate) {

        final List<IncomingInvoiceItem> items = repositoryService.allMatches(
                new QueryDefault<>(
                        IncomingInvoiceItem.class,
                        "findByFixedAssetAndIncomingInvoiceTypeAndReportedDate",
                        "fixedAsset", property,
                        "incomingInvoiceType", incomingInvoiceType,
                        "reportedDate", reportedDate
                ));

        return filterByCompletedOrLaterInvoices(items, reportedDate);
    }

    private List<IncomingInvoiceItem> filterByCompletedOrLaterInvoices(
            final List<IncomingInvoiceItem> items,
            final LocalDate reportedDate) {

        // we can't filter by property or incoming invoice type to filter here
        // because individual invoice items can override
        final List<? extends Invoice> incomingInvoices =
                incomingInvoiceRepository.findCompletedOrLaterWithItemsByReportedDate(reportedDate);

        // client-side join :-(
        return items.stream()
                .filter(x -> {
                    final Invoice<?> invoice = x.getInvoice();
                    return incomingInvoices.contains(invoice);
                })
                .collect(Collectors.toList());
    }


    @Programmatic
    public List<LocalDate> findDistinctReportDates() {
        final PersistenceManager pm = isisJdoSupport.getJdoPersistenceManager();
        final Query query = pm.newQuery(IncomingInvoiceItem.class);
        query.setResultClass(LocalDate.class);
        query.setResult("distinct reportedDate");
        query.setOrdering("reportedDate descending");
        return executeListAndClose(query);
    }

    private static <T> List<T> executeListAndClose(final Query query) {
        final List<T> elements = (List<T>) query.execute();
        final List<T> list = Lists.newArrayList(elements);
        query.closeAll();
        return list;
    }

    @Inject
    IsisJdoSupport isisJdoSupport;
    @Inject
    RepositoryService repositoryService;
    @Inject
    ServiceRegistry2 serviceRegistry2;

    @Inject
    IncomingInvoiceRepository incomingInvoiceRepository;
}
