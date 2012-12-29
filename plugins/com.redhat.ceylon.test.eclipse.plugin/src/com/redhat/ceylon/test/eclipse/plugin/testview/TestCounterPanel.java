package com.redhat.ceylon.test.eclipse.plugin.testview;

import static com.redhat.ceylon.test.eclipse.plugin.CeylonTestImageRegistry.ERROR_OVR;
import static com.redhat.ceylon.test.eclipse.plugin.CeylonTestImageRegistry.FAILED_OVR;
import static com.redhat.ceylon.test.eclipse.plugin.CeylonTestImageRegistry.getImage;
import static com.redhat.ceylon.test.eclipse.plugin.CeylonTestMessages.counterErrors;
import static com.redhat.ceylon.test.eclipse.plugin.CeylonTestMessages.counterFailures;
import static com.redhat.ceylon.test.eclipse.plugin.CeylonTestMessages.counterRuns;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.redhat.ceylon.test.eclipse.plugin.model.TestRun;

public class TestCounterPanel extends Composite {

    private Label runsLabel;
    private Text runsText;
    private Label errorsImage;
    private Label errorsLabel;
    private Text errorsText;
    private Label failuresImage;
    private Label failuresLabel;
    private Text failuresText;
    private TestRun currentTestRun;

    public TestCounterPanel(Composite parent) {
        super(parent, SWT.WRAP);

        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 8;
        gridLayout.marginWidth = 0;
        setLayout(gridLayout);

        createRuns();
        createFailures();
        createErrors();
        updateView();
    }

    public void setCurrentTestRun(TestRun currentTestRun) {
        this.currentTestRun = currentTestRun;
    }

    private void createRuns() {
        runsLabel = new Label(this, SWT.NONE);
        runsLabel.setText(counterRuns);
        runsText = new Text(this, SWT.READ_ONLY);
        runsText.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).create());
        runsText.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
    }

    private void createFailures() {
        failuresImage = new Label(this, SWT.NONE);
        failuresImage.setImage(getImage(FAILED_OVR));
        failuresLabel = new Label(this, SWT.NONE);
        failuresLabel.setText(counterFailures);
        failuresText = new Text(this, SWT.READ_ONLY);
        failuresText.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).create());
        failuresText.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
    }

    private void createErrors() {
        errorsImage = new Label(this, SWT.NONE);
        errorsImage.setImage(getImage(ERROR_OVR));
        errorsLabel = new Label(this, SWT.NONE);
        errorsLabel.setText(counterErrors);
        errorsText = new Text(this, SWT.READ_ONLY);
        errorsText.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).create());
        errorsText.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
    }

    public void updateView() {
        int totalCount = 0;
        int finishedCount = 0;
        int failureCount = 0;
        int errorCount = 0;
        
        if( currentTestRun != null ) {
            totalCount = currentTestRun.getTotalCount();
            finishedCount = currentTestRun.getFinishedCount();
            failureCount = currentTestRun.getFailureCount();
            errorCount = currentTestRun.getErrorCount();
        }
        
        runsText.setText(Integer.toString(finishedCount) + "/" + Integer.toString(totalCount));
        failuresText.setText(Integer.toString(failureCount));
        errorsText.setText(Integer.toString(errorCount));
        
        redraw();
    }

}