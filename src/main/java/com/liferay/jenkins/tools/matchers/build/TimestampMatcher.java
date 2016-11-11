/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.jenkins.tools;

import java.text.DateFormat;
import java.text.ParseException;

import java.util.Calendar;
import java.util.Date;

import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;

/**
 * @author Kevin Yen
 */
public abstract class TimestampMatcher implements BuildMatcher {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(
		TimestampMatcher.class);

	protected static Date parseTimestamp(String timestamp)
		throws IllegalArgumentException {

		try {
			return new Date(Long.parseLong(timestamp));
		}
		catch (NumberFormatException e) {
			logger.debug("{} is not unix time", timestamp);
		}

		int[] styles = {
			DateFormat.SHORT, DateFormat.MEDIUM, DateFormat.LONG,
			DateFormat.FULL
		};

		for (int dateStyle : styles) {
			for (int timeStyle : styles) {
				try {
					DateFormat dateFormat =
						DateFormat.getDateTimeInstance(dateStyle, timeStyle);

					Date date = dateFormat.parse(timestamp);

					return date;
				}
				catch (ParseException e) {
					logger.debug(
						"'{}' does not match format {}, {}", timestamp,
							dateStyle, timeStyle);
				}
			}
		}

		for (int dateStyle : styles) {
			try {
				DateFormat dateFormat = DateFormat.getDateInstance(dateStyle);

				Date date = dateFormat.parse(timestamp);

				return date;
			}
			catch (ParseException e) {
				logger.debug(
					"'{}' does not match date format {}", timestamp, dateStyle);
			}
		}

		for (int timeStyle : styles) {
			try {
				DateFormat dateFormat = DateFormat.getTimeInstance(timeStyle);

				Date time = dateFormat.parse(timestamp);

				return combineDateTime(new Date(), time);
			}
			catch (ParseException e) {
				logger.debug(
					"'{}' does not match time format {}", timestamp, timeStyle);
			}
		}

		throw new IllegalArgumentException(
			"Unable to parse timestamp '" + timestamp + "'");
	}

	protected static Date combineDateTime(Date date, Date time) {
		Calendar dateCalendar = Calendar.getInstance();
		Calendar timeCalendar = Calendar.getInstance();

		dateCalendar.setTime(date);
		timeCalendar.setTime(time);

		timeCalendar.set(dateCalendar.get(Calendar.YEAR),
			dateCalendar.get(Calendar.MONTH), dateCalendar.get(Calendar.DATE));

		return timeCalendar.getTime();
	}

	@Override
	public abstract boolean matches(Build jenkinsBuild);

}

