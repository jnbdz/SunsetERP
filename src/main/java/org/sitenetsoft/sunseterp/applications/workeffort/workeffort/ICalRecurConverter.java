/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/

package org.sitenetsoft.sunseterp.applications.workeffort.workeffort;

import java.time.temporal.Temporal;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.ObjectType;
import org.sitenetsoft.sunseterp.framework.service.calendar.TemporalExpression;
import org.sitenetsoft.sunseterp.framework.service.calendar.TemporalExpressionVisitor;
import org.sitenetsoft.sunseterp.framework.service.calendar.TemporalExpressions;
import org.sitenetsoft.sunseterp.framework.service.calendar.TemporalExpressions.Difference;
import org.sitenetsoft.sunseterp.framework.service.calendar.TemporalExpressions.HourRange;
import org.sitenetsoft.sunseterp.framework.service.calendar.TemporalExpressions.Intersection;
import org.sitenetsoft.sunseterp.framework.service.calendar.TemporalExpressions.MinuteRange;
import org.sitenetsoft.sunseterp.framework.service.calendar.TemporalExpressions.Null;
import org.sitenetsoft.sunseterp.framework.service.calendar.TemporalExpressions.Substitution;
import org.sitenetsoft.sunseterp.framework.service.calendar.TemporalExpressions.Union;

import com.ibm.icu.util.Calendar;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.NumberList;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.WeekDay;
import net.fortuna.ical4j.model.WeekDayList;
import net.fortuna.ical4j.model.property.DateListProperty;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.ExRule;
import net.fortuna.ical4j.model.property.RDate;
import net.fortuna.ical4j.model.property.RRule;

/** Temporal Expression to iCalendar recurrence converter. The conversion results
 * (or conversion success) are unpredictable since the OFBiz Temporal Expressions
 * are more sophisticated than iCalendar recurrences. This class attempts to
 * make a best attempt at conversion and throws <code>IllegalStateException</code>
 * when conversion is not possible.
 */
public class ICalRecurConverter implements TemporalExpressionVisitor {
    protected static final WeekDay DAY_OF_WEEK_ARRAY[] = {WeekDay.SU, WeekDay.MO, WeekDay.TU, WeekDay.WE, WeekDay.TH, WeekDay.FR, WeekDay.SA};
    private static final String MODULE = ObjectType.class.getName();

    @SuppressWarnings("unchecked")
    public static void convert(TemporalExpression expr, PropertyList eventProps) {
        // @NOTE change multiple lines from original code from OFBiz

        ICalRecurConverter converter = new ICalRecurConverter();
        expr.accept(converter);
        DtStart dateStart = (DtStart) eventProps.getFirst(Property.DTSTART).get();
        if (converter.dateStart != null) {
            if (dateStart != null) {
                eventProps.remove(dateStart);
            }
            dateStart = converter.dateStart;
            eventProps.add(dateStart);
        }
        if (dateStart != null && !converter.exRuleList.isEmpty()) {
            // iCalendar quirk - if exclusions exist, then the start date must be excluded also
            ExDate exdate = new ExDate();
            exdate.getDates().add(dateStart.getDate());
            converter.exDateList.add(exdate);
        }
        /*eventProps.addAll(converter.incDateList);
        eventProps.addAll(converter.incRuleList);
        eventProps.addAll(converter.exDateList);
        eventProps.addAll(converter.exRuleList);*/

        for (DateListProperty dateList : converter.incDateList) {
            eventProps.add(dateList);
        }

        for (RRule rule : converter.incRuleList) {
            eventProps.add(rule);
        }

        for (DateListProperty dateList : converter.exDateList) {
            eventProps.add(dateList);
        }

        for (ExRule rule : converter.exRuleList) {
            eventProps.add(rule);
        }
    }

    private DtStart dateStart = null;
    private List<DateListProperty> incDateList = new LinkedList<>();
    private List<DateListProperty> exDateList = new LinkedList<>();
    private List<RRule> incRuleList = new LinkedList<>();
    private List<ExRule> exRuleList = new LinkedList<>();
    private VisitorState state = new VisitorState();
    private Stack<VisitorState> stateStack = new Stack<>();

    protected ICalRecurConverter() { }

