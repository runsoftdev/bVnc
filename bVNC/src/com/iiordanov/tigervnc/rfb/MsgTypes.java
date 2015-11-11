/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
 * 
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */

package com.iiordanov.tigervnc.rfb;

public class MsgTypes {
  // server to client

  public static final int msgTypeFramebufferUpdate = 0;
  public static final int msgTypeSetColourMapEntries = 1;
  public static final int msgTypeBell = 2;
  public static final int msgTypeServerCutText = 3;

  // client to server

  public static final int msgTypeSetPixelFormat = 0;
  public static final int msgTypeFixColourMapEntries = 1;
  public static final int msgTypeSetEncodings = 2;
  public static final int msgTypeFramebufferUpdateRequest = 3;
  public static final int msgTypeKeyEvent = 4;
  public static final int msgTypePointerEvent = 5;
  public static final int msgTypeClientCutText = 6;

  public static final int msgTypeSetDesktopSize = 251;
}
