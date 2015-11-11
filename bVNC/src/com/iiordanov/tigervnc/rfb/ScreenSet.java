/* Copyright 2009 Pierre Ossman for Cendio AB
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

// Management class for the RFB virtual screens

package com.iiordanov.tigervnc.rfb;

import java.util.*;

public class ScreenSet {

  // Represents a complete screen configuration, excluding framebuffer
  // dimensions.

  public ScreenSet() {
    screens = new ArrayList<Screen>();
  }

  public final int num_screens() { return screens.size(); }

  public final void add_screen(Screen screen) { screens.add(screen); }
  public final void remove_screen(int id) { 
    for (Iterator iter = screens.iterator(); iter.hasNext(); ) {
      Screen refScreen = (Screen)iter.next();
      if (refScreen.id == id)
        iter.remove();
    }
  }

  public final boolean validate(int fb_width, int fb_height) {
      List<Integer> seen_ids = new ArrayList<Integer>();
      Rect fb_rect = new Rect();

      if (screens.isEmpty())
        return false;
      if (num_screens() > 255)
        return false;

      fb_rect.setXYWH(0, 0, fb_width, fb_height);

      for (Iterator iter = screens.iterator(); iter.hasNext(); ) {
        Screen refScreen = (Screen)iter.next();
        if (refScreen.dimensions.is_empty())
          return false;
        if (!refScreen.dimensions.enclosed_by(fb_rect))
          return false;
        //if (seen_ids.lastIndexOf(refScreen.id) != seen_ids.get(-1))
        //  return false;
        seen_ids.add(refScreen.id);
      }

      return true;
  }

  public final void debug_print() {
    for (Iterator iter = screens.iterator(); iter.hasNext(); ) {
      Screen refScreen = (Screen)iter.next();
      vlog.error("    "+refScreen.id+" (0x"+refScreen.id+"): "+
                refScreen.dimensions.width()+"x"+refScreen.dimensions.height()+
                "+"+refScreen.dimensions.tl.x+"+"+refScreen.dimensions.tl.y+
                " (flags 0x"+refScreen.flags+")");
    }
  }

  // FIXME: List order shouldn't matter
  //inline bool operator(const ScreenSet& r) const { return screens == r.screens; }
  //inline bool operator(const ScreenSet& r) const { return screens != r.screens; }

  public ArrayList<Screen> screens;

  static LogWriter vlog = new LogWriter("ScreenSet");

}

