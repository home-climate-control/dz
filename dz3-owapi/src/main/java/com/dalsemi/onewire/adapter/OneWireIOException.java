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

package com.dalsemi.onewire.adapter;

// imports
import com.dalsemi.onewire.OneWireException;

/**
 * This exception is thrown when there is an IO error communicating on on the
 * 1-Wire Network. For instance, when a network error occurs when calling the
 * putBit(boolean) method.
 * 
 * @version 0.00, 28 Aug 2000
 * @author DS
 */
public class OneWireIOException extends OneWireException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a <code>OneWireIOException</code> with no detail message.
     */
    public OneWireIOException() {

        super();
    }

    /**
     * Constructs a <code>OneWireIOException</code> with the specified detail
     * message.
     * 
     * @param message The detail message description.
     */
    public OneWireIOException(String message) {

        super(message);
    }

    /**
     * Create an instance with a message and the root cause.
     * 
     * @param message Exception message.
     * @param cause Root cause.
     */
    public OneWireIOException(String message, Throwable cause) {

        super(message, cause);
    }

    /**
     * Create an instance with the address and the message.
     * 
     * @param address 1-Wire address of the device that generated the exception.
     * @param message Exception message.
     */
    public OneWireIOException(byte address[], String message) {

        super(address, message, null);
    }

    /**
     * Create an instance with the address, the message and the root cause.
     * 
     * @param address 1-Wire address of the device that generated the exception.
     * @param message Exception message.
     * @param rootCause Root cause.
     */
    public OneWireIOException(byte address[], String message, Throwable rootCause) {

        super(address, message, rootCause);
    }
}
