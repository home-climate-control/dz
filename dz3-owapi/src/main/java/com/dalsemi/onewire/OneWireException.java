/*---------------------------------------------------------------------------
 * Copyright (C) 1999,2000 Dallas Semiconductor Corporation, All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY,  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL DALLAS SEMICONDUCTOR BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Dallas Semiconductor
 * shall not be used except as stated in the Dallas Semiconductor
 * Branding Policy.
 *---------------------------------------------------------------------------
 */

package com.dalsemi.onewire;

import com.dalsemi.onewire.utils.Address;

/**
 * This is the general exception thrown by the iButton and 1-Wire operations.
 * 
 * @version 0.00, 21 August 2000
 * @author DS
 * @author Stability enhancements &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2009
 */
public class OneWireException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * 1-Wire address.
     */
    private byte address[];

    /**
     * Constructs a <code>OneWireException</code> with no detail message.
     */
    public OneWireException() {

        super();
    }

    /**
     * Constructs a <code>OneWireException</code> with the specified detail
     * message.
     * 
     * @param message the detail message description.
     */
    public OneWireException(String message) {

        super(message);
    }

    /**
     * Create an instance with a message and the root cause.
     * 
     * @param message Exception message.
     * @param cause Root cause.
     */
    public OneWireException(String message, Throwable cause) {

        super(message, cause);
    }

    /**
     * Create an instance with the address, message and the cause.
     * 
     * @param address 1-Wire address of the device that generated the exception.
     * @param message Exception message.
     * @param cause Root cause.
     */
    public OneWireException(byte address[], String message, Throwable cause) {

        super(message, cause);

        this.address = address;
    }

    /**
     * Create an instance with an address and a message.
     * 
     * @param address 1-Wire address of the device that generated the exception.
     * @param message Exception message.
     */
    public OneWireException(byte address[], String message) {

        this(address, message, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {

        if (address == null) {

            return super.toString();
        }

        // VT: FIXME: This will not work too good if there is a cause.
        // However, since original OWAPI code didn't have a Throwable in the
        // constructor, this is unlikely to happen.

        return super.toString() + ": " + Address.toString(address);
    }
    
    /**
     * Get a human readable address representation.
     * 
     * @return {@link #address} as String.
     */
    public String getAddressAsString() {
        return Address.toString(address);
    }
}
