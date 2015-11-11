/* Copyright (C) 2005 Martin Koegler
 * Copyright (C) 2010 TigerVNC Team
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

public class CSecurityStack extends CSecurity {

  public CSecurityStack(int Type, String Name, CSecurity s0,
                     CSecurity s1)
  {
    name = Name;
    type = Type; 
    state = 0;
    state0 = s0;
    state1 = s1;
  }
  
  public boolean processMsg(CConnection cc)
  {
    boolean res = true;
    if (state == 0) {
      if (state0 != null)
        res = state0.processMsg(cc);
  
      if (!res)
        return res;
  
      state++;
    }
  
    if (state == 1) {
      if(state1 != null)
        res = state1.processMsg(cc);
  
      if(!res)
        return res;
  
      state++;
    }
  
    return res;
  }

  public final int getType() { return type; }
  public final String description() { return name; }

  private int state;
  private CSecurity state0;
  private CSecurity state1;
  private String name;
  private int type;

}