    /**
     * Consolidate recurs recur.
     * @param recurList the recur list
     * @return the recur
     */
    @SuppressWarnings("unchecked")
    protected Recur consolidateRecurs(List<Recur> recurList) {
        // Try to consolidate a list of Recur instances into one instance
        Set<Integer> monthList = new HashSet<>();
        Set<Integer> monthDayList = new HashSet<>();
        Set<WeekDay> weekDayList = new HashSet<>();
        Set<Integer> hourList = new HashSet<>();
        Set<Integer> minuteList = new HashSet<>();
        String freq = null;
        int freqCount = 0;
        for (Recur recur : recurList) {
            monthList.addAll(recur.getMonthList());
            monthDayList.addAll(recur.getMonthDayList());
            weekDayList.addAll(recur.getDayList());
            hourList.addAll(recur.getHourList());
            minuteList.addAll(recur.getMinuteList());
            if (recur.getInterval() != 0) {
                freq = String.valueOf(recur.getFrequency());
                freqCount = recur.getInterval();
            }
        }
        if (freq == null && !monthList.isEmpty()) {
            freq = Recur.MONTHLY;
        } else if (freq == null && (!monthDayList.isEmpty() || !weekDayList.isEmpty())) {
            freq = Recur.DAILY;
        } else if (freq == null && !hourList.isEmpty()) {
            freq = Recur.HOURLY;
        } else if (freq == null && !minuteList.isEmpty()) {
            freq = Recur.MINUTELY;
        }
        if (freq == null) {
            throw new IllegalStateException("Unable to convert intersection");
        }
        Recur newRecur = new Recur(freq, 0);
        if (freqCount != 0) {
            newRecur.setInterval(freqCount);
        }
        newRecur.getMonthList().addAll(monthList);
        newRecur.getMonthDayList().addAll(monthDayList);
        newRecur.getDayList().addAll(weekDayList);
        newRecur.getHourList().addAll(hourList);
        newRecur.getMinuteList().addAll(minuteList);
        return newRecur;
    }

    // ----- TemporalExpressionVisitor Implementation ----- //

