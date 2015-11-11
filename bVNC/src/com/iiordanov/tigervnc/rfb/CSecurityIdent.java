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

import com.iiordanov.tigervnc.rdr.*;
import com.iiordanov.tigervnc.vncviewer.*;

public class CSecurityIdent extends CSecurity {

  public CSecurityIdent() { }

  public boolean processMsg(CConnection cc) {
    OutStream os = cc.getOutStream();

    StringBuffer username = new StringBuffer();

    CConn.upg.getUserPasswd(username, null);

    // Return the response to the server
    os.writeU32(username.length());
    try {
      byte[] utf8str = username.toString().getBytes("UTF8");
      os.writeBytes(utf8str, 0, username.length());
    } catch(java.io.UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    os.flush();
    return true;
  }

  public int getType() { return Security.secTypeIdent; }

  java.net.Socket sock;
  UserPasswdGetter upg;

  static LogWriter vlog = new LogWriter("Ident");
  public String description() { return "No Encryption"; }

}
