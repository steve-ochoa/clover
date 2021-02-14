package org.openclover.eclipse.core.views.coverageexplorer.widgets;

import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.openclover.eclipse.core.ui.SwtUtils;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.settings.InstallationSettings;

public class InstallationSettingsDialog extends PopupDialog {
    private final Point location;
    private Button apply;
    private Combo refreshIntervalCombo;
    private Button refreshButton;
    private Button aggregateCoverageButton;
    private Button trackPerTestCoverageButton;
    private Button inMemPerTestCoverageButton;

    public InstallationSettingsDialog(Shell parent, Point location) {
        super(parent, INFOPOPUP_SHELLSTYLE, true, false, false, false, false, "Workspace Settings", null);
        this.location = location;
    }

    @Override
    protected Control createDialogArea(Composite composite) {
        Composite parent = new Composite(composite, SWT.NONE);
        parent.setLayout(new GridLayout(2, false));

        SelectionListener onChangeEnableApply = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                apply.setEnabled(true);
            }
        };

        aggregateCoverageButton = new Button(parent, SWT.CHECK);
        SwtUtils.gridDataFor(aggregateCoverageButton).horizontalSpan = 2;
        aggregateCoverageButton.setText("Aggregate coverage generated since the last clean build");
        aggregateCoverageButton.setToolTipText(
            "If enabled, Clover will aggregate all coverage generated from all tests run since the last clean build " +
            "(this is useful when you want to continually track how your total coverage changes as you change your code and re-run tests)\n\n" +
            "If disabled, Clover will only load coverage generated since the last source change " +
            "(this is useful when you are focussing on a single test and wish to interatively build out unit tests to increase its coverage)");
        aggregateCoverageButton.setSelection(CloverPlugin.getInstance().getInstallationSettings().isAggregatingCoverage());
        aggregateCoverageButton.addSelectionListener(onChangeEnableApply);

        trackPerTestCoverageButton = new Button(parent, SWT.CHECK);
        SwtUtils.gridDataFor(trackPerTestCoverageButton).horizontalSpan = 2;
        trackPerTestCoverageButton.setText("Track per-test coverage");
        trackPerTestCoverageButton.setToolTipText(
            "Enabled Clover to track the coverage generated by each test in your project. You can disable this setting if " +
            "you only care about total coverage, and not per-test coverage. For per-test coverage to operate correctly, " +
            "you must ensure your JUnit or TestNG tests are instrumented by Clover.");
        trackPerTestCoverageButton.setSelection(CloverPlugin.getInstance().getInstallationSettings().isTrackingPerTestCoverage());
        trackPerTestCoverageButton.addSelectionListener(onChangeEnableApply);
        trackPerTestCoverageButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                inMemPerTestCoverageButton.setEnabled(trackPerTestCoverageButton.getSelection());
            }
        });

        inMemPerTestCoverageButton = new Button(parent, SWT.CHECK);
        SwtUtils.gridDataFor(inMemPerTestCoverageButton).horizontalSpan = 2;
        inMemPerTestCoverageButton.setText("Keep per-test coverage data fully in memory");
        inMemPerTestCoverageButton.setToolTipText(
            "If enabled, per-test coverage data is loaded into memory which may speed performance of certain operations but will consume more " +
            "memory. This is only recommended if your Clover-enabled projects are small in size and small in number.");
        inMemPerTestCoverageButton.setSelection(CloverPlugin.getInstance().getInstallationSettings().isPerTestCoverageInMemory());
        inMemPerTestCoverageButton.addSelectionListener(onChangeEnableApply);
        inMemPerTestCoverageButton.setEnabled(trackPerTestCoverageButton.getSelection());

        refreshButton = new Button(parent, SWT.CHECK);
        refreshButton.setText("Look for updated coverage every:");
        refreshButton.setToolTipText(
            "Tells Clover whether it should look for new coverage generated by your tests or by running your application. " +
            "If disabled, to see updated coverage you must select your project in the Coverage Explorer and manually reload coverage " +
            "(via the button in the Coverage Explorer toolbar or via the context menu).");
        refreshButton.setSelection(CloverPlugin.getInstance().getInstallationSettings().isAutoRefreshingCoverage());
        refreshButton.addSelectionListener(onChangeEnableApply);
        refreshButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                refreshIntervalCombo.setEnabled(refreshButton.getSelection());
            }
        });

        refreshIntervalCombo = new Combo(parent, SWT.READ_ONLY);
        refreshIntervalCombo.setItems(new String[]{
            CloverEclipsePluginMessages.TWO_SECONDS(),
            CloverEclipsePluginMessages.FIVE_SECONDS(),
            CloverEclipsePluginMessages.TEN_SECONDS(),
            CloverEclipsePluginMessages.TWENTY_SECONDS()
        });
        final long refreshPeriod = CloverPlugin.getInstance().getInstallationSettings().getCoverageRefreshPeriod();
        if (refreshPeriod == InstallationSettings.Values.FIVE_SECONDS_COVERAGE_REFRESH_PERIOD) {
            refreshIntervalCombo.select(1);
        } else if (refreshPeriod == InstallationSettings.Values.TEN_SECONDS_COVERAGE_REFRESH_PERIOD) {
            refreshIntervalCombo.select(2);
        } else if (refreshPeriod == InstallationSettings.Values.TWENTY_SECONDS_COVERAGE_REFRESH_PERIOD) {
            refreshIntervalCombo.select(3);
        } else {
            refreshIntervalCombo.select(0);
        }
        
        refreshIntervalCombo.addSelectionListener(onChangeEnableApply);

        apply = new Button(parent, SWT.NONE);
        apply.setText("Apply");
        SwtUtils.gridDataFor(apply).horizontalSpan = 2;
        SwtUtils.gridDataFor(apply).horizontalAlignment = GridData.END;
        apply.setEnabled(false);
        apply.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                boolean needsReload =
                    (CloverPlugin.getInstance().getInstallationSettings().isAggregatingCoverage() ^ aggregateCoverageButton.getSelection())
                    || (CloverPlugin.getInstance().getInstallationSettings().isTrackingPerTestCoverage() ^ trackPerTestCoverageButton.getSelection())
                    || (CloverPlugin.getInstance().getInstallationSettings().isPerTestCoverageInMemory() ^ inMemPerTestCoverageButton.getSelection());

                CloverPlugin.getInstance().getInstallationSettings().setAggregateCoverage(aggregateCoverageButton.getSelection());
                CloverPlugin.getInstance().getInstallationSettings().setTrackingPerTestCoverage(trackPerTestCoverageButton.getSelection());
                CloverPlugin.getInstance().getInstallationSettings().setAutoRefreshingCoverage(refreshButton.getSelection());
                CloverPlugin.getInstance().getInstallationSettings().setPerTestCoverageInMemory(inMemPerTestCoverageButton.getSelection());
                long refreshPeriod = 0l;
                switch (refreshIntervalCombo.getSelectionIndex()) {
                    case 1:
                        refreshPeriod = InstallationSettings.Values.FIVE_SECONDS_COVERAGE_REFRESH_PERIOD;
                        break;
                    case 2:
                        refreshPeriod = InstallationSettings.Values.TEN_SECONDS_COVERAGE_REFRESH_PERIOD;
                        break;
                    case 3:
                        refreshPeriod = InstallationSettings.Values.TWENTY_SECONDS_COVERAGE_REFRESH_PERIOD;
                        break;
                    default:
                        refreshPeriod = InstallationSettings.Values.TWO_SECONDS_COVERAGE_REFRESH_PERIOD;
                        break;
                }
                CloverPlugin.getInstance().getInstallationSettings().setValue(InstallationSettings.Keys.COVERAGE_REFRESH_PERIOD, (int)refreshPeriod);
                apply.setEnabled(false);
                close();
                if (needsReload) {
                    CloverProject.refreshAllModels(false, true);
                }
            }
        });

        return parent;
    }

    @Override
    protected Point getInitialLocation(Point point) {
        return location;
    }
}