    @Override
    public void visit(Difference expr) {
        VisitorState newState = new VisitorState();
        newState.isIntersection = this.state.isIntersection;
        this.stateStack.push(this.state);
        this.state = newState;
        expr.getIncluded().accept(this);
        newState.isExcluded = true;
        expr.getExcluded().accept(this);
        this.state = this.stateStack.pop();
        if (this.state.isIntersection) {
            this.state.inclRecurList.addAll(newState.inclRecurList);
            this.state.exRecurList.addAll(newState.exRecurList);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void visit(HourRange expr) {
        NumberList hourList = new NumberList();
        hourList.addAll(expr.getHourRangeAsSet());
        Recur recur = new Recur(Recur.HOURLY, 0);
        recur.getHourList().addAll(hourList);
        this.state.addRecur(recur);
    }

    @Override
    public void visit(Intersection expr) {
        this.stateStack.push(this.state);
        VisitorState newState = new VisitorState();
        newState.isExcluded = this.state.isExcluded;
        newState.isIntersection = true;
        this.state = newState;
        for (TemporalExpression childExpr : expr.getExpressionSet()) {
            childExpr.accept(this);
        }
        this.state = this.stateStack.pop();
        if (!newState.inclRecurList.isEmpty()) {
            this.incRuleList.add(new RRule(this.consolidateRecurs(newState.inclRecurList)));
        }
        if (!newState.exRecurList.isEmpty()) {
            this.exRuleList.add(new ExRule(this.consolidateRecurs(newState.exRecurList)));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void visit(MinuteRange expr) {
        NumberList minuteList = new NumberList();
        minuteList.addAll(expr.getMinuteRangeAsSet());
        Recur recur = new Recur(Recur.MINUTELY, 0);
        recur.getMinuteList().addAll(minuteList);
        this.state.addRecur(recur);
    }

    @Override
    public void visit(Null expr) { }

    @Override
    public void visit(Substitution expr) {
        Debug.logVerbose("iCalendar format does not support substitutions. Do nothing for now", MODULE);
    }

    @Override
    public void visit(TemporalExpressions.DateRange expr) {
        if (this.state.isExcluded) {
            throw new IllegalStateException("iCalendar does not support excluded date ranges");
        }
        org.sitenetsoft.sunseterp.framework.base.util.DateRange range = expr.getDateRange();
        PeriodList periodList = new PeriodList();
        periodList.add(new Period((Temporal) new DateTime(range.start()), (Temporal) new DateTime(range.end())));
        this.incDateList.add(new RDate((List<Period>) periodList));
    }

    @Override
    public void visit(TemporalExpressions.DayInMonth expr) {
        Recur recur = new Recur(Recur.MONTHLY, 0);
        recur.getDayList().add(new WeekDay(DAY_OF_WEEK_ARRAY[expr.getDayOfWeek() - 1], expr.getOccurrence()));
        this.state.addRecur(recur);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void visit(TemporalExpressions.DayOfMonthRange expr) {
        int startDay = expr.getStartDay();
        int endDay = expr.getEndDay();
        NumberList dayList = new NumberList();
        dayList.add(startDay);
        while (startDay != endDay) {
            startDay++;
            dayList.add(startDay);
        }
        Recur recur = new Recur(Recur.DAILY, 0);
        recur.getMonthDayList().addAll(dayList);
        this.state.addRecur(recur);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void visit(TemporalExpressions.DayOfWeekRange expr) {
        int startDay = expr.getStartDay();
        int endDay = expr.getEndDay();
        WeekDayList dayList = new WeekDayList();
        dayList.add(DAY_OF_WEEK_ARRAY[startDay - 1]);
        while (startDay != endDay) {
            startDay++;
            if (startDay > Calendar.SATURDAY) {
                startDay = Calendar.SUNDAY;
            }
            dayList.add(DAY_OF_WEEK_ARRAY[startDay - 1]);
        }
        Recur recur = new Recur(Recur.DAILY, 0);
        recur.getDayList().addAll(dayList);
        this.state.addRecur(recur);
    }

    @Override
    public void visit(TemporalExpressions.Frequency expr) {
        if (this.dateStart == null) {
            this.dateStart = new DtStart(String.valueOf(new net.fortuna.ical4j.model.Date(expr.getStartDate())));
        }
        int freqCount = expr.getFreqCount();
        int freqType = expr.getFreqType();
        switch (freqType) {
        case Calendar.SECOND:
            this.state.addRecur((new Recur(Recur.SECONDLY, freqCount)));
            break;
        case Calendar.MINUTE:
            this.state.addRecur((new Recur(Recur.MINUTELY, freqCount)));
            break;
        case Calendar.HOUR:
            this.state.addRecur((new Recur(Recur.HOURLY, freqCount)));
            break;
        case Calendar.DAY_OF_MONTH:
            this.state.addRecur((new Recur(Recur.DAILY, freqCount)));
            break;
        case Calendar.MONTH:
            this.state.addRecur((new Recur(Recur.MONTHLY, freqCount)));
            break;
        case Calendar.YEAR:
            this.state.addRecur((new Recur(Recur.YEARLY, freqCount)));
            break;
        default:
            break;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void visit(TemporalExpressions.MonthRange expr) {
        int startMonth = expr.getStartMonth();
        int endMonth = expr.getEndMonth();
        Calendar cal = Calendar.getInstance();
        int maxMonth = cal.getActualMaximum(Calendar.MONTH);
        NumberList monthList = new NumberList();
        monthList.add(startMonth + 1);
        while (startMonth != endMonth) {
            startMonth++;
            if (startMonth > maxMonth) {
                startMonth = Calendar.JANUARY;
            }
            monthList.add(startMonth + 1);
        }
        Recur recur = new Recur(Recur.MONTHLY, 0);
        recur.getMonthList().addAll(monthList);
        this.state.addRecur(recur);
    }

    @Override
    public void visit(Union expr) {
        for (TemporalExpression childExpr : expr.getExpressionSet()) {
            childExpr.accept(this);
        }
    }

    protected class VisitorState {
        private boolean isExcluded = false;
        private boolean isIntersection = false;
        private List<Recur> inclRecurList = new LinkedList<>();
        private List<Recur> exRecurList = new LinkedList<>();

        /**
         * Add recur.
         * @param recur the recur
         */
        public void addRecur(Recur recur) {
            if (this.isIntersection) {
                if (this.isExcluded) {
                    this.exRecurList.add(recur);
                } else {
                    this.inclRecurList.add(recur);
                }
            } else {
                if (this.isExcluded) {
                    exRuleList.add(new ExRule(recur));
                } else {
                    incRuleList.add(new RRule(recur));
                }
            }
        }
    }
}
