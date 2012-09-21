/**
 * Copyright (c) 2011-2012 Felix Mannhardt, felix.mannhardt@smail.wir.h-brs.de
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See: http://www.gnu.org/licenses/lgpl-3.0
 *
 */
package de.hbrs.oryx.yawl.converter.context;

import de.hbrs.oryx.yawl.converter.exceptions.ConversionException;
import de.hbrs.oryx.yawl.converter.handler.HandlerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for conversion contexts. Sharing error/warning reporting and
 * access to HandlerFactory.
 *
 * @author Felix Mannhardt (Bonn-Rhein-Sieg University of Applied Sciences)
 */
public abstract class ConversionContext {

    /**
     * Used to create new Handlers for YAWL Elements
     */
    private HandlerFactory handlerFactory;

    /**
     * True if conversion failed
     */
    private Boolean conversionError;

    /**
     * May contain serveral warning or error messages
     */
    private List<ConversionException> conversionWarnings;

    public ConversionContext() {
        super();
        this.conversionWarnings = new ArrayList<ConversionException>();
        this.conversionError = false;
    }

    public void setHandlerFactory(HandlerFactory handlerFactory) {
        this.handlerFactory = handlerFactory;
    }

    public HandlerFactory getHandlerFactory() {
        return handlerFactory;
    }

    public void setConversionError(Boolean conversionError) {
        this.conversionError = conversionError;
    }

    public Boolean getConversionError() {
        return conversionError;
    }

    public void addConversionWarnings(ConversionException e) {
        this.conversionWarnings.add(e);
    }

    public void addConversionWarnings(String string, Exception e) {
        this.conversionWarnings.add(new ConversionException(string, e));
    }

    public List<ConversionException> getConversionWarnings() {
        return conversionWarnings;
    }

}