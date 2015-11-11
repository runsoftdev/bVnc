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

public class AliasParameter extends VoidParameter {
  public AliasParameter(String name_, String desc_, VoidParameter v) {
    super(name_, desc_);
    param = v;
  }

  public boolean setParam(String v) { return param.setParam(v); }
  public boolean setParam() { return param.setParam(); }

  public String getDefaultStr() { return param.getDefaultStr(); }
  public String getValueStr() { return param.getValueStr(); }
  public boolean isBool() { return param.isBool(); }

  protected VoidParameter param;
}
