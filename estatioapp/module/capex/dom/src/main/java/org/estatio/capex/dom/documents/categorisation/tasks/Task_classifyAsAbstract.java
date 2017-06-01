package org.estatio.capex.dom.documents.categorisation.tasks;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.Contributed;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.services.queryresultscache.QueryResultsCache;
import org.apache.isis.applib.services.registry.ServiceRegistry2;

import org.incode.module.document.dom.impl.docs.Document;
import org.incode.module.document.dom.impl.paperclips.PaperclipRepository;

import org.estatio.capex.dom.documents.categorisation.IncomingDocumentCategorisationStateTransition;
import org.estatio.capex.dom.documents.categorisation.document.IncomingDocViewModel;
import org.estatio.capex.dom.task.Task;
import org.estatio.dom.invoice.DocumentTypeData;

public abstract class Task_classifyAsAbstract {

    protected final Task task;
    private final DocumentTypeData documentTypeData;

    public Task_classifyAsAbstract(final Task task, final DocumentTypeData documentTypeData) {
        this.task = task;
        this.documentTypeData = documentTypeData;
    }

    @Action(semantics = SemanticsOf.SAFE)
    @ActionLayout(contributed = Contributed.AS_ACTION)
    public IncomingDocViewModel act() {
        IncomingDocViewModel viewModel = doCreate();

        serviceRegistry2.injectServicesInto(viewModel);
        viewModel.inferFixedAssetFromPaperclips();

        // to support 'goToNext' when finished with the view model
        viewModel.setTask(task);
        return viewModel;
    }

    protected abstract IncomingDocViewModel doCreate();

    public boolean hideAct() {
        return task.isCompleted() || getDocument() == null || !documentTypeData.isDocTypeFor(getDocument());
    }

    protected Document getDocument() {
        return queryResultsCache.execute(
                this::doGetDocument,
                getClass(), "getDocument", task);
    }

    private Document doGetDocument() {
        final IncomingDocumentCategorisationStateTransition transition = repository.findByTask(this.task);
        return transition != null ? transition.getDocument() : null;
    }

    @Inject
    protected PaperclipRepository paperclipRepository;

    @Inject
    ServiceRegistry2 serviceRegistry2;

    @Inject
    IncomingDocumentCategorisationStateTransition.Repository repository;

    @Inject
    QueryResultsCache queryResultsCache;

}