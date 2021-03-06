/*
 * Copyright 2000-2013 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.client.ui;

import java.util.Date;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.LiveValue;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.BrowserInfo;
import com.vaadin.client.VConsole;
import com.vaadin.client.ui.VCalendarPanel.FocusOutListener;
import com.vaadin.client.ui.VCalendarPanel.SubmitListener;
import com.vaadin.client.ui.aria.AriaHelper;
import com.vaadin.shared.ui.datefield.PopupDateFieldState;
import com.vaadin.shared.ui.datefield.Resolution;

/**
 * Represents a date selection component with a text field and a popup date
 * selector.
 * 
 * <b>Note:</b> To change the keyboard assignments used in the popup dialog you
 * should extend <code>com.vaadin.client.ui.VCalendarPanel</code> and then pass
 * set it by calling the <code>setCalendarPanel(VCalendarPanel panel)</code>
 * method.
 * 
 */
public class VPopupCalendar extends VTextualDate implements Field,
        ClickHandler, CloseHandler<PopupPanel>, SubPartAware {

    /** For internal use only. May be removed or replaced in the future. */
    public final Button calendarToggle = new Button();

    /** For internal use only. May be removed or replaced in the future. */
    public VCalendarPanel calendar;

    /** For internal use only. May be removed or replaced in the future. */
    public final VOverlay popup;

    /** For internal use only. May be removed or replaced in the future. */
    public boolean parsable = true;

    private boolean open = false;

    private boolean textFieldEnabled = true;

    private String captionId;

    private Label selectedDate;

    private Element descriptionForAssisitveDevicesElement;

    public VPopupCalendar() {
        super();

        calendarToggle.setText("");
        calendarToggle.addClickHandler(this);
        // -2 instead of -1 to avoid FocusWidget.onAttach to reset it
        calendarToggle.getElement().setTabIndex(-2);

        Roles.getButtonRole().set(calendarToggle.getElement());
        Roles.getButtonRole().setAriaHiddenState(calendarToggle.getElement(),
                true);

        add(calendarToggle);

        // Description of the usage of the widget for assisitve device users
        descriptionForAssisitveDevicesElement = DOM.createDiv();
        descriptionForAssisitveDevicesElement
                .setInnerText(PopupDateFieldState.DESCRIPTION_FOR_ASSISTIVE_DEVICES);
        AriaHelper.ensureHasId(descriptionForAssisitveDevicesElement);
        Id.of(descriptionForAssisitveDevicesElement);
        AriaHelper.setVisibleForAssistiveDevicesOnly(
                descriptionForAssisitveDevicesElement, true);
        DOM.appendChild(getElement(), descriptionForAssisitveDevicesElement);

        calendar = GWT.create(VCalendarPanel.class);
        calendar.setParentField(this);
        calendar.setFocusOutListener(new FocusOutListener() {
            @Override
            public boolean onFocusOut(DomEvent<?> event) {
                event.preventDefault();
                closeCalendarPanel();
                return true;
            }
        });

        Roles.getButtonRole().setAriaControlsProperty(
                calendarToggle.getElement(), Id.of(calendar.getElement()));

        calendar.setSubmitListener(new SubmitListener() {
            @Override
            public void onSubmit() {
                // Update internal value and send valuechange event if immediate
                updateValue(calendar.getDate());

                // Update text field (a must when not immediate).
                buildDate(true);

                closeCalendarPanel();
            }

            @Override
            public void onCancel() {
                closeCalendarPanel();
            }
        });

        popup = new VOverlay(true, true, true);
        popup.setOwner(this);

        FlowPanel wrapper = new FlowPanel();
        selectedDate = new Label();
        selectedDate.setStyleName(getStylePrimaryName() + "-selecteddate");
        AriaHelper.setVisibleForAssistiveDevicesOnly(selectedDate.getElement(),
                true);

        Roles.getTextboxRole().setAriaLiveProperty(selectedDate.getElement(),
                LiveValue.ASSERTIVE);
        Roles.getTextboxRole().setAriaAtomicProperty(selectedDate.getElement(),
                true);
        wrapper.add(selectedDate);
        wrapper.add(calendar);

        popup.setWidget(wrapper);
        popup.addCloseHandler(this);

        DOM.setElementProperty(calendar.getElement(), "id",
                "PID_VAADIN_POPUPCAL");

        sinkEvents(Event.ONKEYDOWN);

        updateStyleNames();
    }

    @SuppressWarnings("deprecation")
    public void updateValue(Date newDate) {
        Date currentDate = getCurrentDate();
        if (currentDate == null || newDate.getTime() != currentDate.getTime()) {
            setCurrentDate((Date) newDate.clone());
            getClient().updateVariable(getId(), "year",
                    newDate.getYear() + 1900, false);
            if (getCurrentResolution().getCalendarField() > Resolution.YEAR
                    .getCalendarField()) {
                getClient().updateVariable(getId(), "month",
                        newDate.getMonth() + 1, false);
                if (getCurrentResolution().getCalendarField() > Resolution.MONTH
                        .getCalendarField()) {
                    getClient().updateVariable(getId(), "day",
                            newDate.getDate(), false);
                    if (getCurrentResolution().getCalendarField() > Resolution.DAY
                            .getCalendarField()) {
                        getClient().updateVariable(getId(), "hour",
                                newDate.getHours(), false);
                        if (getCurrentResolution().getCalendarField() > Resolution.HOUR
                                .getCalendarField()) {
                            getClient().updateVariable(getId(), "min",
                                    newDate.getMinutes(), false);
                            if (getCurrentResolution().getCalendarField() > Resolution.MINUTE
                                    .getCalendarField()) {
                                getClient().updateVariable(getId(), "sec",
                                        newDate.getSeconds(), false);
                            }
                        }
                    }
                }
            }
            if (isImmediate()) {
                getClient().sendPendingVariableChanges();
            }
        }
    }

    /**
     * Checks whether the text field is enabled.
     * 
     * @see VPopupCalendar#setTextFieldEnabled(boolean)
     * @return The current state of the text field.
     */
    public boolean isTextFieldEnabled() {
        return textFieldEnabled;
    }

    /**
     * Sets the state of the text field of this component. By default the text
     * field is enabled. Disabling it causes only the button for date selection
     * to be active, thus preventing the user from entering invalid dates. See
     * {@link http://dev.vaadin.com/ticket/6790}.
     * 
     * @param state
     */
    public void setTextFieldEnabled(boolean textFieldEnabled) {
        this.textFieldEnabled = textFieldEnabled;
        text.setEnabled(textFieldEnabled);
        if (textFieldEnabled) {
            calendarToggle.setTabIndex(-1);
            Roles.getButtonRole().setAriaHiddenState(
                    calendarToggle.getElement(), true);
        } else {
            calendarToggle.setTabIndex(0);
            Roles.getButtonRole().setAriaHiddenState(
                    calendarToggle.getElement(), false);
        }

        handleAriaAttributes();
    }

    @Override
    public void bindAriaCaption(Element captionElement) {
        if (captionElement == null) {
            captionId = null;
        } else {
            captionId = captionElement.getId();
        }

        if (isTextFieldEnabled()) {
            super.bindAriaCaption(captionElement);
        } else {
            AriaHelper.bindCaption(calendarToggle, captionElement);
        }

        handleAriaAttributes();
    }

    private void handleAriaAttributes() {
        Widget removeFromWidget;
        Widget setForWidget;

        if (isTextFieldEnabled()) {
            setForWidget = text;
            removeFromWidget = calendarToggle;
        } else {
            setForWidget = calendarToggle;
            removeFromWidget = text;
        }

        Roles.getFormRole().removeAriaLabelledbyProperty(
                removeFromWidget.getElement());
        if (captionId == null) {
            Roles.getFormRole().removeAriaLabelledbyProperty(
                    setForWidget.getElement());
        } else {
            Roles.getFormRole().setAriaLabelledbyProperty(
                    setForWidget.getElement(), Id.of(captionId));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.google.gwt.user.client.ui.UIObject#setStyleName(java.lang.String)
     */
    @Override
    public void setStyleName(String style) {
        super.setStyleName(style);
        updateStyleNames();
    }

    @Override
    public void setStylePrimaryName(String style) {
        removeStyleName(getStylePrimaryName() + "-popupcalendar");
        super.setStylePrimaryName(style);
        updateStyleNames();
    }

    @Override
    protected void updateStyleNames() {
        super.updateStyleNames();
        if (getStylePrimaryName() != null && calendarToggle != null) {
            addStyleName(getStylePrimaryName() + "-popupcalendar");
            calendarToggle.setStyleName(getStylePrimaryName() + "-button");
            popup.setStyleName(getStylePrimaryName() + "-popup");
            calendar.setStyleName(getStylePrimaryName() + "-calendarpanel");
        }
    }

    /**
     * Opens the calendar panel popup
     */
    public void openCalendarPanel() {

        if (!open && !readonly) {
            open = true;

            if (getCurrentDate() != null) {
                calendar.setDate((Date) getCurrentDate().clone());
            } else {
                calendar.setDate(new Date());
            }

            // clear previous values
            popup.setWidth("");
            popup.setHeight("");
            popup.setPopupPositionAndShow(new PositionCallback() {
                @Override
                public void setPosition(int offsetWidth, int offsetHeight) {
                    final int w = offsetWidth;
                    final int h = offsetHeight;
                    final int browserWindowWidth = Window.getClientWidth()
                            + Window.getScrollLeft();
                    final int browserWindowHeight = Window.getClientHeight()
                            + Window.getScrollTop();
                    int t = calendarToggle.getAbsoluteTop();
                    int l = calendarToggle.getAbsoluteLeft();

                    // Add a little extra space to the right to avoid
                    // problems with IE7 scrollbars and to make it look
                    // nicer.
                    int extraSpace = 30;

                    boolean overflowRight = false;
                    if (l + +w + extraSpace > browserWindowWidth) {
                        overflowRight = true;
                        // Part of the popup is outside the browser window
                        // (to the right)
                        l = browserWindowWidth - w - extraSpace;
                    }

                    if (t + h + calendarToggle.getOffsetHeight() + 30 > browserWindowHeight) {
                        // Part of the popup is outside the browser window
                        // (below)
                        t = browserWindowHeight - h
                                - calendarToggle.getOffsetHeight() - 30;
                        if (!overflowRight) {
                            // Show to the right of the popup button unless we
                            // are in the lower right corner of the screen
                            l += calendarToggle.getOffsetWidth();
                        }
                    }

                    popup.setPopupPosition(l,
                            t + calendarToggle.getOffsetHeight() + 2);

                    /*
                     * We have to wait a while before focusing since the popup
                     * needs to be opened before we can focus
                     */
                    Timer focusTimer = new Timer() {
                        @Override
                        public void run() {
                            setFocus(true);
                        }
                    };

                    focusTimer.schedule(100);
                }
            });
        } else {
            VConsole.error("Cannot reopen popup, it is already open!");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.google.gwt.event.dom.client.ClickHandler#onClick(com.google.gwt.event
     * .dom.client.ClickEvent)
     */
    @Override
    public void onClick(ClickEvent event) {
        if (event.getSource() == calendarToggle && isEnabled()) {
            openCalendarPanel();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.google.gwt.event.logical.shared.CloseHandler#onClose(com.google.gwt
     * .event.logical.shared.CloseEvent)
     */
    @Override
    public void onClose(CloseEvent<PopupPanel> event) {
        if (event.getSource() == popup) {
            buildDate();
            if (!BrowserInfo.get().isTouchDevice()) {
                /*
                 * Move focus to textbox, unless on touch device (avoids opening
                 * virtual keyboard).
                 */
                focus();
            }

            // TODO resolve what the "Sigh." is all about and document it here
            // Sigh.
            Timer t = new Timer() {
                @Override
                public void run() {
                    open = false;
                }
            };
            t.schedule(100);
        }
    }

    /**
     * Sets focus to Calendar panel.
     * 
     * @param focus
     */
    public void setFocus(boolean focus) {
        calendar.setFocus(focus);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (enabled) {
            Roles.getButtonRole().setAriaDisabledState(
                    calendarToggle.getElement(), true);
        } else {
            Roles.getButtonRole().setAriaDisabledState(
                    calendarToggle.getElement(), false);
        }
    }

    /**
     * Sets the content of a special field for assistive devices, so that they
     * can recognize the change and inform the user (reading out in case of
     * screen reader)
     * 
     * @param selectedDate
     *            Date that is currently selected
     */
    public void setFocusedDate(Date selectedDate) {
        this.selectedDate.setText(DateTimeFormat.getFormat("dd, MMMM, yyyy")
                .format(selectedDate));
    }

    /**
     * For internal use only. May be removed or replaced in the future.
     * 
     * @see com.vaadin.client.ui.VTextualDate#buildDate()
     */
    @Override
    public void buildDate() {
        // Save previous value
        String previousValue = getText();
        super.buildDate();

        // Restore previous value if the input could not be parsed
        if (!parsable) {
            setText(previousValue);
        }

        // superclass sets the text field independently when building date
        text.setEnabled(isEnabled() && isTextFieldEnabled());
    }

    /**
     * Update the text field contents from the date. See {@link #buildDate()}.
     * 
     * @param forceValid
     *            true to force the text field to be updated, false to only
     *            update if the parsable flag is true.
     */
    protected void buildDate(boolean forceValid) {
        if (forceValid) {
            parsable = true;
        }
        buildDate();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.client.ui.VDateField#onBrowserEvent(com.google
     * .gwt.user.client.Event)
     */
    @Override
    public void onBrowserEvent(com.google.gwt.user.client.Event event) {
        super.onBrowserEvent(event);
        if (DOM.eventGetType(event) == Event.ONKEYDOWN
                && event.getKeyCode() == getOpenCalenderPanelKey()) {
            openCalendarPanel();
            event.preventDefault();
        }
    }

    /**
     * Get the key code that opens the calendar panel. By default it is the down
     * key but you can override this to be whatever you like
     * 
     * @return
     */
    protected int getOpenCalenderPanelKey() {
        return KeyCodes.KEY_DOWN;
    }

    /**
     * Closes the open popup panel
     */
    public void closeCalendarPanel() {
        if (open) {
            popup.hide(true);
        }
    }

    private final String CALENDAR_TOGGLE_ID = "popupButton";

    @Override
    public Element getSubPartElement(String subPart) {
        if (subPart.equals(CALENDAR_TOGGLE_ID)) {
            return calendarToggle.getElement();
        }

        return super.getSubPartElement(subPart);
    }

    @Override
    public String getSubPartName(Element subElement) {
        if (calendarToggle.getElement().isOrHasChild(subElement)) {
            return CALENDAR_TOGGLE_ID;
        }

        return super.getSubPartName(subElement);
    }

    /**
     * Set a description that explains the usage of the Widget for users of
     * assistive devices.
     * 
     * @param descriptionForAssistiveDevices
     *            String with the description
     */
    public void setDescriptionForAssistiveDevices(
            String descriptionForAssistiveDevices) {
        descriptionForAssisitveDevicesElement
                .setInnerText(descriptionForAssistiveDevices);
    }

    /**
     * Get the description that explains the usage of the Widget for users of
     * assistive devices.
     * 
     * @return String with the description
     */
    public String getDescriptionForAssistiveDevices() {
        return descriptionForAssisitveDevicesElement.getInnerText();
    }

    /**
     * Sets the start range for this component. The start range is inclusive,
     * and it depends on the current resolution, what is considered inside the
     * range.
     * 
     * @param startDate
     *            - the allowed range's start date
     */
    public void setRangeStart(Date rangeStart) {
        calendar.setRangeStart(rangeStart);
    }

    /**
     * Sets the end range for this component. The end range is inclusive, and it
     * depends on the current resolution, what is considered inside the range.
     * 
     * @param endDate
     *            - the allowed range's end date
     */
    public void setRangeEnd(Date rangeEnd) {
        calendar.setRangeEnd(rangeEnd);
    }

}
