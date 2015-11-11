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

public class Point {

  // Point
  //
  // Represents a point in 2D space, by X and Y coordinates.
  // Can also be used to represent a delta, or offset, between
  // two Points.
  // Functions are provided to allow Points to be compared for
  // equality and translated by a supplied offset.
  // Functions are also provided to negate offset Points.

  public Point() {x=0; y=0;}
  public Point(int x_, int y_) { x=x_; y=y_;}
  public Point(float x_, float y_) { x=(int)x_; y=(int)y_;}
  public final Point negate() {return new Point(-x, -y);}
  public final boolean equals(Point p) {return (x==p.x && y==p.y);}
  public final Point translate(Point p) {return new Point(x+p.x, y+p.y);}
  public final Point subtract(Point p) {return new Point(x-p.x, y-p.y);}
  public int x, y;

}
