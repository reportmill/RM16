/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.base.ReportMill;
import snap.util.*;
import snap.view.*;
import snap.viewx.DialogBox;

/**
 * This class provides a UI panel to send feedback back to ReportMill.
 */
public class FeedbackPanel extends ViewOwner {

    /**
     * Show panel.
     */
    public void showPanel(View aView)
    {
        // Show panel (just return if cancelled)
        DialogBox dbox = new DialogBox("ReportMill Feedback");
        dbox.setContent(getUI());
        dbox.setOptions("Submit", "Cancel");
        if (!dbox.showConfirmDialog(null)) return;

        // Update preferences and send feedback
        Prefs.getDefaultPrefs().setValue("ExceptionUserName", getViewStringValue("UserText"));
        Prefs.getDefaultPrefs().setValue("ExceptionEmail", getViewStringValue("EmailText"));
        sendFeedback();
    }

    /**
     * Initialize UI.
     */
    public void initUI()
    {
        setViewValue("UserText", Prefs.getDefaultPrefs().getString("ExceptionUserName", ""));
        setViewValue("EmailText", Prefs.getDefaultPrefs().getString("ExceptionEmail", ""));

        // Configure TypeComboBox
        String[] types = {"Bug Report", "Enhancement Request", "General Comment"};
        setViewItems("TypeComboBox", types);

        // Configure SeverityComboBox
        String[] severities = {"Low", "Medium", "High"};
        setViewItems("SeverityComboBox", severities);

        // Configure ModuleComboBox
        String[] mods = {"General", "Text", "Images", "Drawing", "Tables", "Graphs", "Labels", "API", "Other"};
        setViewItems("ModuleComboBox", mods);
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        // Update UserText, EmailText
        setViewValue("UserText", UserInfo.getUserName());
        setViewValue("EmailText", UserInfo.getUserEmail());
    }

    /**
     * Respond UI.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        switch (anEvent.getName()) {

            // Handle UserText, EmailText
            case "UserText" -> UserInfo.setUserName(anEvent.getStringValue());
            case "EmailText" -> UserInfo.setUserEmail(anEvent.getStringValue());
        }
    }

    /**
     * Send feedback via SendMail.py at reportmill.com.
     */
    public void sendFeedback()
    {
        // Get to address, from address and subject
        String toAddr = "support@reportmill.com";
        String fromAddr = UserInfo.getFullUserEmail();
        String subject = "ReportMill Feedback";

        // Configure environment string
        StringBuilder env = new StringBuilder();
        String lic = ReportMill.getLicense(); if (lic == null) lic = "Unlicensed Copy";
        env.append("License: ").append(lic).append('\n');
        env.append("Build Date: ").append(ReportMill.getBuildInfo()).append('\n');
        env.append(SnapUtils.getSystemInfo());

        // Get body
        StringBuilder sb = new StringBuilder();
        sb.append(subject).append('\n').append('\n');
        sb.append("From: ").append(fromAddr).append('\n');
        sb.append("Type: ").append(getViewStringValue("TypeComboBox")).append('\n');
        sb.append("Severity: ").append(getViewStringValue("SeverityComboBox")).append('\n');
        sb.append("Module: ").append(getViewStringValue("ModuleComboBox")).append('\n').append('\n');
        sb.append("Title: ").append(getViewStringValue("TitleText")).append('\n').append('\n');
        sb.append(getViewStringValue("DescriptionText")).append('\n').append('\n').append(env);
        String body = sb.toString();

        // Send email in background thread
        new Thread(() -> {
            String str = UserInfo.sendMail(toAddr, fromAddr, subject, body);
            if (str != null)
                System.out.println("ExceptionReporter Response: " + str);
        }).start();
    }
}