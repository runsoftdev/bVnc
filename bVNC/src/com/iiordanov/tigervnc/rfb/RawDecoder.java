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

public class RawDecoder extends Decoder {

  public RawDecoder(CMsgReader reader_) { reader = reader_; }

  public void readRect(Rect r, CMsgHandler handler) {
    int x = r.tl.x;
    int y = r.tl.y;
    int w = r.width();
    int h = r.height();
    int[] imageBuf = reader.getImageBuf(w * h);
    int nPixels = imageBuf.length / (reader.bpp() / 8);
    while (h > 0) {
      int nRows = nPixels / w;
      if (nRows > h) nRows = h;
      reader.is.readPixels(imageBuf, w * h, (reader.bpp() / 8), handler.cp.pf().bigEndian);
      handler.imageRect(new Rect(x, y, x+w, y+nRows), imageBuf);
      h -= nRows;
      y += nRows;
    }
  }

  CMsgReader reader;
  static LogWriter vlog = new LogWriter("RawDecoder");
}
