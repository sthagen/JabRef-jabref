package org.jabref.gui.collab.entrychange;

import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

import org.jabref.gui.DialogService;
import org.jabref.gui.collab.DatabaseChangeDetailsView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import com.tobiasdiez.easybind.EasyBind;

public final class EntryChangeDetailsView extends DatabaseChangeDetailsView {
    private boolean scrolling = false;

    public EntryChangeDetailsView(BibEntry oldEntry,
                                  BibEntry newEntry,
                                  BibDatabaseContext databaseContext,
                                  DialogService dialogService,
                                  ThemeManager themeManager,
                                  GuiPreferences preferences,
                                  BibEntryTypesManager entryTypesManager,
                                  PreviewViewer previewViewer,
                                  TaskExecutor taskExecutor) {
        Label inJabRef = new Label(Localization.lang("In JabRef"));
        inJabRef.getStyleClass().add("lib-change-header");
        Label onDisk = new Label(Localization.lang("On disk"));
        onDisk.getStyleClass().add("lib-change-header");

        // we need a copy here as we otherwise would set the same entry twice
        PreviewViewer previewClone = new PreviewViewer(dialogService, preferences, themeManager, taskExecutor);
        previewClone.setDatabaseContext(databaseContext);

        // The scroll bar used is not part of ScrollPane, but the attached WebView.
        WebView previewCloneView = (WebView) previewClone.getContent();
        WebView previewViewerView = (WebView) previewViewer.getContent();
        synchronizeScrolling(previewCloneView, previewViewerView);
        synchronizeScrolling(previewViewerView, previewCloneView);
        // TODO: Also sync scrolling for BibTeX view.

        PreviewWithSourceTab oldPreviewWithSourcesTab = new PreviewWithSourceTab();
        TabPane oldEntryTabPane = oldPreviewWithSourcesTab.getPreviewWithSourceTab(oldEntry, databaseContext, preferences, entryTypesManager, previewClone, Localization.lang("Entry Preview"));
        PreviewWithSourceTab newPreviewWithSourcesTab = new PreviewWithSourceTab();
        TabPane newEntryTabPane = newPreviewWithSourcesTab.getPreviewWithSourceTab(newEntry, databaseContext, preferences, entryTypesManager, previewViewer, Localization.lang("Entry Preview"));

        EasyBind.subscribe(
            oldEntryTabPane.getSelectionModel().selectedIndexProperty(),
            selectedIndex -> newEntryTabPane.getSelectionModel().select(selectedIndex.intValue()));

        EasyBind.subscribe(newEntryTabPane.getSelectionModel().selectedIndexProperty(), selectedIndex -> {
            if (oldEntryTabPane.getSelectionModel().getSelectedIndex() != selectedIndex.intValue()) {
                oldEntryTabPane.getSelectionModel().select(selectedIndex.intValue());
            }
        });

        VBox containerOld = new VBox(inJabRef, oldEntryTabPane);
        VBox containerNew = new VBox(onDisk, newEntryTabPane);

        SplitPane split = new SplitPane(containerOld, containerNew);
        split.setOrientation(Orientation.HORIZONTAL);

        this.setAllAnchorsAndAttachChild(split);
    }

    // Method adapted from:
    // https://stackoverflow.com/questions/49509395/synchronize-scrollbars-of-two-javafx-webviews
    // https://stackoverflow.com/questions/31264847/how-to-set-remember-scrollbar-thumb-position-in-javafx-8-webview
    private void synchronizeScrolling(WebView webView, WebView otherWebView) {
        webView.addEventHandler(Event.ANY, event -> {
            if (!scrolling) {
                scrolling = true;
                if (event instanceof MouseEvent mouseEvent) {
                    if (mouseEvent.isPrimaryButtonDown()) {
                        int value = (Integer) webView.getEngine().executeScript("window.scrollY");
                        otherWebView.getEngine().executeScript("window.scrollTo(0, " + value + ")");
                    }
                } else {
                    otherWebView.fireEvent(event.copyFor(otherWebView, otherWebView));
                }
                scrolling = false;
            }
        });
    }
}
